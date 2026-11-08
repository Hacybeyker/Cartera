package com.hacybeyker.cartera.feature.home.domain

import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.core.testing.FakeMarketRepository
import com.hacybeyker.cartera.core.testing.FakePortfolioRepository
import com.hacybeyker.cartera.core.testing.TestPortfolio
import com.hacybeyker.cartera.core.testing.TestQuotes
import com.hacybeyker.cartera.core.testing.testQuote
import com.hacybeyker.cartera.feature.home.domain.model.MarketOverview
import com.hacybeyker.cartera.feature.home.domain.usecase.LoadMarketUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class LoadMarketUseCaseTest {
    private val market = FakeMarketRepository()
    private val portfolio = FakePortfolioRepository()

    @Test
    fun `asks for the stocks and the portfolio`() = runTest {
        val outcome = LoadMarketUseCase(market, portfolio)()

        assertEquals(Outcome.Success(MarketOverview(TestQuotes, TestPortfolio)), outcome)
        assertEquals(1, market.stocksCalls)
        assertEquals(1, portfolio.calls)
    }

    @Test
    fun `the portfolio is valued at the prices of the list`() = runTest {
        market.stocks = TestQuotes.map { if (it.symbol == "QORI") testQuote("QORI", "116.20", "0.64") else it }

        val overview = (LoadMarketUseCase(market, portfolio)() as Outcome.Success).value

        assertEquals("116.20", overview.portfolio.holdings.single { it.symbol == "QORI" }.lastPrice.toPlainString())
        assertEquals("1096.40", overview.portfolio.totals.marketValue.amount.toPlainString())
    }

    @Test
    fun `fails if either call fails`() = runTest {
        market.error = AppError.Network
        assertEquals(Outcome.Failure(AppError.Network), LoadMarketUseCase(market, portfolio)())

        market.error = null
        portfolio.error = AppError.Server
        assertEquals(Outcome.Failure(AppError.Server), LoadMarketUseCase(market, portfolio)())
    }
}
