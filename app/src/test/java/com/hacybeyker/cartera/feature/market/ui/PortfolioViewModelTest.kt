package com.hacybeyker.cartera.feature.market.ui

import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.domain.Money
import com.hacybeyker.cartera.core.domain.market.MarketStreamStatus
import com.hacybeyker.cartera.core.domain.market.ObserveMarketStreamStatusUseCase
import com.hacybeyker.cartera.core.domain.market.ObserveQuotesUseCase
import com.hacybeyker.cartera.core.domain.portfolio.GetPortfolioRevisionUseCase
import com.hacybeyker.cartera.core.domain.portfolio.GetPortfolioUseCase
import com.hacybeyker.cartera.core.testing.FakeMarketStream
import com.hacybeyker.cartera.core.testing.FakePortfolioChanges
import com.hacybeyker.cartera.core.testing.FakePortfolioRepository
import com.hacybeyker.cartera.core.testing.MainDispatcherRule
import com.hacybeyker.cartera.core.testing.TestPortfolio
import com.hacybeyker.cartera.core.testing.testQuote
import com.hacybeyker.cartera.feature.market.ui.portfolio.HoldingItem
import com.hacybeyker.cartera.feature.market.ui.portfolio.PortfolioAction
import com.hacybeyker.cartera.feature.market.ui.portfolio.PortfolioUiState
import com.hacybeyker.cartera.feature.market.ui.portfolio.PortfolioViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PortfolioViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val portfolio = FakePortfolioRepository()
    private val changes = FakePortfolioChanges()
    private val stream = FakeMarketStream()

    private fun viewModel() = PortfolioViewModel(
        getPortfolio = GetPortfolioUseCase(portfolio),
        getPortfolioRevision = GetPortfolioRevisionUseCase(changes),
        observeQuotes = ObserveQuotesUseCase(stream),
        observeStreamStatus = ObserveMarketStreamStatusUseCase(stream)
    )

    private fun usd(value: String) = Money(value.toBigDecimal(), "USD")

    /** Recolecta como la pantalla en STARTED; cancelarlo equivale a salir o pasar a segundo plano. */
    private fun TestScope.show(viewModel: PortfolioViewModel): Job =
        backgroundScope.launch { viewModel.uiState.collect {} }.also { runCurrent() }

    @Test
    fun `loads the holdings and their totals and follows their prices`() = runTest {
        val viewModel = viewModel()
        assertTrue(viewModel.uiState.value.isLoading)
        show(viewModel)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf("ANDM", "MOCH", "QORI"), state.holdings.map { it.symbol })
        assertEquals(usd("40.00"), state.holdings[0].averagePrice)
        assertEquals(usd("42.10"), state.holdings[0].lastPrice)
        assertEquals(usd("21.00"), state.holdings[0].gain)
        assertEquals(usd("1094.40"), state.summary?.marketValue)
        assertEquals(usd("-9.17"), state.summary?.dayChange)
        assertEquals(1, state.contentKey)
        assertEquals(setOf("ANDM", "MOCH", "QORI"), stream.subscriptions.last())
    }

    @Test
    fun `a tick changes only the row of its symbol and the totals`() = runTest {
        val viewModel = viewModel()
        show(viewModel)
        advanceUntilIdle()
        val before = viewModel.uiState.value

        stream.prices.emit(testQuote("MOCH", "90.60", change = "-0.37"))
        runCurrent()

        val after = viewModel.uiState.value
        assertEquals(usd("453.00"), after.holdings[1].marketValue)
        assertEquals(usd("53.00"), after.holdings[1].gain)
        assertSame(before.holdings[0], after.holdings[0])
        assertSame(before.holdings[2], after.holdings[2])
        assertEquals(usd("1104.40"), after.summary?.marketValue)
        assertEquals(before.contentKey, after.contentKey)
    }

    @Test
    fun `an empty portfolio shows the empty state and does not open the stream`() = runTest {
        portfolio.portfolio = TestPortfolio.copy(holdings = emptyList())
        val viewModel = viewModel()
        show(viewModel)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.holdings.isEmpty())
        assertEquals(1, viewModel.uiState.value.contentKey)
        assertTrue(stream.subscriptions.isEmpty())
    }

    @Test
    fun `failure shows the error and retry loads again`() = runTest {
        portfolio.error = AppError.Network
        val viewModel = viewModel()
        show(viewModel)
        advanceUntilIdle()
        assertEquals(AppError.Network, viewModel.uiState.value.error)
        assertNull(viewModel.uiState.value.contentKey)

        portfolio.error = null
        viewModel.onAction(PortfolioAction.RetryClicked)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.error)
        assertEquals(3, viewModel.uiState.value.holdings.size)
        assertEquals(2, portfolio.calls)
    }

    @Test
    fun `showing it again without changes does not ask again`() = runTest {
        val viewModel = viewModel()
        show(viewModel)
        advanceUntilIdle()

        viewModel.onAction(PortfolioAction.Shown)
        advanceUntilIdle()

        assertEquals(1, portfolio.calls)
        assertEquals(1, viewModel.uiState.value.contentKey)
    }

    @Test
    fun `after an order it is asked again and valued at the live prices`() = runTest {
        val viewModel = viewModel()
        show(viewModel)
        advanceUntilIdle()
        stream.prices.emit(testQuote("QORI", "116.20", change = "0.64"))
        runCurrent()

        changes.notifyChanged()
        portfolio.portfolio = TestPortfolio.copy(holdings = TestPortfolio.holdings.filter { it.symbol == "QORI" })
        viewModel.onAction(PortfolioAction.Shown)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, portfolio.calls)
        assertEquals(listOf("QORI"), state.holdings.map { it.symbol })
        assertEquals(usd("232.40"), state.summary?.marketValue)
        assertEquals(2, state.contentKey)
        assertEquals(setOf("QORI"), stream.subscriptions.last())
    }

    @Test
    fun `a failed refresh shows the error`() = runTest {
        val viewModel = viewModel()
        show(viewModel)
        advanceUntilIdle()

        changes.notifyChanged()
        portfolio.error = AppError.Server
        viewModel.onAction(PortfolioAction.Shown)
        advanceUntilIdle()

        assertEquals(AppError.Server, viewModel.uiState.value.error)
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
    fun `prices are followed only while the screen collects`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        assertEquals(0, stream.activeSubscribers)

        val screen = show(viewModel)
        assertEquals(1, stream.activeSubscribers)

        screen.cancel()
        runCurrent()

        assertEquals(0, stream.activeSubscribers)
    }

    @Test
    fun `revaluing a symbol that is not listed leaves the state as it was`() {
        val state = PortfolioUiState().loaded(TestPortfolio.copy(holdings = TestPortfolio.holdings.take(1)))

        assertSame(state, state.revalued(TestPortfolio, "QORI"))
        assertSame(state, state.revalued(TestPortfolio.copy(holdings = emptyList()), "ANDM"))
        assertEquals(HoldingItem.from(TestPortfolio.holdings[0]), state.holdings.single())
    }
}
