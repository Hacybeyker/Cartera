package com.hacybeyker.cartera.feature.order.domain.model

import com.hacybeyker.cartera.core.domain.Money
import com.hacybeyker.cartera.core.domain.market.OrderSide

/** Lo que la persona eligió en el formulario, todavía sin validar, con el precio en vivo de ese momento. */
data class OrderDraft(
    val symbol: String,
    val side: OrderSide,
    val quantity: String,
    val accountId: String?,
    val price: Money
)
