package com.hacybeyker.cartera.demo.contract

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.hacybeyker.cartera.demo.backend.BackendRequest
import com.hacybeyker.cartera.demo.backend.BackendResponse
import com.hacybeyker.cartera.demo.backend.CarteraBackend
import com.hacybeyker.cartera.demo.backend.Outcome
import com.hacybeyker.cartera.demo.backend.RouterHarness
import com.hacybeyker.cartera.demo.backend.billBody
import com.hacybeyker.cartera.demo.backend.exchangeBody
import com.hacybeyker.cartera.demo.backend.fileFixtures
import com.hacybeyker.cartera.demo.backend.json
import com.hacybeyker.cartera.demo.backend.orderBody
import com.hacybeyker.cartera.demo.backend.string
import com.hacybeyker.cartera.demo.backend.transferBody
import java.time.Duration

/** Lo que una prueba le manda al backend: la ruta va sin el prefijo `/api/v1`. */
class Sent(
    val method: String,
    val path: String,
    val query: Map<String, String> = emptyMap(),
    val headers: Map<String, String> = emptyMap(),
    val body: String = ""
)

class Played(val sent: Sent, val response: BackendResponse)

/** Un backend limpio con la sesión de Ana ya iniciada. */
class Env(val harness: RouterHarness = RouterHarness()) {
    val token: String = harness.login()

    fun play(sent: Sent, token: String? = this.token) = Played(
        sent,
        harness.call(sent.method, sent.path, token, sent.body, sent.query, sent.headers)
    )

    fun get(path: String, query: Map<String, String> = emptyMap()) = play(Sent("GET", path, query))

    fun post(path: String, body: String, key: String? = "key-1") = play(
        Sent("POST", path, body = body, headers = key?.let { mapOf(IDEMPOTENCY to it) }.orEmpty())
    )

    fun quoteId(ttlSeconds: Int? = null): String = harness.quote(token, ttlSeconds).string("quoteId")

    fun advance(seconds: Long) {
        harness.clock.now = harness.clock.now.plus(Duration.ofSeconds(seconds))
    }

    companion object {
        const val IDEMPOTENCY = "Idempotency-Key"
    }
}

/**
 * Una llamada al backend embebido y lo que el contrato dice de ella: la operación (`get /accounts`), el estado y, si
 * el contrato trae un ejemplo con nombre para esa respuesta, cuál. [adapt] recorta la respuesta real a lo que el
 * ejemplo muestra.
 */
class Scenario(
    val operation: String,
    val status: Int,
    val example: String? = null,
    val adapt: (JsonNode) -> JsonNode = { it },
    val play: Env.() -> Played
) {
    override fun toString() = "$operation $status ${example.orEmpty()}".trim()
}

private const val LUZ = "billers/BIL-LUZ/debts"
private val CUSTOMER_LUZ = mapOf("customerCode" to "100200")
private const val NO_BODY = ""

private fun scenario(
    operation: String,
    status: Int,
    example: String? = null,
    adapt: (JsonNode) -> JsonNode = { it },
    play: Env.() -> Played
) = Scenario(operation, status, example, adapt, play)

private fun JsonNode.withNullCursor(): JsonNode = (deepCopy<ObjectNode>()).putNull("nextCursor")

private fun JsonNode.lastPoints(count: Int): JsonNode {
    val copy = deepCopy<ObjectNode>()
    val points = copy.get("points") as ArrayNode
    val tail = points.toList().takeLast(count)
    points.removeAll()
    points.addAll(tail)
    return copy
}

object ContractScenarios {
    private val session = listOf(
        scenario("get /app/config", 200, "configuracion") { get("app/config") },
        scenario("post /auth/login", 200, "sesion") { login("ana.torres", "Cartera2026") },
        scenario("post /auth/login", 401, "credencialesInvalidas") { login("ana.torres", "otra") },
        scenario("post /auth/login", 422, "camposObligatorios") { post("auth/login", "{}", key = null) },
        scenario("get /me", 200, "ana") { get("me") },
        scenario("get /accounts", 200, "cuentas") { get("accounts") },
        scenario("get /accounts/{accountId}", 200, "cuentaSueldo") { get("accounts/ACC-001") },
        scenario("get /accounts/{accountId}", 404, "cuentaNoExiste") { get("accounts/ACC-999") }
    )

    private val movements = listOf(
        scenario("get /movements", 200, "sinMasPaginas", JsonNode::withNullCursor) { firstPage() },
        scenario("get /movements", 200, "conMasPaginas") { firstPage() },
        scenario("get /movements", 422, "limiteFueraDeRango") { get("movements", mapOf("limit" to "0")) },
        scenario("get /movements", 422, "cursorInvalido") { get("movements", mapOf("cursor" to "AAAA")) },
        scenario("get /movements", 422, "cuentaAjena") { get("movements", mapOf("accountId" to "ACC-999")) },
        scenario("get /movements/{movementId}", 200, "detalle") { get("movements/MOV-001-0001") },
        scenario("get /movements/{movementId}", 404, "movimientoNoExiste") { get("movements/MOV-999") },
        scenario("get /movements/{movementId}/breakdown", 200, "desglose") { get("movements/MOV-001-0001/breakdown") },
        scenario("get /movements/{movementId}/breakdown", 404, "movimientoNoExiste") {
            get("movements/MOV-999/breakdown")
        }
    )

    private val transfers = listOf(
        scenario("post /transfers", 201, "transferencia") { post("transfers", transferBody()) },
        scenario("post /transfers", 422, "saldoInsuficiente") { post("transfers", transferBody(value = "999999.00")) },
        scenario("post /transfers", 422, "datosInvalidos") { post("transfers", transferBody(value = "0")) },
        scenario("post /transfers", 422, "sinClaveDeIdempotencia") { post("transfers", transferBody(), key = null) },
        scenario("post /transfers", 422, "destinoNoExiste") {
            post("transfers", transferBody(destination = "999-9999999-9-99"))
        },
        scenario("post /transfers", 422, "monedasDistintas") {
            post("transfers", transferBody(destination = "999-0000003-0-10"))
        }
    )

    private val market = listOf(
        scenario("get /market/stocks", 200, "acciones") { get("market/stocks") },
        scenario("get /market/stocks/{symbol}", 200, "andm") { get("market/stocks/ANDM") },
        scenario("get /market/stocks/{symbol}", 404, "accionNoExiste") { get("market/stocks/ZZZZ") },
        scenario("get /market/stocks/{symbol}/history", 200, "ultimosPuntos1D", { it.lastPoints(6) }) {
            get("market/stocks/ANDM/history", mapOf("range" to "1D"))
        },
        scenario("get /market/stocks/{symbol}/history", 404, "accionNoExiste") {
            get("market/stocks/ZZZZ/history", mapOf("range" to "1D"))
        },
        scenario("get /market/stocks/{symbol}/history", 422, "rangoInvalido") {
            get("market/stocks/ANDM/history", mapOf("range" to "2D"))
        },
        scenario("get /portfolio", 200, "portafolio") { get("portfolio") }
    )

    private val orders = listOf(
        scenario("post /orders", 201, "compra") { post("orders", orderBody()) },
        scenario("post /orders", 422, "saldoInsuficiente") { post("orders", orderBody(quantity = "100000")) },
        scenario("post /orders", 422, "tenenciaInsuficiente") {
            post("orders", orderBody(side = "SELL", quantity = "99"))
        },
        scenario("post /orders", 422, "datosInvalidos") { post("orders", orderBody(quantity = "0")) }
    )

    private val exchange = listOf(
        scenario("get /fx/rates", 200, "cotizacion") { get("fx/rates") },
        scenario("post /fx/exchanges", 201, "cambio") { post("fx/exchanges", exchangeBody(quoteId())) },
        scenario("post /fx/exchanges", 422, "cotizacionVencida") {
            quoteId()
            val expiring = quoteId(ttlSeconds = 5)
            advance(10)
            post("fx/exchanges", exchangeBody(expiring), key = "key-2")
        },
        scenario("post /fx/exchanges", 422, "saldoInsuficiente") {
            post("fx/exchanges", exchangeBody(quoteId(), value = "999999.00"))
        },
        scenario("post /fx/exchanges", 422, "monedasDistintas") {
            post("fx/exchanges", exchangeBody(quoteId(), source = "ACC-001", destination = "ACC-002", currency = "PEN"))
        },
        scenario("post /fx/exchanges", 422, "datosInvalidos") {
            post("fx/exchanges", exchangeBody(quoteId(), value = "0"))
        }
    )

    private val bills = listOf(
        scenario("get /billers", 200, "empresas") { get("billers") },
        scenario("get /billers/{billerId}/debts", 200, "luzDelNorte") { get(LUZ, CUSTOMER_LUZ) },
        scenario("get /billers/{billerId}/debts", 200, "sinRecibos") {
            get("billers/BIL-NET/debts", mapOf("customerCode" to "700800"))
        },
        scenario("get /billers/{billerId}/debts", 404, "clienteNoExiste") {
            get(LUZ, mapOf("customerCode" to "999999"))
        },
        scenario("get /billers/{billerId}/debts", 404, "empresaNoExiste") {
            get("billers/BIL-XXX/debts", CUSTOMER_LUZ)
        },
        scenario("get /billers/{billerId}/debts", 422, "datosInvalidos") {
            get(LUZ, mapOf("customerCode" to "12", "includeHistory" to "x"))
        },
        scenario("post /bill-payments", 201, "pagoDeReferencia") { post("bill-payments", billBody()) },
        scenario("post /bill-payments", 422, "claveInvalida") { post("bill-payments", billBody(pin = "0000")) },
        scenario("post /bill-payments", 422, "reciboYaPagado") {
            post("bill-payments", billBody())
            post("bill-payments", billBody(), key = "key-2")
        },
        scenario("post /bill-payments", 422, "monedasDistintas") {
            post("bill-payments", billBody(accountId = "ACC-003"))
        },
        scenario("post /bill-payments", 422, "datosInvalidos") {
            post("bill-payments", billBody(pin = "12", debtIds = emptyList()))
        },
        scenario("get /bill-payments/{paymentId}/voucher", 200, "constancia") {
            post("bill-payments", billBody())
            get("bill-payments/PAY-000001/voucher")
        },
        scenario("get /bill-payments/{paymentId}/voucher", 404, "pagoNoExiste") {
            get("bill-payments/PAY-999999/voucher")
        }
    )

    private val biometrics = listOf(
        scenario("post /auth/biometric/enroll", 201, "dispositivo") {
            post("auth/biometric/enroll", NO_BODY, key = null)
        },
        scenario("post /auth/biometric/login", 200, "sesion") {
            val token = enroll()
            post("auth/biometric/login", """{"deviceToken":"$token"}""", key = null)
        },
        scenario("post /auth/biometric/login", 401, "dispositivoInvalido") {
            post("auth/biometric/login", """{"deviceToken":"dvc_nope"}""", key = null)
        },
        scenario("post /auth/biometric/login", 422, "tokenObligatorio") {
            post("auth/biometric/login", "{}", key = null)
        },
        scenario("delete /auth/biometric", 204) { play(Sent("DELETE", "auth/biometric")) },
        scenario("post /_demo/reset", 204) { play(Sent("POST", "_demo/reset"), token = null) }
    )

    private fun Env.login(username: String, password: String) =
        post("auth/login", """{"username":"$username","password":"$password"}""", key = null)

    private fun Env.firstPage() = get("movements", mapOf("accountId" to "ACC-001", "limit" to "6"))

    private fun Env.enroll(): String {
        val response = post("auth/biometric/enroll", NO_BODY, key = null).response
        return response.json().string("deviceToken")
    }

    private val specific = session + movements + transfers + market + orders + exchange + bills + biometrics

    /** Las respuestas que cualquier operación autenticada y cualquier operación en general pueden dar. */
    private val generic: List<Scenario> by lazy {
        ContractSpec.operations.flatMap { (operation, node) ->
            val authenticated = ContractSpec.isAuthenticated(node)
            val statuses = ContractSpec.statuses(node)
            listOfNotNull(
                scenario(operation, 401, "sinToken") { play(sentFor(operation), token = null) }
                    .takeIf { authenticated && 401 in statuses },
                scenario(operation, 401, "sesionExpirada") {
                    advance(SESSION_SECONDS + 1)
                    play(sentFor(operation))
                }.takeIf { authenticated && 401 in statuses },
                scenario(operation, 500, "errorInterno") { injected(operation, "X-Demo-Failure-Rate" to "100") }
                    .takeIf { 500 in statuses },
                scenario(operation, 503, "servicioCaido") { injected(operation, "X-Demo-Backend-Down" to "true") }
                    .takeIf { 503 in statuses }
            )
        }
    }

    val all: List<Scenario> by lazy { specific + generic }

    private const val SESSION_SECONDS = 900L

    private fun sentFor(operation: String): Sent {
        val (method, path) = operation.split(' ')
        return Sent(method.uppercase(), ContractSpec.concrete(path).removePrefix("/"))
    }

    /** Una falla inyectada por cabecera, que el backend resuelve antes de enrutar. */
    private fun injected(operation: String, header: Pair<String, String>): Played {
        val sent = sentFor(operation)
        val request = BackendRequest(sent.method, "/api/v1/${sent.path}", headers = mapOf(header))
        val backend = CarteraBackend(fileFixtures)
        try {
            val outcome = backend.handle(request).outcome as Outcome.Respond
            return Played(sent, outcome.response)
        } finally {
            backend.close()
        }
    }
}
