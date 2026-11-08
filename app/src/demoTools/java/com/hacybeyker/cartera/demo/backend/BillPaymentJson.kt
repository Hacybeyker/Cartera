package com.hacybeyker.cartera.demo.backend

import java.math.BigDecimal
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/** Las dos formas en JSON de un pago: la respuesta del 201 y el movimiento que deja en la cuenta. */
object BillPaymentJson {
    const val CURRENCY = "PEN"
    const val CATEGORY = "SERVICES"

    fun receipt(payment: BillPayment, movementId: String): JsonObject = buildJsonObject {
        put("paymentId", payment.id)
        put("operationNumber", payment.operationNumber)
        put("createdAt", timestamp(payment.createdAt))
        putJsonArray("paidDebts") {
            payment.paidDebts.forEach { debt ->
                addJsonObject {
                    put("id", debt.id)
                    put("period", debt.period)
                    put("amount", money(debt.amount, CURRENCY))
                }
            }
        }
        put("total", money(payment.total, CURRENCY))
        put("accountId", payment.accountId)
        put("accountAvailableBalance", money(payment.newBalance, CURRENCY))
        put("movementId", movementId)
    }

    fun movement(payment: BillPayment, movementId: String): JsonObject = buildJsonObject {
        put("id", movementId)
        put("accountId", payment.accountId)
        put("date", timestamp(payment.createdAt))
        put("description", "Pago de ${payment.billerName}")
        put("category", CATEGORY)
        put("amount", money(payment.total.negate(), CURRENCY))
        put("type", "DEBIT")
        put("operationNumber", payment.operationNumber)
        put("channel", "APP")
        put("status", "COMPLETED")
        putJsonObject("counterparty") {
            put("name", payment.billerName)
            put("reference", payment.customerCode)
        }
        putJsonObject("breakdown") {
            putJsonArray("lines") {
                payment.paidDebts.forEach { debt ->
                    addJsonObject {
                        put("concept", "Recibo de ${BillFormat.period(debt.period)}")
                        put("amount", money(debt.amount, CURRENCY))
                    }
                }
            }
            put("fees", money(BigDecimal.ZERO, CURRENCY))
            put("taxes", money(BigDecimal.ZERO, CURRENCY))
            put("merchant", JsonNull)
            put("category", CATEGORY)
        }
    }
}
