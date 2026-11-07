package com.hacybeyker.cartera.demo.backend

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

// Proyecciones de los fixtures a lo que publica el contrato: cada respuesta expone solo sus campos.
object ResponseViews {
    private val PROFILE_FIELDS = setOf("id", "fullName", "documentMasked", "email")
    private val SUMMARY_FIELDS = setOf("id", "accountId", "date", "description", "category", "amount", "type")
    private val DETAIL_FIELDS = SUMMARY_FIELDS + setOf("operationNumber", "channel", "status", "counterparty")

    fun profile(user: JsonObject) = JsonObject(user.filterKeys { it in PROFILE_FIELDS })

    fun account(account: JsonObject) = JsonObject(account.filterKeys { it != "ownerId" })

    fun accounts(accounts: List<JsonObject>) = buildJsonObject {
        put("items", JsonArray(accounts.map(::account)))
    }

    fun movementDetail(record: MovementRecord) = JsonObject(record.json.filterKeys { it in DETAIL_FIELDS })

    fun movementPage(page: Page) = buildJsonObject {
        put("items", JsonArray(page.items.map(::movementSummary)))
        put("nextCursor", page.nextCursor?.let(::JsonPrimitive) ?: JsonNull)
    }

    private fun movementSummary(record: MovementRecord) = JsonObject(record.json.filterKeys { it in SUMMARY_FIELDS })
}
