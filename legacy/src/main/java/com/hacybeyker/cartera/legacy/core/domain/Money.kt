package com.hacybeyker.cartera.legacy.core.domain

import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

const val PEN = "PEN"

/** Monto exacto en su moneda ISO 4217. */
data class Money(val amount: BigDecimal, val currency: String) {

    operator fun plus(other: Money): Money {
        require(currency == other.currency) { "No se suman montos de $currency y ${other.currency}" }
        return Money(amount + other.amount, currency)
    }

    /** "S/ 85.40" o "S/ 2,122.50": dos decimales y miles con coma, como la constancia del servidor. */
    fun format(): String {
        val number = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US)).format(amount)
        return "${symbolOf(currency)} $number"
    }

    companion object {
        fun zero(currency: String): Money = Money(BigDecimal.ZERO.setScale(2), currency)

        private fun symbolOf(currency: String): String = when (currency) {
            PEN -> "S/"
            "USD" -> "US$"
            else -> currency
        }
    }
}
