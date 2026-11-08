package com.hacybeyker.cartera.core.domain.market

import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.core.domain.portfolio.GetPortfolioRevisionUseCase
import com.hacybeyker.cartera.core.domain.portfolio.GetPortfolioUseCase
import com.hacybeyker.cartera.core.testing.FakeMarketRepository
import com.hacybeyker.cartera.core.testing.FakeMarketStream
import com.hacybeyker.cartera.core.testing.FakePortfolioChanges
import com.hacybeyker.cartera.core.testing.FakePortfolioRepository
import com.hacybeyker.cartera.core.testing.TestPortfolio
import com.hacybeyker.cartera.core.testing.testQuote
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class MarketUseCasesTest {
    private val market = FakeMarketRepository()

    @Test
    fun `stock and history ask the repository for that symbol`() = runTest {
        val detail = (GetStockUseCase(market)("QORI") as Outcome.Success).value
        val history = (GetPriceHistoryUseCase(market)("QORI", HistoryRange.WEEK) as Outcome.Success).value

        assertEquals("115.20", detail.quote.last.toPlainString())
        assertEquals(HistoryRange.WEEK, history.range)
        assertEquals(listOf("QORI", "QORI/1W"), market.requests)
        assertEquals(Outcome.Failure(AppError.NotFound), GetStockUseCase(market)("NOPE"))
    }

    @Test
    fun `portfolio and its revision come from their sources`() = runTest {
        val changes = FakePortfolioChanges(revision = 3)

        assertEquals(Outcome.Success(TestPortfolio), GetPortfolioUseCase(FakePortfolioRepository())())
        assertEquals(3, GetPortfolioRevisionUseCase(changes)())
    }

    @Test
    fun `live quotes and the stream status come from the stream`() = runTest {
        val stream = FakeMarketStream()
        val received = mutableListOf<Quote>()
        backgroundScope.launch { ObserveQuotesUseCase(stream)(setOf("ANDM")).collect { received += it } }
        runCurrent()

        stream.prices.emit(testQuote("ANDM", "42.20"))
        stream.prices.emit(testQuote("QORI", "115.30"))
        runCurrent()
        stream.status.value = MarketStreamStatus.RECONNECTING

        assertEquals(listOf("ANDM"), received.map { it.symbol })
        assertEquals(MarketStreamStatus.RECONNECTING, ObserveMarketStreamStatusUseCase(stream)().first())
    }

    @Test
    fun `history ranges use the contract names`() {
        assertEquals(listOf("1D", "1W", "1M", "1Y"), HistoryRange.entries.map { it.wireName })
    }
}
