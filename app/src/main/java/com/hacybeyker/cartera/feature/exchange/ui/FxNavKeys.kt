package com.hacybeyker.cartera.feature.exchange.ui

import androidx.navigation3.runtime.NavKey
import com.hacybeyker.cartera.core.domain.Money
import com.hacybeyker.cartera.core.domain.exchange.ExchangeAccount
import com.hacybeyker.cartera.core.domain.exchange.ExchangeDirection
import com.hacybeyker.cartera.core.domain.exchange.ExchangeOrder
import com.hacybeyker.cartera.feature.exchange.domain.model.ExchangeReceipt
import java.time.OffsetDateTime
import kotlinx.serialization.Serializable

/**
 * Lleva el cambio completo, con la cotización (`quoteId`, tasa y vencimiento) y su `Idempotency-Key`: sobrevive a la
 * muerte del proceso junto con la pila.
 */
@Serializable
data class FxConfirmNavKey(val order: ExchangeOrderArgs) : NavKey

/** Sin [receipt], el cambio no se pudo confirmar. */
@Serializable
data class FxResultNavKey(val receipt: ExchangeReceiptArgs? = null) : NavKey

/** Las monedas de cada cuenta salen del sentido del cambio. */
@Serializable
data class ExchangeOrderArgs(
    val direction: ExchangeDirection,
    val quoteId: String,
    val rateValue: String,
    val expiresAt: String,
    val sourceId: String,
    val sourceAlias: String,
    val sourceNumberMasked: String,
    val destinationId: String,
    val destinationAlias: String,
    val destinationNumberMasked: String,
    val amountValue: String,
    val idempotencyKey: String
) {
    fun toOrder() = ExchangeOrder(
        direction = direction,
        quoteId = quoteId,
        rate = rateValue.toBigDecimal(),
        expiresAt = OffsetDateTime.parse(expiresAt),
        source = ExchangeAccount(sourceId, sourceAlias, sourceNumberMasked, direction.sourceCurrency),
        destination = ExchangeAccount(
            destinationId,
            destinationAlias,
            destinationNumberMasked,
            direction.destinationCurrency
        ),
        amount = Money(amountValue.toBigDecimal(), direction.sourceCurrency),
        idempotencyKey = idempotencyKey
    )
}

fun ExchangeOrder.toArgs() = ExchangeOrderArgs(
    direction = direction,
    quoteId = quoteId,
    rateValue = rate.toPlainString(),
    expiresAt = expiresAt.toString(),
    sourceId = source.id,
    sourceAlias = source.alias,
    sourceNumberMasked = source.numberMasked,
    destinationId = destination.id,
    destinationAlias = destination.alias,
    destinationNumberMasked = destination.numberMasked,
    amountValue = amount.amount.toPlainString(),
    idempotencyKey = idempotencyKey
)

/** El resultado, con el nombre de cada cuenta tal como se mostró en la confirmación. */
@Serializable
data class ExchangeReceiptArgs(
    val operationNumber: String,
    val rateValue: String,
    val debitedValue: String,
    val debitedCurrency: String,
    val creditedValue: String,
    val creditedCurrency: String,
    val sourceLabel: String,
    val sourceBalanceValue: String,
    val destinationLabel: String,
    val destinationBalanceValue: String
) {
    val debited: Money get() = Money(debitedValue.toBigDecimal(), debitedCurrency)
    val credited: Money get() = Money(creditedValue.toBigDecimal(), creditedCurrency)
    val sourceBalance: Money get() = Money(sourceBalanceValue.toBigDecimal(), debitedCurrency)
    val destinationBalance: Money get() = Money(destinationBalanceValue.toBigDecimal(), creditedCurrency)
}

fun ExchangeReceipt.toArgs(order: ExchangeOrder) = ExchangeReceiptArgs(
    operationNumber = operationNumber,
    rateValue = rate.toPlainString(),
    debitedValue = debited.amount.toPlainString(),
    debitedCurrency = debited.currency,
    creditedValue = credited.amount.toPlainString(),
    creditedCurrency = credited.currency,
    sourceLabel = order.source.label,
    sourceBalanceValue = sourceAvailableBalance.amount.toPlainString(),
    destinationLabel = order.destination.label,
    destinationBalanceValue = destinationAvailableBalance.amount.toPlainString()
)
