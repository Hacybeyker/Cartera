package com.hacybeyker.cartera.core.domain.exchange

import com.hacybeyker.cartera.core.domain.market.FxQuote
import java.math.BigDecimal
import java.math.RoundingMode

/** Sentido del cambio entre una cuenta en dólares y una en soles. */
enum class ExchangeDirection(val sourceCurrency: String, val destinationCurrency: String) {
    /** El banco compra los dólares: se aplica la tasa de compra. */
    USD_TO_PEN(sourceCurrency = "USD", destinationCurrency = "PEN"),

    /** El banco vende los dólares: se aplica la tasa de venta. */
    PEN_TO_USD(sourceCurrency = "PEN", destinationCurrency = "USD");

    val reversed: ExchangeDirection
        get() = when (this) {
            USD_TO_PEN -> PEN_TO_USD
            PEN_TO_USD -> USD_TO_PEN
        }

    fun rate(quote: FxQuote): BigDecimal = when (this) {
        USD_TO_PEN -> quote.buy
        PEN_TO_USD -> quote.sell
    }

    /**
     * Lo que se acredita por [amount], en la moneda de origen, a [rate]: monto × tasa si el banco compra dólares y
     * monto ÷ tasa si los vende. A 2 decimales y mitad hacia arriba, igual que el servidor.
     */
    fun credit(amount: BigDecimal, rate: BigDecimal): BigDecimal = when (this) {
        USD_TO_PEN -> (amount * rate).setScale(CENTS, RoundingMode.HALF_UP)
        PEN_TO_USD -> amount.divide(rate, CENTS, RoundingMode.HALF_UP)
    }

    private companion object {
        const val CENTS = 2
    }
}
