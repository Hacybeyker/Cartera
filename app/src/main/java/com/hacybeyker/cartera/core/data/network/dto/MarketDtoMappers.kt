package com.hacybeyker.cartera.core.data.network.dto

import com.hacybeyker.cartera.core.domain.market.FxQuote
import com.hacybeyker.cartera.core.domain.market.HistoryRange
import com.hacybeyker.cartera.core.domain.market.PriceHistory
import com.hacybeyker.cartera.core.domain.market.PricePoint
import com.hacybeyker.cartera.core.domain.market.Quote
import com.hacybeyker.cartera.core.domain.market.StockDetail
import com.hacybeyker.cartera.core.domain.portfolio.Holding
import com.hacybeyker.cartera.core.domain.portfolio.Portfolio
import java.math.RoundingMode
import java.time.OffsetDateTime
import kotlinx.serialization.SerializationException

fun StockQuoteDto.toQuote(): Quote = Quote(
    symbol = symbol,
    name = name,
    currency = currency,
    last = last.toBigDecimal(),
    change = change.toBigDecimal(),
    changePct = changePct.toBigDecimal(),
    ts = OffsetDateTime.parse(ts)
)

fun StockDetailDto.toStockDetail(): StockDetail = StockDetail(
    quote = StockQuoteDto(symbol, name, currency, last, change, changePct, ts).toQuote(),
    open = open.toBigDecimal(),
    high = high.toBigDecimal(),
    low = low.toBigDecimal(),
    previousClose = previousClose.toBigDecimal(),
    volume = volume,
    description = description
)

fun PriceHistoryDto.toPriceHistory(): PriceHistory = PriceHistory(
    symbol = symbol,
    range = HistoryRange.entries.firstOrNull { it.wireName == range }
        ?: throw SerializationException("Rango desconocido: $range"),
    currency = currency,
    points = points.map { PricePoint(ts = OffsetDateTime.parse(it.ts), price = it.price.toBigDecimal()) }
)

fun FxQuoteDto.toFxQuote(): FxQuote = FxQuote(
    pair = pair,
    buy = buy.toBigDecimal(),
    sell = sell.toBigDecimal(),
    quoteId = quoteId,
    expiresAt = OffsetDateTime.parse(expiresAt),
    ts = OffsetDateTime.parse(ts)
)

fun PortfolioDto.toPortfolio(): Portfolio = Portfolio(
    accountId = accountId,
    currency = currency,
    holdings = holdings.map { it.toHolding(currency) }
)

// dayChange = cantidad × (precio − cierre anterior), así que la variación por acción sale exacta al dividir.
private fun PortfolioHoldingDto.toHolding(currency: String): Holding {
    if (quantity <= 0) throw SerializationException("Tenencia sin acciones: $symbol")
    return Holding(
        symbol = symbol,
        name = name,
        quantity = quantity,
        currency = currency,
        averagePrice = averagePrice.value.toBigDecimal(),
        lastPrice = lastPrice.value.toBigDecimal(),
        priceChange = dayChange.value.toBigDecimal().divide(quantity.toBigDecimal(), 2, RoundingMode.HALF_UP)
    )
}
