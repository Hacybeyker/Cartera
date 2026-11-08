package com.hacybeyker.cartera.demo.backend

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Clock
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

class BackendState(private val catalog: FixtureCatalog, clock: Clock = Clock.systemDefaultZone()) {
    private val balances = mutableMapOf<String, BigDecimal>()
    private val createdMovements = mutableMapOf<String, MutableList<MovementRecord>>()
    private val sequences = mutableMapOf<String, Int>()
    val idempotency = IdempotencyStore()
    val market = MarketState(catalog, clock)
    val holdings = HoldingsBook(catalog)
    val fxQuotes = mutableMapOf<String, FxQuote>()

    /** `deviceToken` → id del usuario que lo registró. */
    val deviceTokens = mutableMapOf<String, String>()

    val appConfig: JsonObject get() = catalog.appConfig

    fun userByUsername(username: String): JsonObject? = catalog.users.firstOrNull { it.string("username") == username }

    fun user(userId: String): JsonObject? = catalog.users.firstOrNull { it.string("id") == userId }

    fun accountsOf(userId: String): List<JsonObject> =
        catalog.accounts.filter { it.string("ownerId") == userId }.map(::withCurrentBalance)

    fun account(userId: String, accountId: String): JsonObject? =
        accountsOf(userId).firstOrNull { it.string("id") == accountId }

    fun thirdPartyAccount(number: String): JsonObject? =
        catalog.thirdPartyAccounts.firstOrNull { it.string("number") == number }

    fun movements(accountIds: List<String>): List<MovementRecord> {
        val created = accountIds.flatMap { createdMovements[it].orEmpty() }
        val fixtures = accountIds.flatMap(catalog::movements)
        return if (created.isEmpty() && accountIds.size == 1) {
            fixtures
        } else {
            (created + fixtures).sortedWith(MovementRecord.NEWEST_FIRST)
        }
    }

    /** Numeración propia por tipo de operación (transferencias, órdenes, cambios, cotizaciones), desde 1. */
    fun nextSequence(kind: String): Int = sequences.merge(kind, 1, Int::plus) ?: 1

    fun post(accountId: String, newBalance: BigDecimal, movement: MovementRecord) {
        balances[accountId] = newBalance
        createdMovements.getOrPut(accountId) { mutableListOf() }.add(movement)
    }

    fun reset() {
        balances.clear()
        createdMovements.clear()
        idempotency.clear()
        sequences.clear()
        market.reset()
        holdings.reset()
        fxQuotes.clear()
        deviceTokens.clear()
    }

    private fun withCurrentBalance(account: JsonObject): JsonObject {
        val balance = balances[account.string("id")] ?: return account
        val currency = account.string("currency")
        return JsonObject(account + ("availableBalance" to money(balance, currency)))
    }
}

class IdempotencyStore {
    private val responses = mutableMapOf<String, BackendResponse>()

    /** Solo se guarda una respuesta correcta: si [action] lanza un problema, la misma clave se puede reintentar. */
    fun once(key: String, action: () -> BackendResponse): BackendResponse =
        responses[key] ?: action().also { responses[key] = it }

    fun clear() = responses.clear()
}

private val LIMA: ZoneOffset = ZoneOffset.ofHours(-5)

fun timestamp(instant: Instant): String = OffsetDateTime.ofInstant(
    instant,
    LIMA
).truncatedTo(ChronoUnit.SECONDS).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

/** Redondeo de montos del backend: a 2 decimales, mitad hacia arriba, como los fixtures. */
fun cents(value: BigDecimal): BigDecimal = value.setScale(2, RoundingMode.HALF_UP)

fun JsonObject.availableBalance(): BigDecimal = BigDecimal(getValue("availableBalance").jsonObject.string("value"))

fun money(value: BigDecimal, currency: String): JsonObject = buildJsonObject {
    put("value", value.setScale(2).toPlainString())
    put("currency", currency)
}
