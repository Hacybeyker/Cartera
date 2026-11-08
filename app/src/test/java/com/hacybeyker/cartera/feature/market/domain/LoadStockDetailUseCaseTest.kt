package com.hacybeyker.cartera.feature.market.domain

import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.domain.Money
import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.core.domain.market.HistoryRange
import com.hacybeyker.cartera.core.testing.FakeMarketRepository
import com.hacybeyker.cartera.core.testing.FakePortfolioRepository
import com.hacybeyker.cartera.core.testing.testQuote
import com.hacybeyker.cartera.feature.market.domain.usecase.LoadStockDetailUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LoadStockDetailUseCaseTest {
    private val market = FakeMarketRepository()
    private val portfolio = FakePortfolioRepository()
    private val load = LoadStockDetailUseCase(market, portfolio)

    @Test
    fun `brings the stock, its history and the own holding valued at that price`() = runTest {
        market.stocks = market.stocks.map { if (it.symbol == "QORI") testQuote("QORI", "116.20", "0.64") else it }

        val overview = (load("QORI", HistoryRange.MONTH) as Outcome.Success).value

        assertEquals("116.20", overview.stock.quote.last.toPlainString())
        assertEquals(HistoryRange.MONTH, overview.history.range)
        assertEquals(Money("232.40".toBigDecimal(), "USD"), overview.holding?.marketValue)
        assertEquals(listOf("QORI", "QORI/1M"), market.requests)
        assertEquals(1, portfolio.calls)
    }

    @Test
    fun `a stock that is not held has no holding`() = runTest {
        val overview = (load("LIMA", HistoryRange.DAY) as Outcome.Success).value

        assertNull(overview.holding)
    }

    @Test
    fun `a missing stock wins over other failures`() = runTest {
        portfolio.error = AppError.Network

        assertEquals(Outcome.Failure(AppError.NotFound), load("NOPE", HistoryRange.DAY))
    }

    @Test
    fun `a failed history or portfolio fails the load`() = runTest {
        market.historyError = AppError.Server
        assertEquals(Outcome.Failure(AppError.Server), load("QORI", HistoryRange.DAY))

        market.historyError = null
        portfolio.error = AppError.Network
        assertEquals(Outcome.Failure(AppError.Network), load("QORI", HistoryRange.DAY))
    }
}
