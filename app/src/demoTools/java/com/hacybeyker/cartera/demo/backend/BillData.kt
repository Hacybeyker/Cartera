package com.hacybeyker.cartera.demo.backend

import java.math.BigDecimal
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

data class Biller(val id: String, val name: String, val category: String, val customerCodeFormat: String) {
    private val codePattern = Regex(customerCodeFormat)

    fun accepts(customerCode: String): Boolean = codePattern.matches(customerCode)

    fun toJson(): JsonObject = buildJsonObject {
        put("id", id)
        put("name", name)
        put("category", category)
        put("customerCodeFormat", customerCodeFormat)
    }

    companion object {
        fun from(json: JsonObject) = Biller(
            id = json.string("id"),
            name = json.string("name"),
            category = json.string("category"),
            customerCodeFormat = json.string("customerCodeFormat")
        )
    }
}

/** Un recibo pendiente. [json] es lo que publica el contrato: el fixture ya trae la forma de la respuesta. */
data class Debt(
    val id: String,
    val period: String,
    val amount: BigDecimal,
    val currency: String,
    val json: JsonObject
) {
    companion object {
        fun from(json: JsonObject): Debt {
            val amount = json.getValue("amount").jsonObject
            return Debt(
                id = json.string("id"),
                period = json.string("period"),
                amount = amount.decimal("value"),
                currency = amount.string("currency"),
                json = json
            )
        }
    }
}

/** Un cliente de una empresa: su código, su nombre, los recibos que debe y el histórico que se envía si se pide. */
data class BillerCustomer(val code: String, val name: String, val debts: List<Debt>, val history: JsonArray) {
    companion object {
        fun from(json: JsonObject) = BillerCustomer(
            code = json.string("code"),
            name = json.string("name"),
            debts = json.getValue("debts").jsonArray.map { Debt.from(it.jsonObject) },
            history = json["history"] as? JsonArray ?: JsonArray(emptyList())
        )
    }
}
