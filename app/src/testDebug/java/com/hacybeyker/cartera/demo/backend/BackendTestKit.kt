package com.hacybeyker.cartera.demo.backend

import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

class MutableClock(var now: Instant = Instant.parse("2026-11-07T15:00:00Z")) : Clock() {
    override fun getZone(): ZoneId = ZoneOffset.UTC

    override fun withZone(zone: ZoneId?): Clock = this

    override fun instant(): Instant = now
}

val fixturesDir: File = File(requireNotNull(System.getProperty("cartera.fixturesDir")))

val fileFixtures = FixtureSource { path -> File(fixturesDir, path).readText() }

class RouterHarness(val clock: MutableClock = MutableClock()) {
    val state = BackendState(FixtureCatalog(fileFixtures), clock)
    val jwt = JwtSigner(clock)
    val desks = BackendDesks.create(state, jwt, clock)
    var resets = 0
    val router = BackendRouter(state, desks, onReset = { resets++ })

    fun call(
        method: String,
        path: String,
        token: String? = null,
        body: String = "",
        query: Map<String, String> = emptyMap(),
        headers: Map<String, String> = emptyMap()
    ): BackendResponse {
        val auth = token?.let { mapOf("Authorization" to "Bearer $it") }.orEmpty()
        return router.route(
            BackendRequest(
                method = method,
                path = "${Endpoint.BASE_PATH}$path",
                query = query,
                headers = headers + auth,
                body = body,
                requestId = "req-test"
            )
        )
    }

    fun get(path: String, token: String? = null, query: Map<String, String> = emptyMap()) =
        call("GET", path, token = token, query = query)

    fun login(username: String = "ana.torres", password: String = "Cartera2026"): String {
        val response = call("POST", "auth/login", body = """{"username":"$username","password":"$password"}""")
        return response.json().string("accessToken")
    }

    fun transfer(token: String, body: String, key: String? = "key-1"): BackendResponse =
        post("transfers", token, body, key)

    fun order(token: String, body: String, key: String? = "order-1"): BackendResponse = post("orders", token, body, key)

    fun exchange(token: String, body: String, key: String? = "fx-1"): BackendResponse =
        post("fx/exchanges", token, body, key)

    fun quote(token: String, ttlSeconds: Int? = null): JsonObject = call(
        method = "GET",
        path = "fx/rates",
        token = token,
        headers = ttlSeconds?.let { mapOf("X-Demo-Quote-Ttl-S" to it.toString()) }.orEmpty()
    ).json()

    fun balance(token: String, accountId: String): String =
        get("accounts/$accountId", token).json().obj("availableBalance").string("value")

    private fun post(path: String, token: String, body: String, key: String?) = call(
        method = "POST",
        path = path,
        token = token,
        body = body,
        headers = key?.let { mapOf("Idempotency-Key" to it) }.orEmpty()
    )
}

fun orderBody(symbol: String = "ANDM", side: String = "BUY", quantity: String = "2", account: String = "ACC-003") =
    """{"symbol":"$symbol","side":"$side","quantity":$quantity,"accountId":"$account"}"""

fun exchangeBody(
    quoteId: String,
    source: String = "ACC-003",
    destination: String = "ACC-002",
    value: String = "100.00",
    currency: String = "USD"
) = """{"quoteId":"$quoteId","sourceAccountId":"$source","destinationAccountId":"$destination",""" +
    """"amount":{"value":"$value","currency":"$currency"}}"""

fun BackendResponse.problemCode(): String = json().string("code")

fun BackendResponse.errorFields(): List<String> =
    json().getValue("errors").jsonArray.map { it.jsonObject.string("field") }

fun BackendResponse.json(): JsonObject = Json.parseToJsonElement(requireNotNull(body)).jsonObject

fun JsonObject.items(): JsonArray = getValue("items").jsonArray

fun JsonObject.obj(key: String): JsonObject = getValue(key).jsonObject

fun transferBody(
    source: String = "ACC-002",
    destination: String = "999-0000001-0-01",
    value: String = "150.00",
    currency: String = "PEN"
) = """{"sourceAccountId":"$source","destinationAccountNumber":"$destination",""" +
    """"amount":{"value":"$value","currency":"$currency"}}"""
