package com.hacybeyker.cartera.feature.order.domain.model

import com.hacybeyker.cartera.core.domain.Money
import com.hacybeyker.cartera.core.domain.market.OrderSide
import java.math.RoundingMode

/**
 * Una orden validada y lista para confirmar. [referencePrice] es el precio en vivo al tocar Continuar: el servidor
 * ejecuta al suyo. [idempotencyKey] identifica esta orden y se reutiliza en cada reintento.
 */
data class MarketOrder(
    val symbol: String,
    val side: OrderSide,
    val quantity: Int,
    val accountId: String,
    val accountAlias: String,
    val accountNumberMasked: String,
    val referencePrice: Money,
    val idempotencyKey: String
) {
    val estimatedTotal: Money get() = estimatedTotal(referencePrice, quantity)
}

/** Cantidad × precio, a 2 decimales y mitad hacia arriba, como el servidor. */
fun estimatedTotal(price: Money, quantity: Int): Money =
    Money((price.amount * quantity.toBigDecimal()).setScale(2, RoundingMode.HALF_UP), price.currency)
