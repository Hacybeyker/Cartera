package com.hacybeyker.cartera.feature.market.ui

import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.domain.Money
import com.hacybeyker.cartera.core.domain.market.GetPriceHistoryUseCase
import com.hacybeyker.cartera.core.domain.market.HistoryRange
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
import com.hacybeyker.cartera.core.testing.testQuote
import com.hacybeyker.cartera.feature.market.domain.usecase.LoadStockDetailUseCase
import com.hacybeyker.cartera.feature.market.ui.detail.StockDetailAction
import com.hacybeyker.cartera.feature.market.ui.detail.StockDetailViewModel
import kotlinx.coroutines.CompletableDeferred
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

class StockDetailViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val market = FakeMarketRepository().apply {
        historyPrices = listOf("114.80", "116.10", "115.50", "115.20").map { it.toBigDecimal() }
    }
    private val portfolio = FakePortfolioRepository()
    private val stream = FakeMarketStream()
    private val changes = FakePortfolioChanges()

    private fun viewModel(symbol: String = "QORI") = StockDetailViewModel(
        symbol = symbol,
        loadStockDetail = LoadStockDetailUseCase(market, portfolio),
        getPriceHistory = GetPriceHistoryUseCase(market),
        observeQuotes = ObserveQuotesUseCase(stream),
        observeStreamStatus = ObserveMarketStreamStatusUseCase(stream),
        getPortfolio = GetPortfolioUseCase(portfolio),
        getPortfolioRevision = GetPortfolioRevisionUseCase(changes)
    )

    private fun usd(value: String) = Money(value.toBigDecimal(), "USD")

    /** Recolecta como la pantalla en STARTED; cancelarlo equivale a salir o pasar a segundo plano. */
    private fun TestScope.show(viewModel: StockDetailViewModel): Job =
        backgroundScope.launch { viewModel.uiState.collect {} }.also { runCurrent() }

    @Test
    fun `loads the price, the day data, the 1D chart and the holding`() = runTest {
        val viewModel = viewModel()
        show(viewModel)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Qori Gold", state.name)
        assertEquals(usd("115.20"), state.price?.price)
        assertEquals(usd("115.56"), state.day?.previousClose)
        assertEquals(HistoryRange.DAY, state.range)
        assertEquals(3, state.chart.chart?.series?.size)
        assertEquals("115.20", state.chart.chart?.last?.toPlainString())
        assertEquals(2, state.holding?.quantity)
        assertEquals(usd("230.40"), state.holding?.marketValue)
        assertEquals(usd("-9.60"), state.holding?.gain)
        assertEquals(1, state.contentKey)
        assertEquals(listOf("QORI", "QORI/1D"), market.requests)
    }

    @Test
    fun `a stock that is not held shows no holding`() = runTest {
        val viewModel = viewModel("LIMA")
        show(viewModel)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.holding)
        assertEquals(usd("9.85"), viewModel.uiState.value.price?.price)
    }

    @Test
    fun `a tick moves the price, the holding and the last point of the 1D chart`() = runTest {
        val viewModel = viewModel()
        show(viewModel)
        advanceUntilIdle()
        val before = viewModel.uiState.value

        stream.prices.emit(testQuote("QORI", "116.20", change = "0.64"))
        runCurrent()

        val after = viewModel.uiState.value
        assertEquals(usd("116.20"), after.price?.price)
        assertEquals("0.64", after.price?.change?.toPlainString())
        assertEquals(usd("232.40"), after.holding?.marketValue)
        assertEquals("116.20", after.chart.chart?.last?.toPlainString())
        assertSame(before.chart.chart?.series, after.chart.chart?.series)
        assertSame(before.day, after.day)
        assertEquals(before.contentKey, after.contentKey)
    }

    @Test
    fun `changing the range loads its history and ticks no longer move the chart`() = runTest {
        val viewModel = viewModel()
        show(viewModel)
        advanceUntilIdle()

        viewModel.onAction(StockDetailAction.RangeSelected(HistoryRange.WEEK))
        runCurrent()
        assertEquals(HistoryRange.WEEK, viewModel.uiState.value.range)
        advanceUntilIdle()
        val week = viewModel.uiState.value
        stream.prices.emit(testQuote("QORI", "117.00", change = "1.44"))
        runCurrent()

        assertEquals(listOf("QORI", "QORI/1D", "QORI/1W"), market.requests)
        assertEquals(HistoryRange.WEEK, week.range)
        assertEquals(2, week.contentKey)
        assertEquals(usd("117.00"), viewModel.uiState.value.price?.price)
        assertSame(week.chart, viewModel.uiState.value.chart)
    }

    @Test
    fun `a new range joins the history with the latest price`() = runTest {
        val viewModel = viewModel()
        show(viewModel)
        advanceUntilIdle()
        stream.prices.emit(testQuote("QORI", "116.80", change = "1.24"))
        runCurrent()

        viewModel.onAction(StockDetailAction.RangeSelected(HistoryRange.YEAR))
        advanceUntilIdle()

        assertEquals("116.80", viewModel.uiState.value.chart.chart?.last?.toPlainString())
    }

    @Test
    fun `choosing the range already shown does nothing`() = runTest {
        val viewModel = viewModel()
        show(viewModel)
        advanceUntilIdle()

        viewModel.onAction(StockDetailAction.RangeSelected(HistoryRange.DAY))
        advanceUntilIdle()

        assertEquals(listOf("QORI", "QORI/1D"), market.requests)
        assertEquals(1, viewModel.uiState.value.contentKey)
    }

    @Test
    fun `a failed range keeps the detail and retries only the chart`() = runTest {
        val viewModel = viewModel()
        show(viewModel)
        advanceUntilIdle()

        market.historyError = AppError.Network
        viewModel.onAction(StockDetailAction.RangeSelected(HistoryRange.MONTH))
        advanceUntilIdle()
        val failed = viewModel.uiState.value
        assertEquals(AppError.Network, failed.chart.error)
        assertNull(failed.error)
        assertEquals(1, failed.contentKey)

        market.historyError = null
        viewModel.onAction(StockDetailAction.ChartRetryClicked)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.chart.error)
        assertEquals(listOf("QORI", "QORI/1D", "QORI/1M", "QORI/1M"), market.requests)
        assertEquals(2, viewModel.uiState.value.contentKey)
    }

    @Test
    fun `a missing stock shows not found`() = runTest {
        val viewModel = viewModel("NOPE")
        show(viewModel)
        advanceUntilIdle()

        assertEquals(AppError.NotFound, viewModel.uiState.value.error)
        assertNull(viewModel.uiState.value.contentKey)
    }

    @Test
    fun `failure shows the error and retry loads again`() = runTest {
        market.error = AppError.Server
        val viewModel = viewModel()
        show(viewModel)
        advanceUntilIdle()
        assertEquals(AppError.Server, viewModel.uiState.value.error)
        stream.prices.emit(testQuote("QORI", "116.00"))
        runCurrent()
        assertNull(viewModel.uiState.value.price)

        market.error = null
        viewModel.onAction(StockDetailAction.RetryClicked)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.error)
        assertEquals(1, viewModel.uiState.value.contentKey)
    }

    @Test
    fun `a newer live price that arrives during the load wins`() = runTest {
        val gate = CompletableDeferred<Unit>().also { market.stockGate = it }
        val viewModel = viewModel()
        show(viewModel)
        val newer = testQuote("QORI", "116.40", change = "0.84").let { it.copy(ts = it.ts.plusSeconds(2)) }
        stream.prices.emit(newer)
        runCurrent()

        gate.complete(Unit)
        advanceUntilIdle()

        assertEquals(usd("116.40"), viewModel.uiState.value.price?.price)
        assertEquals("116.40", viewModel.uiState.value.chart.chart?.last?.toPlainString())
        assertEquals(usd("232.80"), viewModel.uiState.value.holding?.marketValue)
    }

    @Test
    fun `an older live price that arrives during the load loses`() = runTest {
        val gate = CompletableDeferred<Unit>().also { market.stockGate = it }
        val viewModel = viewModel()
        show(viewModel)
        val older = testQuote("QORI", "114.00").let { it.copy(ts = it.ts.minusSeconds(2)) }
        stream.prices.emit(older)
        runCurrent()

        gate.complete(Unit)
        advanceUntilIdle()

        assertEquals(usd("115.20"), viewModel.uiState.value.price?.price)
    }

    @Test
    fun `the price is followed only while the screen collects`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        assertEquals(0, stream.activeSubscribers)

        val screen = show(viewModel)
        assertEquals(setOf("QORI"), stream.subscriptions.single())

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
    fun `shown asks for the portfolio again only after an order changed the holdings`() = runTest {
        val viewModel = viewModel()
        show(viewModel)
        advanceUntilIdle()
        viewModel.onAction(StockDetailAction.Shown)
        advanceUntilIdle()
        assertEquals(1, portfolio.calls)

        portfolio.portfolio = TestPortfolio.copy(
            holdings = TestPortfolio.holdings.map { if (it.symbol == "QORI") it.copy(quantity = 1) else it }
        )
        changes.notifyChanged()
        stream.prices.emit(testQuote("QORI", "116.00", "0.44"))
        runCurrent()
        viewModel.onAction(StockDetailAction.Shown)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, portfolio.calls)
        assertEquals(1, state.holding?.quantity)
        // La tenencia nueva se valoriza al último precio del canal, no al de la respuesta.
        assertEquals(usd("116.00"), state.holding?.marketValue)
        assertEquals(listOf("QORI", "QORI/1D"), market.requests)

        viewModel.onAction(StockDetailAction.Shown)
        advanceUntilIdle()
        assertEquals(2, portfolio.calls)
    }

    @Test
    fun `selling everything removes the holding and a failed refresh is tried again`() = runTest {
        val viewModel = viewModel()
        show(viewModel)
        advanceUntilIdle()

        changes.notifyChanged()
        portfolio.error = AppError.Network
        viewModel.onAction(StockDetailAction.Shown)
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.holding?.quantity)

        portfolio.error = null
        portfolio.portfolio = TestPortfolio.copy(holdings = TestPortfolio.holdings.filter { it.symbol != "QORI" })
        viewModel.onAction(StockDetailAction.Shown)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.holding)
        assertFalse(viewModel.uiState.value.canSell)
        assertEquals(3, portfolio.calls)
    }

    @Test
    fun `shown before the detail loads does nothing`() = runTest {
        market.stockGate = CompletableDeferred()
        val viewModel = viewModel()
        show(viewModel)
        changes.notifyChanged()

        viewModel.onAction(StockDetailAction.Shown)
        runCurrent()

        assertEquals(1, portfolio.calls)
    }

    @Test
    fun `operar opens the trade sheet and sell needs a holding`() = runTest {
        val held = viewModel()
        val notHeld = viewModel("LIMA")
        show(held)
        show(notHeld)
        advanceUntilIdle()

        held.onAction(StockDetailAction.TradeClicked)
        runCurrent()
        assertTrue(held.uiState.value.isTradeSheetVisible)
        assertTrue(held.uiState.value.canSell)
        assertFalse(notHeld.uiState.value.canSell)

        held.onAction(StockDetailAction.TradeSheetDismissed)
        runCurrent()
        assertFalse(held.uiState.value.isTradeSheetVisible)
    }
}
