package com.hacybeyker.cartera.core.domain

import java.math.BigDecimal

/** Monto exacto en su moneda ISO 4217. El signo indica cargo (negativo) o abono (positivo). */
data class Money(val amount: BigDecimal, val currency: String)
