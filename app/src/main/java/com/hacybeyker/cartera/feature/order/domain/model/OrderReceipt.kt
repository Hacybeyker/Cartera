package com.hacybeyker.cartera.feature.order.domain.model

import com.hacybeyker.cartera.core.domain.Money
import com.hacybeyker.cartera.core.domain.market.OrderSide

/** La orden ejecutada. [holding] es la tenencia de la acción después de la orden. */
data class OrderReceipt(
    val orderId: String,
    val operationNumber: String,
    val side: OrderSide,
    val symbol: String,
    val quantity: Int,
    val executedPrice: Money,
    val total: Money,
    val accountId: String,
    val accountAvailableBalance: Money,
    val movementId: String,
    val holding: OrderHolding
)

/** Sin [averagePrice] cuando la tenencia quedó en 0. */
data class OrderHolding(val quantity: Int, val averagePrice: Money?)
