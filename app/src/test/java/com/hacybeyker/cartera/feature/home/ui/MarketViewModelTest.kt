package com.hacybeyker.cartera.feature.home.ui

import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.domain.Money
import com.hacybeyker.cartera.core.domain.market.MarketStreamStatus
import com.hacybeyker.cartera.core.domain.market.ObserveMarketStreamStatusUseCase
import com.hacybeyker.cartera.core.domain.market.ObserveQuotesUseCase
import com.hacybeyker.cartera.core.domain.portfolio.GetPortfolioRevisionUseCase
import com.hacybeyker.cartera.core.domain.portfolio.GetPortfolioUseCase
import com.hacybeyker.cartera.core.testing.FakeMarketRepository
import com.hacybeyker.cartera.core.testing.FakeMarketStream
import com.hacybeyker.cartera.core.testing.FakePortfolioChanges
import com.hacybeyker.cartera.core.testing.FakePortfolioRepository
import com.hacybeyker.cartera.core.testing.MainDispatcherRule
import com.hacybeyker.cartera.core.testing.TestPortfolio
import com.hacybeyker.cartera.core.testing.TestQuotes
import com.hacybeyker.cartera.core.testing.testQuote
import com.hacybeyker.cartera.feature.home.domain.usecase.LoadMarketUseCase
import com.hacybeyker.cartera.feature.home.ui.market.MarketAction
import com.hacybeyker.cartera.feature.home.ui.market.MarketUiState
import com.hacybeyker.cartera.feature.home.ui.market.MarketViewModel
import com.hacybeyker.cartera.feature.home.ui.market.StockItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MarketViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val market = FakeMarketRepository()
    private val portfolio = FakePortfolioRepository()
    private val changes = FakePortfolioChanges()
    private val stream = FakeMarketStream()

    private fun viewModel() = MarketViewModel(
        loadMarket = LoadMarketUseCase(market, portfolio),
        getPortfolio = GetPortfolioUseCase(portfolio),
        getPortfolioRevision = GetPortfolioRevisionUseCase(changes),
        observeQuotes = ObserveQuotesUseCase(stream),
        observeStreamStatus = ObserveMarketStreamStatusUseCase(stream)
    )

    private fun usd(value: String) = Money(value.toBigDecimal(), "USD")

    /** Recolecta como la pantalla en STARTED; cancelarlo equivale a salir o pasar a segundo plano. */
    private fun TestScope.show(viewModel: MarketViewModel): Job =
        backgroundScope.launch { viewModel.uiState.collect {} }.also { runCurrent() }

    @Test
    fun `loads the stocks and the portfolio header`() = runTest {
        val viewModel = viewModel()
        assertTrue(viewModel.uiState.value.isLoading)
        show(viewModel)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(TestQuotes.map(StockItem::from), state.stocks)
        assertEquals(usd("1094.40"), state.portfolio?.marketValue)
        assertEquals(usd("-9.17"), state.portfolio?.dayChange)
        assertEquals("-0.83", state.portfolio?.dayChangePct?.toPlainString())
        assertEquals(1, state.contentKey)
    }

    @Test
    fun `failure shows the error and retry loads again`() = runTest {
        market.error = AppError.Network
        val viewModel = viewModel()
        show(viewModel)
        advanceUntilIdle()
        assertEquals(AppError.Network, viewModel.uiState.value.error)
        assertNull(viewModel.uiState.value.contentKey)

        market.error = null
        viewModel.onAction(MarketAction.RetryClicked)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.error)
        assertEquals(8, viewModel.uiState.value.stocks.size)
        assertEquals(2, market.stocksCalls)
    }

    @Test
    fun `a tick changes only the row of its symbol and the header if it is held`() = runTest {
        val viewModel = viewModel()
        show(viewModel)
        advanceUntilIdle()
        val before = viewModel.uiState.value

        stream.prices.emit(testQuote("ANDM", "43.10", change = "1.34"))
        runCurrent()

        val after = viewModel.uiState.value
        assertEquals("43.10", after.stocks[0].price.amount.toPlainString())
        before.stocks.indices.drop(1).forEach { assertSame(before.stocks[it], after.stocks[it]) }
        assertEquals(usd("1104.40"), after.portfolio?.marketValue)
        assertEquals(before.contentKey, after.contentKey)
    }

    @Test
    fun `a tick of a stock not held keeps the header`() = runTest {
        val viewModel = viewModel()
        show(viewModel)
        advanceUntilIdle()
        val header = viewModel.uiState.value.portfolio

        stream.prices.emit(testQuote("LIMA", "9.90", change = "-0.23"))
        runCurrent()

        assertEquals("9.90", viewModel.uiState.value.stocks.single { it.symbol == "LIMA" }.price.amount.toPlainString())
        assertSame(header, viewModel.uiState.value.portfolio)
    }

    @Test
    fun `prices are followed only while the screen collects`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        assertEquals(0, stream.activeSubscribers)

        val screen = show(viewModel)
        assertEquals(1, stream.activeSubscribers)
        assertEquals(TestQuotes.map { it.symbol }.toSet(), stream.subscriptions.single())

        screen.cancel()
        runCurrent()

        assertEquals(0, stream.activeSubscribers)
    }

    @Test
    fun `the banner follows the reconnection`() = runTest {
        val viewModel = viewModel()
        show(viewModel)
        advanceUntilIdle()

        stream.status.value = MarketStreamStatus.RECONNECTING
        runCurrent()
        assertTrue(viewModel.uiState.value.isReconnecting)

        stream.status.value = MarketStreamStatus.CONNECTED
        runCurrent()
        assertFalse(viewModel.uiState.value.isReconnecting)
    }

    @Test
    fun `showing Mercado without changes does not ask again`() = runTest {
        val viewModel = viewModel()
        show(viewModel)
        advanceUntilIdle()

        viewModel.onAction(MarketAction.Shown)
        advanceUntilIdle()

        assertEquals(1, portfolio.calls)
        assertEquals(1, market.stocksCalls)
    }

    @Test
    fun `after an order only the portfolio is asked again, at the live prices`() = runTest {
        val viewModel = viewModel()
        show(viewModel)
        advanceUntilIdle()
        stream.prices.emit(testQuote("QORI", "116.20", change = "0.64"))
        runCurrent()

        changes.notifyChanged()
        portfolio.portfolio = TestPortfolio.copy(holdings = TestPortfolio.holdings.filter { it.symbol == "QORI" })
        viewModel.onAction(MarketAction.Shown)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, portfolio.calls)
        assertEquals(1, market.stocksCalls)
        assertEquals(usd("232.40"), state.portfolio?.marketValue)
        assertEquals(2, state.contentKey)
    }

    @Test
    fun `a failed portfolio refresh shows the error`() = runTest {
        val viewModel = viewModel()
        show(viewModel)
        advanceUntilIdle()

        changes.notifyChanged()
        portfolio.error = AppError.Server
        viewModel.onAction(MarketAction.Shown)
        advanceUntilIdle()

        assertEquals(AppError.Server, viewModel.uiState.value.error)
    }

    @Test
    fun `a quote of an unknown symbol leaves the state as it was`() {
        val state = MarketUiState(isLoading = false, stocks = TestQuotes.take(1).map(StockItem::from))

        assertSame(state, state.withQuote(testQuote("QORI", "1.00"), header = null))
        val same = state.withQuote(TestQuotes[0], header = null)
        assertSame(state.stocks, same.stocks)
        assertNotSame(state.stocks, state.withQuote(testQuote("ANDM", "1.00"), header = null).stocks)
    }
}
