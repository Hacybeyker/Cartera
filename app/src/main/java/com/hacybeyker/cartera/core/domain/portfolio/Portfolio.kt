package com.hacybeyker.cartera.core.domain.portfolio

import com.hacybeyker.cartera.core.domain.Money
import com.hacybeyker.cartera.core.domain.market.Quote
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Una tenencia valorizada a [lastPrice]. [priceChange] es la variación del precio contra el cierre anterior, por
 * acción: con ella sale la variación del día sin conocer el cierre.
 */
data class Holding(
    val symbol: String,
    val name: String,
    val quantity: Int,
    val currency: String,
    val averagePrice: BigDecimal,
    val lastPrice: BigDecimal,
    val priceChange: BigDecimal
) {
    val marketValue: Money get() = money(lastPrice * shares)
    val cost: Money get() = money(averagePrice * shares)
    val gain: Money get() = money(marketValue.amount - cost.amount)
    val gainPct: BigDecimal get() = percent(gain.amount, cost.amount)
    val dayChange: Money get() = money(priceChange * shares)

    /** La misma tenencia al precio de [quote]; otra acción la deja igual. */
    fun withQuote(quote: Quote): Holding = if (quote.symbol != symbol) {
        this
    } else {
        copy(lastPrice = quote.last, priceChange = quote.change)
    }

    private val shares: BigDecimal get() = quantity.toBigDecimal()

    private fun money(amount: BigDecimal) = Money(amount.setScale(2, RoundingMode.HALF_UP), currency)
}

data class PortfolioTotals(
    val marketValue: Money,
    val cost: Money,
    val gain: Money,
    val gainPct: BigDecimal,
    val dayChange: Money,
    val dayChangePct: BigDecimal
)

/**
 * Tenencias liquidadas en [accountId]. Los totales se calculan en el cliente, así siguen a los precios en vivo con
 * las mismas reglas del servidor: ganancia sobre el costo y variación del día sobre el valor al cierre anterior.
 */
data class Portfolio(val accountId: String, val currency: String, val holdings: List<Holding>) {

    val totals: PortfolioTotals
        get() {
            val marketValue = holdings.sumOf { it.marketValue.amount }
            val cost = holdings.sumOf { it.cost.amount }
            val dayChange = holdings.sumOf { it.dayChange.amount }
            return PortfolioTotals(
                marketValue = money(marketValue),
                cost = money(cost),
                gain = money(marketValue - cost),
                gainPct = percent(marketValue - cost, cost),
                dayChange = money(dayChange),
                dayChangePct = percent(dayChange, marketValue - dayChange)
            )
        }

    fun holds(symbol: String): Boolean = holdings.any { it.symbol == symbol }

    /** Revaloriza la tenencia de [quote]; si no hay tenencia de esa acción, devuelve el mismo portafolio. */
    fun withQuote(quote: Quote): Portfolio = if (holds(quote.symbol)) {
        copy(holdings = holdings.map { it.withQuote(quote) })
    } else {
        this
    }

    fun withQuotes(quotes: Collection<Quote>): Portfolio = quotes.fold(this) { portfolio, quote ->
        portfolio.withQuote(quote)
    }

    private fun money(amount: BigDecimal) = Money(amount.setScale(2, RoundingMode.HALF_UP), currency)
}

private val HUNDRED = BigDecimal(100)

/** Porcentaje con 2 decimales, mitad hacia arriba; cero si la base es cero. */
private fun percent(part: BigDecimal, base: BigDecimal): BigDecimal = if (base.signum() == 0) {
    BigDecimal.ZERO.setScale(2)
} else {
    part.multiply(HUNDRED).divide(base, 2, RoundingMode.HALF_UP)
}
