package com.hacybeyker.cartera.feature.transfer.ui

import androidx.navigation3.runtime.NavKey
import com.hacybeyker.cartera.core.domain.Money
import com.hacybeyker.cartera.feature.transfer.domain.model.TransferOrder
import com.hacybeyker.cartera.feature.transfer.domain.model.TransferReceipt
import kotlinx.serialization.Serializable

@Serializable
data object TransferFormNavKey : NavKey

/** Lleva la orden completa, con su `Idempotency-Key`: sobrevive a la muerte del proceso junto con la pila. */
@Serializable
data class TransferConfirmNavKey(val order: TransferOrderArgs) : NavKey

/** Sin [receipt], la transferencia no se pudo confirmar. */
@Serializable
data class TransferResultNavKey(val receipt: TransferReceiptArgs? = null) : NavKey

@Serializable
data class TransferOrderArgs(
    val sourceAccountId: String,
    val sourceAlias: String,
    val sourceNumberMasked: String,
    val destinationAccountNumber: String,
    val amountValue: String,
    val currency: String,
    val description: String?,
    val idempotencyKey: String
) {
    fun toOrder() = TransferOrder(
        sourceAccountId = sourceAccountId,
        sourceAlias = sourceAlias,
        sourceNumberMasked = sourceNumberMasked,
        destinationAccountNumber = destinationAccountNumber,
        amount = Money(amountValue.toBigDecimal(), currency),
        description = description,
        idempotencyKey = idempotencyKey
    )
}

fun TransferOrder.toArgs() = TransferOrderArgs(
    sourceAccountId = sourceAccountId,
    sourceAlias = sourceAlias,
    sourceNumberMasked = sourceNumberMasked,
    destinationAccountNumber = destinationAccountNumber,
    amountValue = amount.amount.toPlainString(),
    currency = amount.currency,
    description = description,
    idempotencyKey = idempotencyKey
)

@Serializable
data class TransferReceiptArgs(val operationNumber: String, val balanceValue: String, val balanceCurrency: String) {
    val balance: Money get() = Money(balanceValue.toBigDecimal(), balanceCurrency)
}

fun TransferReceipt.toArgs() = TransferReceiptArgs(
    operationNumber = operationNumber,
    balanceValue = sourceAvailableBalance.amount.toPlainString(),
    balanceCurrency = sourceAvailableBalance.currency
)
