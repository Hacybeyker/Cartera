package com.hacybeyker.cartera.demo.backend

import java.time.Instant
import java.time.OffsetDateTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

fun interface FixtureSource {
    fun read(path: String): String
}

data class MovementRecord(val id: String, val accountId: String, val instant: Instant, val json: JsonObject) {
    companion object {
        val NEWEST_FIRST: Comparator<MovementRecord> =
            compareByDescending<MovementRecord> { it.instant }.thenByDescending { it.id }

        fun from(json: JsonObject): MovementRecord = MovementRecord(
            id = json.string("id"),
            accountId = json.string("accountId"),
            instant = OffsetDateTime.parse(json.string("date")).toInstant(),
            json = json
        )
    }
}

// Cada archivo se lee la primera vez que se necesita: así el backend no infla el arranque de la app.
class FixtureCatalog(private val source: FixtureSource) {
    val appConfig: JsonObject by lazy { parse("app-config.json").jsonObject }
    val users: List<JsonObject> by lazy { objects("users.json") }
    val accounts: List<JsonObject> by lazy { objects("accounts.json") }
    val thirdPartyAccounts: List<JsonObject> by lazy { objects("third-party-accounts.json") }
    private val movements = mutableMapOf<String, List<MovementRecord>>()

    fun movements(accountId: String): List<MovementRecord> = synchronized(movements) {
        movements.getOrPut(accountId) {
            objects("movements/$accountId.json").map(MovementRecord::from).sortedWith(MovementRecord.NEWEST_FIRST)
        }
    }

    private fun objects(path: String): List<JsonObject> = parse(path).jsonArray.map { it.jsonObject }

    private fun parse(path: String) = Json.parseToJsonElement(source.read(path))
}

fun JsonObject.string(key: String): String = getValue(key).jsonPrimitive.content
