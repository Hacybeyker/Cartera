package com.hacybeyker.cartera.feature.order.ui

import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.domain.market.MarketStreamStatus
import com.hacybeyker.cartera.core.domain.market.ObserveMarketStreamStatusUseCase
import com.hacybeyker.cartera.core.domain.market.ObserveQuotesUseCase
import com.hacybeyker.cartera.core.domain.market.OrderSide
import com.hacybeyker.cartera.core.testing.FakeAccountRepository
import com.hacybeyker.cartera.core.testing.FakeMarketStream
import com.hacybeyker.cartera.core.testing.FakePortfolioRepository
import com.hacybeyker.cartera.core.testing.MainDispatcherRule
import com.hacybeyker.cartera.core.testing.SequentialKeys
import com.hacybeyker.cartera.core.testing.testQuote
import com.hacybeyker.cartera.feature.order.DollarAccount
import com.hacybeyker.cartera.feature.order.OrderTestAccounts
import com.hacybeyker.cartera.feature.order.ReferenceBuy
import com.hacybeyker.cartera.feature.order.domain.model.OrderField
import com.hacybeyker.cartera.feature.order.domain.model.OrderFieldError
import com.hacybeyker.cartera.feature.order.domain.usecase.LoadOrderFormUseCase
import com.hacybeyker.cartera.feature.order.domain.usecase.ValidateOrderUseCase
import com.hacybeyker.cartera.feature.order.ui.form.OrderFormAction
import com.hacybeyker.cartera.feature.order.ui.form.OrderFormViewModel
import com.hacybeyker.cartera.feature.order.usd
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class OrderFormViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val accounts = FakeAccountRepository(OrderTestAccounts)
    private val portfolio = FakePortfolioRepository()
    private val stream = FakeMarketStream()

    private fun viewModel(symbol: String = "ANDM", side: OrderSide = OrderSide.BUY) = OrderFormViewModel(
        symbol = symbol,
        side = side,
        loadOrderForm = LoadOrderFormUseCase(accounts, portfolio),
        validateOrder = ValidateOrderUseCase(SequentialKeys()),
        observeQuotes = ObserveQuotesUseCase(stream),
        observeStreamStatus = ObserveMarketStreamStatusUseCase(stream)
    )

    /** Recolecta como la pantalla en STARTED; cancelarlo equivale a salir o pasar a segundo plano. */
    private fun TestScope.show(viewModel: OrderFormViewModel): Job =
        backgroundScope.launch { viewModel.uiState.collect {} }.also { runCurrent() }

    /** Como la pantalla: la acción llega y el estado que se recolecta se pone al día. */
    private fun TestScope.act(viewModel: OrderFormViewModel, action: OrderFormAction) {
        viewModel.onAction(action)
        runCurrent()
    }

    private suspend fun TestScope.loadedWithPrice(
        symbol: String = "ANDM",
        side: OrderSide = OrderSide.BUY,
        price: String = "42.10"
    ): OrderFormViewModel {
        val viewModel = viewModel(symbol, side)
        show(viewModel)
        advanceUntilIdle()
        stream.prices.emit(testQuote(symbol, price))
        runCurrent()
        return viewModel
    }

    @Test
    fun `loads only dollar accounts from the cache and preselects the only one`() = runTest {
        val viewModel = viewModel()
        show(viewModel)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(listOf(DollarAccount), state.accounts)
        assertEquals("ACC-003", state.accountId)
        assertFalse(state.hasData)
        assertEquals(1, state.contentKey)
        assertEquals(0, accounts.fetchCalls)
        assertEquals(0, portfolio.calls)
    }

    @Test
    fun `with several dollar accounts none is preselected`() = runTest {
        accounts.accounts = OrderTestAccounts + DollarAccount.copy(id = "ACC-009")
        val viewModel = viewModel()
        show(viewModel)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.accountId)
    }

    @Test
    fun `the estimated total follows the quantity and the live price`() = runTest {
        val viewModel = loadedWithPrice()
        assertNull(viewModel.uiState.value.estimatedTotal)
        assertEquals("Andina Minerals", viewModel.uiState.value.name)

        act(viewModel, OrderFormAction.QuantityChanged("2"))
        assertEquals(usd("84.20"), viewModel.uiState.value.estimatedTotal)

        stream.prices.emit(testQuote("ANDM", "42.50"))
        runCurrent()
        assertEquals(usd("85.00"), viewModel.uiState.value.estimatedTotal)

        act(viewModel, OrderFormAction.QuantityChanged("dos"))
        assertNull(viewModel.uiState.value.estimatedTotal)
    }

    @Test
    fun `without a live price it cannot continue`() = runTest {
        val viewModel = viewModel()
        show(viewModel)
        advanceUntilIdle()
        act(viewModel, OrderFormAction.QuantityChanged("2"))

        act(viewModel, OrderFormAction.ContinueClicked)

        assertFalse(viewModel.uiState.value.canContinue)
        assertNull(viewModel.uiState.value.order)
        assertTrue(viewModel.uiState.value.fieldErrors.isEmpty())
    }

    @Test
    fun `continue with the reference buy opens the order once`() = runTest {
        val viewModel = loadedWithPrice()
        act(viewModel, OrderFormAction.QuantityChanged("2"))

        act(viewModel, OrderFormAction.ContinueClicked)
        assertEquals(ReferenceBuy, viewModel.uiState.value.order)

        act(viewModel, OrderFormAction.OrderOpened)
        assertNull(viewModel.uiState.value.order)

        // Volver de la confirmación y continuar otra vez es otra orden, con otra clave.
        act(viewModel, OrderFormAction.ContinueClicked)
        assertEquals("key-2", viewModel.uiState.value.order?.idempotencyKey)
    }

    @Test
    fun `the form stops funds above the balance and editing clears the error`() = runTest {
        val viewModel = loadedWithPrice()
        act(viewModel, OrderFormAction.QuantityChanged("25"))

        act(viewModel, OrderFormAction.ContinueClicked)
        assertEquals(
            mapOf(OrderField.QUANTITY to OrderFieldError.EXCEEDS_BALANCE),
            viewModel.uiState.value.fieldErrors
        )
        assertNull(viewModel.uiState.value.order)

        act(viewModel, OrderFormAction.QuantityChanged("24"))
        assertTrue(viewModel.uiState.value.fieldErrors.isEmpty())
    }

    @Test
    fun `a sell knows the holding and stops selling more than that`() = runTest {
        val viewModel = loadedWithPrice(symbol = "QORI", side = OrderSide.SELL, price = "115.20")
        assertEquals(2, viewModel.uiState.value.heldQuantity)
        assertEquals(1, portfolio.calls)

        act(viewModel, OrderFormAction.QuantityChanged("3"))
        act(viewModel, OrderFormAction.ContinueClicked)

        assertEquals(
            mapOf(OrderField.QUANTITY to OrderFieldError.EXCEEDS_HOLDINGS),
            viewModel.uiState.value.fieldErrors
        )
    }

    @Test
    fun `the account sheet opens, chooses and closes`() = runTest {
        accounts.accounts = OrderTestAccounts + DollarAccount.copy(id = "ACC-009")
        val viewModel = loadedWithPrice()
        act(viewModel, OrderFormAction.ContinueClicked)
        assertEquals(OrderFieldError.REQUIRED, viewModel.uiState.value.fieldErrors[OrderField.ACCOUNT])

        act(viewModel, OrderFormAction.AccountClicked)
        assertTrue(viewModel.uiState.value.isAccountSheetVisible)
        act(viewModel, OrderFormAction.AccountSelected("ACC-009"))

        val state = viewModel.uiState.value
        assertFalse(state.isAccountSheetVisible)
        assertEquals("ACC-009", state.selectedAccount?.id)
        assertNull(state.fieldErrors[OrderField.ACCOUNT])
        assertTrue(state.hasData)

        act(viewModel, OrderFormAction.AccountClicked)
        act(viewModel, OrderFormAction.AccountSheetDismissed)
        assertFalse(viewModel.uiState.value.isAccountSheetVisible)
    }

    @Test
    fun `back without data exits and with data asks first`() = runTest {
        val viewModel = loadedWithPrice()
        act(viewModel, OrderFormAction.BackClicked)
        assertTrue(viewModel.uiState.value.isExited)

        val withData = loadedWithPrice()
        act(withData, OrderFormAction.QuantityChanged("2"))
        act(withData, OrderFormAction.BackClicked)
        assertTrue(withData.uiState.value.isExitConfirmationVisible)
        assertFalse(withData.uiState.value.isExited)

        act(withData, OrderFormAction.ExitCancelled)
        assertFalse(withData.uiState.value.isExitConfirmationVisible)

        act(withData, OrderFormAction.BackClicked)
        act(withData, OrderFormAction.ExitConfirmed)
        assertTrue(withData.uiState.value.isExited)
    }

    @Test
    fun `a failed load shows the error and retry loads again`() = runTest {
        accounts.error = AppError.Network
        val viewModel = viewModel()
        show(viewModel)
        advanceUntilIdle()
        assertEquals(AppError.Network, viewModel.uiState.value.error)
        assertNull(viewModel.uiState.value.contentKey)

        accounts.error = null
        act(viewModel, OrderFormAction.RetryClicked)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.error)
        assertEquals("ACC-003", viewModel.uiState.value.accountId)
    }

    @Test
    fun `the price is followed only while the screen collects and the banner follows the stream`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        assertEquals(0, stream.activeSubscribers)

        val screen = show(viewModel)
        assertEquals(setOf("ANDM"), stream.subscriptions.single())
        stream.status.value = MarketStreamStatus.RECONNECTING
        runCurrent()
        assertTrue(viewModel.uiState.value.isReconnecting)

        screen.cancel()
        runCurrent()
        assertEquals(0, stream.activeSubscribers)
    }
}
