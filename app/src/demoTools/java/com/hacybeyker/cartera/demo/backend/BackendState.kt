package com.hacybeyker.cartera.demo.backend

import java.math.BigDecimal
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

class BackendState(private val catalog: FixtureCatalog) {
    private val balances = mutableMapOf<String, BigDecimal>()
    private val createdMovements = mutableMapOf<String, MutableList<MovementRecord>>()
    private var transferSequence = 0
    val idempotency = IdempotencyStore()

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

    fun nextTransferSequence(): Int = ++transferSequence

    fun debit(accountId: String, newBalance: BigDecimal, movement: MovementRecord) {
        balances[accountId] = newBalance
        createdMovements.getOrPut(accountId) { mutableListOf() }.add(movement)
    }

    fun reset() {
        balances.clear()
        createdMovements.clear()
        idempotency.clear()
        transferSequence = 0
    }

    private fun withCurrentBalance(account: JsonObject): JsonObject {
        val balance = balances[account.string("id")] ?: return account
        val currency = account.string("currency")
        return JsonObject(account + ("availableBalance" to money(balance, currency)))
    }
}

class IdempotencyStore {
    private val responses = mutableMapOf<String, BackendResponse>()

    operator fun get(key: String): BackendResponse? = responses[key]

    operator fun set(key: String, response: BackendResponse) {
        responses[key] = response
    }

    fun clear() = responses.clear()
}

fun JsonObject.availableBalance(): BigDecimal = BigDecimal(getValue("availableBalance").jsonObject.string("value"))

fun money(value: BigDecimal, currency: String): JsonObject = buildJsonObject {
    put("value", value.setScale(2).toPlainString())
    put("currency", currency)
}
