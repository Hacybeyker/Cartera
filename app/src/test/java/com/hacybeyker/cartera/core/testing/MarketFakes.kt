package com.hacybeyker.cartera.core.testing

import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.core.domain.market.FxQuote
import com.hacybeyker.cartera.core.domain.market.HistoryRange
import com.hacybeyker.cartera.core.domain.market.MarketRepository
import com.hacybeyker.cartera.core.domain.market.MarketStream
import com.hacybeyker.cartera.core.domain.market.MarketStreamStatus
import com.hacybeyker.cartera.core.domain.market.PriceHistory
import com.hacybeyker.cartera.core.domain.market.PricePoint
import com.hacybeyker.cartera.core.domain.market.Quote
import com.hacybeyker.cartera.core.domain.market.StockDetail
import com.hacybeyker.cartera.core.domain.portfolio.Portfolio
import com.hacybeyker.cartera.core.domain.portfolio.PortfolioChanges
import com.hacybeyker.cartera.core.domain.portfolio.PortfolioRepository
import com.hacybeyker.cartera.core.ui.preview.previewPortfolio
import com.hacybeyker.cartera.core.ui.preview.previewQuotes
import java.math.BigDecimal
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow

val TestQuotes: List<Quote> = previewQuotes
val TestPortfolio: Portfolio = previewPortfolio

fun testQuote(symbol: String, last: String, change: String = "0.00"): Quote =
    TestQuotes.first { it.symbol == symbol }.copy(last = last.toBigDecimal(), change = change.toBigDecimal())

class FakeMarketRepository(var stocks: List<Quote> = TestQuotes, var error: AppError? = null) : MarketRepository {
    var stocksCalls = 0
    val requests = mutableListOf<String>()

    /** Si no es null, `stock()` espera a que se complete: deja ver lo que pasa durante la carga. */
    var stockGate: CompletableDeferred<Unit>? = null

    /** Los precios de cada historial; sin ellos, un solo punto con el precio vigente. */
    var historyPrices: List<BigDecimal>? = null
    var historyError: AppError? = null

    override suspend fun stocks(): Outcome<List<Quote>> {
        stocksCalls++
        return error?.let { Outcome.Failure(it) } ?: Outcome.Success(stocks)
    }

    override suspend fun stock(symbol: String): Outcome<StockDetail> {
        requests += symbol
        stockGate?.await()
        val quote = stocks.firstOrNull { it.symbol == symbol }
        val failure = error
        return when {
            failure != null -> Outcome.Failure(failure)
            quote == null -> Outcome.Failure(AppError.NotFound)
            else -> Outcome.Success(
                StockDetail(quote, quote.last, quote.last, quote.last, quote.last - quote.change, 1_000, "Descripción")
            )
        }
    }

    override suspend fun history(symbol: String, range: HistoryRange): Outcome<PriceHistory> {
        requests += "$symbol/${range.wireName}"
        val quote = stocks.firstOrNull { it.symbol == symbol }
        val failure = historyError
        return when {
            failure != null -> Outcome.Failure(failure)
            quote == null -> Outcome.Failure(AppError.NotFound)
            else -> {
                val prices = historyPrices ?: listOf(quote.last)
                Outcome.Success(PriceHistory(symbol, range, "USD", prices.map { PricePoint(quote.ts, it) }))
            }
        }
    }
}

class FakePortfolioRepository(var portfolio: Portfolio = TestPortfolio, var error: AppError? = null) :
    PortfolioRepository {
    var calls = 0

    override suspend fun portfolio(): Outcome<Portfolio> {
        calls++
        return error?.let { Outcome.Failure(it) } ?: Outcome.Success(portfolio)
    }
}

class FakePortfolioChanges(override var revision: Int = 0) : PortfolioChanges {
    override fun notifyChanged() {
        revision++
    }
}

/** Canal en vivo sin red: [prices] reparte a cada suscriptor los precios de sus símbolos. */
class FakeMarketStream : MarketStream {
    val prices = MutableSharedFlow<Quote>(extraBufferCapacity = 64)
    val subscriptions = mutableListOf<Set<String>>()
    var activeSubscribers = 0
        private set

    override val status = MutableStateFlow(MarketStreamStatus.IDLE)

    override fun quotes(symbols: Set<String>): Flow<Quote> = flow {
        subscriptions += symbols
        activeSubscribers++
        try {
            emitAll(prices.filter { it.symbol in symbols })
        } finally {
            activeSubscribers--
        }
    }

    override fun fxQuotes(pair: String): Flow<FxQuote> = emptyFlow()
}
