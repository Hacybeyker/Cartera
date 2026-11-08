package com.hacybeyker.cartera.feature.order.ui

import androidx.navigation3.runtime.NavKey
import com.hacybeyker.cartera.core.domain.Money
import com.hacybeyker.cartera.core.domain.market.OrderSide
import com.hacybeyker.cartera.feature.order.domain.model.MarketOrder
import com.hacybeyker.cartera.feature.order.domain.model.OrderReceipt
import kotlinx.serialization.Serializable

@Serializable
data class OrderFormNavKey(val symbol: String, val side: OrderSide) : NavKey

/** Lleva la orden completa, con su `Idempotency-Key`: sobrevive a la muerte del proceso junto con la pila. */
@Serializable
data class OrderConfirmNavKey(val order: MarketOrderArgs) : NavKey

/** Sin [receipt], la orden no se pudo confirmar. */
@Serializable
data class OrderResultNavKey(val receipt: OrderReceiptArgs? = null) : NavKey

@Serializable
data class MarketOrderArgs(
    val symbol: String,
    val side: OrderSide,
    val quantity: Int,
    val accountId: String,
    val accountAlias: String,
    val accountNumberMasked: String,
    val priceValue: String,
    val currency: String,
    val idempotencyKey: String
) {
    fun toOrder() = MarketOrder(
        symbol = symbol,
        side = side,
        quantity = quantity,
        accountId = accountId,
        accountAlias = accountAlias,
        accountNumberMasked = accountNumberMasked,
        referencePrice = Money(priceValue.toBigDecimal(), currency),
        idempotencyKey = idempotencyKey
    )
}

fun MarketOrder.toArgs() = MarketOrderArgs(
    symbol = symbol,
    side = side,
    quantity = quantity,
    accountId = accountId,
    accountAlias = accountAlias,
    accountNumberMasked = accountNumberMasked,
    priceValue = referencePrice.amount.toPlainString(),
    currency = referencePrice.currency,
    idempotencyKey = idempotencyKey
)

/** Los montos de una orden van todos en la moneda de la cuenta (USD, la única en que se liquidan las acciones). */
@Serializable
data class OrderReceiptArgs(
    val side: OrderSide,
    val symbol: String,
    val operationNumber: String,
    val priceValue: String,
    val totalValue: String,
    val balanceValue: String,
    val currency: String,
    val holdingQuantity: Int,
    val holdingAverageValue: String?
) {
    val price: Money get() = money(priceValue)
    val total: Money get() = money(totalValue)
    val balance: Money get() = money(balanceValue)
    val holdingAverage: Money? get() = holdingAverageValue?.let(::money)

    private fun money(value: String) = Money(value.toBigDecimal(), currency)
}

fun OrderReceipt.toArgs() = OrderReceiptArgs(
    side = side,
    symbol = symbol,
    operationNumber = operationNumber,
    priceValue = executedPrice.amount.toPlainString(),
    totalValue = total.amount.toPlainString(),
    balanceValue = accountAvailableBalance.amount.toPlainString(),
    currency = executedPrice.currency,
    holdingQuantity = holding.quantity,
    holdingAverageValue = holding.averagePrice?.amount?.toPlainString()
)
