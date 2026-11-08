package com.hacybeyker.cartera.legacy.payment.domain

/**
 * Lo que se envía a `POST /bill-payments`. Un reintento por red o 5xx envía la misma orden con la misma
 * [idempotencyKey]; un intento de clave nuevo crea otra.
 */
data class PaymentOrder(
    val billerId: String,
    val customerCode: String,
    val debtIds: List<String>,
    val accountId: String,
    val pin: String,
    val idempotencyKey: String
) {
    override fun toString(): String =
        "PaymentOrder(billerId=$billerId, debtIds=$debtIds, accountId=$accountId, idempotencyKey=$idempotencyKey)"
}
