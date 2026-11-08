package com.hacybeyker.cartera.feature.home.ui

import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.domain.Money
import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.core.domain.account.GetAccountsRevisionUseCase
import com.hacybeyker.cartera.core.domain.account.GetAccountsUseCase
import com.hacybeyker.cartera.core.domain.exchange.ExchangeDirection
import com.hacybeyker.cartera.core.domain.exchange.ExchangeField
import com.hacybeyker.cartera.core.domain.exchange.ExchangeFieldError
import com.hacybeyker.cartera.core.domain.market.MarketStreamStatus
import com.hacybeyker.cartera.core.domain.market.ObserveFxQuotesUseCase
import com.hacybeyker.cartera.core.domain.market.ObserveMarketStreamStatusUseCase
import com.hacybeyker.cartera.core.testing.FakeAccountChanges
import com.hacybeyker.cartera.core.testing.FakeAccountRepository
import com.hacybeyker.cartera.core.testing.FakeMarketRepository
import com.hacybeyker.cartera.core.testing.FakeMarketStream
import com.hacybeyker.cartera.core.testing.MainDispatcherRule
import com.hacybeyker.cartera.core.testing.SequentialKeys
import com.hacybeyker.cartera.core.testing.TestFxQuote
import com.hacybeyker.cartera.core.ui.preview.previewAccounts
import com.hacybeyker.cartera.feature.home.domain.usecase.LoadExchangeUseCase
import com.hacybeyker.cartera.feature.home.domain.usecase.StartExchangeUseCase
import com.hacybeyker.cartera.feature.home.domain.usecase.ValidateExchangeUseCase
import com.hacybeyker.cartera.feature.home.ui.exchange.ExchangeAccountRole
import com.hacybeyker.cartera.feature.home.ui.exchange.ExchangeAction
import com.hacybeyker.cartera.feature.home.ui.exchange.ExchangeViewModel
import java.math.BigDecimal
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
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ExchangeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val accounts = FakeAccountRepository(previewAccounts)
    private val changes = FakeAccountChanges()
    private val market = FakeMarketRepository()
    private val stream = FakeMarketStream()
    private val validate = ValidateExchangeUseCase(SequentialKeys())

    private fun viewModel() = ExchangeViewModel(
        loadExchange = LoadExchangeUseCase(accounts, market),
        getAccounts = GetAccountsUseCase(accounts),
        getAccountsRevision = GetAccountsRevisionUseCase(changes),
        validateExchange = validate,
        startExchange = StartExchangeUseCase(market, validate),
        observeFxQuotes = ObserveFxQuotesUseCase(stream),
        observeStreamStatus = ObserveMarketStreamStatusUseCase(stream)
    )

    /** Envía [action] y deja correr lo que dispara, como la pantalla entre un frame y otro. */
    private fun TestScope.act(viewModel: ExchangeViewModel, action: ExchangeAction) {
        viewModel.onAction(action)
        runCurrent()
    }

    /** Recolecta como el tab a la vista en STARTED; cancelarlo equivale a cambiar de tab o pasar a segundo plano. */
    private fun TestScope.show(viewModel: ExchangeViewModel): Job =
        backgroundScope.launch { viewModel.uiState.collect {} }.also { runCurrent() }

    private fun pen(value: String) = Money(BigDecimal(value), "PEN")

    private fun usd(value: String) = Money(BigDecimal(value), "USD")

    @Test
    fun `loads the accounts and the quote and preselects the only dollar account`() = runTest {
        val viewModel = viewModel()
        assertTrue(viewModel.uiState.value.isLoading)
        show(viewModel)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(TestFxQuote, state.quote)
        assertEquals(previewAccounts, state.accounts)
        assertEquals(ExchangeDirection.USD_TO_PEN, state.direction)
        assertEquals("ACC-003", state.sourceAccountId)
        // Hay dos cuentas en soles: la persona elige.
        assertNull(state.destinationAccountId)
        assertEquals(1, state.contentKey)
        assertEquals(1, stream.activeFxSubscribers)
    }

    @Test
    fun `the reference amount estimates three hundred seventy four soles and twenty cents`() = runTest {
        val viewModel = loaded()

        act(viewModel, ExchangeAction.AmountChanged("100"))

        assertEquals(pen("374.20"), viewModel.uiState.value.estimatedCredit)
        act(viewModel, ExchangeAction.AmountChanged("abc"))
        assertNull(viewModel.uiState.value.estimatedCredit)
    }

    @Test
    fun `live quotes move the rates and the estimate, and an older one is ignored`() = runTest {
        val viewModel = loaded()
        act(viewModel, ExchangeAction.AmountChanged("100"))

        val newer = TestFxQuote.copy(
            quoteId = "QTE-000002",
            buy = BigDecimal("3.745"),
            ts = TestFxQuote.ts.plusSeconds(2)
        )
        stream.fx.emit(newer)
        runCurrent()
        assertEquals(newer, viewModel.uiState.value.quote)
        assertEquals(pen("374.50"), viewModel.uiState.value.estimatedCredit)

        stream.fx.emit(TestFxQuote.copy(quoteId = "QTE-000000", ts = TestFxQuote.ts.minusSeconds(2)))
        runCurrent()
        assertEquals(newer, viewModel.uiState.value.quote)
        // Las cotizaciones no repiten la marca.
        assertEquals(1, viewModel.uiState.value.contentKey)
    }

    @Test
    fun `the channel is released when the tab stops being collected`() = runTest {
        val viewModel = viewModel()
        val screen = show(viewModel)
        advanceUntilIdle()
        assertEquals(1, stream.activeFxSubscribers)

        screen.cancel()
        runCurrent()

        assertEquals(0, stream.activeFxSubscribers)
    }

    @Test
    fun `a reconnecting channel shows the banner`() = runTest {
        val viewModel = loaded()

        stream.status.value = MarketStreamStatus.RECONNECTING
        runCurrent()
        assertTrue(viewModel.uiState.value.isReconnecting)

        stream.status.value = MarketStreamStatus.CONNECTED
        runCurrent()
        assertFalse(viewModel.uiState.value.isReconnecting)
    }

    @Test
    fun `reversing the direction swaps the accounts and uses the sell rate`() = runTest {
        val viewModel = loaded()
        pickDestination(viewModel, "ACC-002")
        act(viewModel, ExchangeAction.ContinueClicked)
        assertTrue(viewModel.uiState.value.fieldErrors.isNotEmpty())

        act(viewModel, ExchangeAction.DirectionToggled)
        act(viewModel, ExchangeAction.AmountChanged("376.80"))

        val state = viewModel.uiState.value
        assertEquals(ExchangeDirection.PEN_TO_USD, state.direction)
        assertEquals("ACC-002", state.sourceAccountId)
        assertEquals("ACC-003", state.destinationAccountId)
        assertTrue(state.fieldErrors.isEmpty())
        assertEquals(usd("100.00"), state.estimatedCredit)
    }

    @Test
    fun `each account selector offers only the accounts of its currency`() = runTest {
        val viewModel = loaded()

        act(viewModel, ExchangeAction.AccountClicked(ExchangeAccountRole.DESTINATION))
        val state = viewModel.uiState.value
        assertEquals(ExchangeAccountRole.DESTINATION, state.accountSheetFor)
        assertEquals(listOf("ACC-001", "ACC-002"), state.accountsFor(ExchangeAccountRole.DESTINATION).map { it.id })
        assertEquals(listOf("ACC-003"), state.accountsFor(ExchangeAccountRole.SOURCE).map { it.id })

        act(viewModel, ExchangeAction.AccountSelected("ACC-001"))
        assertNull(viewModel.uiState.value.accountSheetFor)
        assertEquals("ACC-001", viewModel.uiState.value.destinationAccountId)

        act(viewModel, ExchangeAction.AccountClicked(ExchangeAccountRole.SOURCE))
        act(viewModel, ExchangeAction.SheetDismissed)
        assertNull(viewModel.uiState.value.accountSheetFor)
        assertEquals("ACC-003", viewModel.uiState.value.sourceAccountId)

        // Sin un sheet abierto, una selección no cambia nada.
        act(viewModel, ExchangeAction.AccountSelected("ACC-002"))
        assertEquals("ACC-001", viewModel.uiState.value.destinationAccountId)
    }

    @Test
    fun `the how we calculate sheet opens and closes`() = runTest {
        val viewModel = loaded()

        act(viewModel, ExchangeAction.HowWeCalculateClicked)
        assertTrue(viewModel.uiState.value.isHowWeCalculateVisible)
        act(viewModel, ExchangeAction.SheetDismissed)
        assertFalse(viewModel.uiState.value.isHowWeCalculateVisible)
    }

    @Test
    fun `continue with errors shows them under each field without asking for a quote`() = runTest {
        val viewModel = loaded()
        act(viewModel, ExchangeAction.AmountChanged("2000"))

        act(viewModel, ExchangeAction.ContinueClicked)
        advanceUntilIdle()

        assertEquals(
            mapOf(
                ExchangeField.DESTINATION to ExchangeFieldError.REQUIRED,
                ExchangeField.AMOUNT to ExchangeFieldError.EXCEEDS_BALANCE
            ),
            viewModel.uiState.value.fieldErrors
        )
        assertEquals(1, market.fxCalls)
        assertNull(viewModel.uiState.value.order)

        // Corregir el monto o el origen borra su error.
        act(viewModel, ExchangeAction.AmountChanged("100"))
        assertEquals(setOf(ExchangeField.DESTINATION), viewModel.uiState.value.fieldErrors.keys)
        pickDestination(viewModel, "ACC-002")
        assertTrue(viewModel.uiState.value.fieldErrors.isEmpty())
    }

    @Test
    fun `continue asks for a fresh quote and opens the confirmation with it`() = runTest {
        val viewModel = loaded()
        fillReference(viewModel)
        val fresh = TestFxQuote.copy(quoteId = "QTE-000002", ts = TestFxQuote.ts.plusSeconds(5))
        market.fxQuote = fresh
        val gate = CompletableDeferred<Unit>().also { market.fxGate = it }

        act(viewModel, ExchangeAction.ContinueClicked)
        assertTrue(viewModel.uiState.value.isPreparing)
        // Un segundo toque mientras pide la cotización no hace nada.
        act(viewModel, ExchangeAction.ContinueClicked)
        gate.complete(Unit)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isPreparing)
        assertEquals(2, market.fxCalls)
        assertEquals(fresh, state.quote)
        val order = requireNotNull(state.order)
        assertEquals("QTE-000002", order.quoteId)
        assertEquals(pen("374.20"), order.credit)
        assertEquals("key-1", order.idempotencyKey)

        act(viewModel, ExchangeAction.OrderOpened)
        assertNull(viewModel.uiState.value.order)
        // Lo escrito se conserva para volver de la confirmación.
        assertEquals("100", viewModel.uiState.value.amount)
    }

    @Test
    fun `a draft that the fresh quote invalidates shows the error instead of opening`() = runTest {
        val viewModel = loaded()
        act(viewModel, ExchangeAction.DirectionToggled)
        act(viewModel, ExchangeAction.AccountClicked(ExchangeAccountRole.SOURCE))
        act(viewModel, ExchangeAction.AccountSelected("ACC-002"))
        act(viewModel, ExchangeAction.AmountChanged("0.02"))
        market.fxQuote = TestFxQuote.copy(sell = BigDecimal("4.100"), ts = TestFxQuote.ts.plusSeconds(5))

        act(viewModel, ExchangeAction.ContinueClicked)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.order)
        assertEquals(ExchangeFieldError.TOO_SMALL, viewModel.uiState.value.fieldErrors[ExchangeField.AMOUNT])
    }

    @Test
    fun `a failed quote shows the error modal and retry continues`() = runTest {
        val viewModel = loaded()
        fillReference(viewModel)
        market.fxQuotes += Outcome.Failure(AppError.Network)

        act(viewModel, ExchangeAction.ContinueClicked)
        advanceUntilIdle()
        assertEquals(AppError.Network, viewModel.uiState.value.dialogError)
        assertNull(viewModel.uiState.value.order)

        act(viewModel, ExchangeAction.ContinueClicked)
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.dialogError)
        assertEquals("key-1", viewModel.uiState.value.order?.idempotencyKey)
    }

    @Test
    fun `cancel in the error modal closes it and an expired session leaves the global modal alone`() = runTest {
        val viewModel = loaded()
        fillReference(viewModel)
        market.fxQuotes += Outcome.Failure(AppError.Server)
        act(viewModel, ExchangeAction.ContinueClicked)
        advanceUntilIdle()

        act(viewModel, ExchangeAction.ErrorDismissed)
        assertNull(viewModel.uiState.value.dialogError)

        market.fxQuotes += Outcome.Failure(AppError.SessionExpired)
        act(viewModel, ExchangeAction.ContinueClicked)
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.dialogError)
        assertFalse(viewModel.uiState.value.isPreparing)
        assertNull(viewModel.uiState.value.order)
    }

    @Test
    fun `failure shows the error and retry loads again`() = runTest {
        market.error = AppError.Network
        val viewModel = viewModel()
        show(viewModel)
        advanceUntilIdle()
        assertEquals(AppError.Network, viewModel.uiState.value.error)
        assertNull(viewModel.uiState.value.contentKey)
        // Sin cotización todavía, Continuar no hace nada.
        act(viewModel, ExchangeAction.ContinueClicked)
        assertTrue(viewModel.uiState.value.fieldErrors.isEmpty())

        market.error = null
        act(viewModel, ExchangeAction.RetryClicked)
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.error)
        assertEquals(1, viewModel.uiState.value.contentKey)
    }

    @Test
    fun `shown again asks for the accounts only when an operation changed them`() = runTest {
        val viewModel = loaded()
        fillReference(viewModel)
        act(viewModel, ExchangeAction.Shown)
        advanceUntilIdle()
        assertEquals(1, accounts.accountsCalls)

        changes.notifyChanged()
        accounts.accounts = previewAccounts.map {
            if (it.id == "ACC-003") it.copy(availableBalance = usd("950.20")) else it
        }
        act(viewModel, ExchangeAction.Shown)
        act(viewModel, ExchangeAction.Shown)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, accounts.accountsCalls)
        assertEquals(usd("950.20"), state.sourceAccount?.availableBalance)
        assertEquals(2, state.contentKey)
        assertEquals("100", state.amount)
        assertEquals("ACC-002", state.destinationAccountId)
        // Las cotizaciones siguen llegando por el canal: volver a mostrarse no pide otra.
        assertEquals(1, market.fxCalls)
    }

    @Test
    fun `a failed refresh of the accounts shows the error`() = runTest {
        val viewModel = loaded()
        changes.notifyChanged()
        accounts.error = AppError.Network

        act(viewModel, ExchangeAction.Shown)
        advanceUntilIdle()

        assertEquals(AppError.Network, viewModel.uiState.value.error)
        // Con el error a la vista, volver a mostrarse no insiste: lo hace Reintentar.
        changes.notifyChanged()
        act(viewModel, ExchangeAction.Shown)
        advanceUntilIdle()
        assertEquals(2, accounts.accountsCalls)
    }

    private fun TestScope.loaded(): ExchangeViewModel {
        val viewModel = viewModel()
        show(viewModel)
        advanceUntilIdle()
        return viewModel
    }

    private fun TestScope.pickDestination(viewModel: ExchangeViewModel, accountId: String) {
        act(viewModel, ExchangeAction.AccountClicked(ExchangeAccountRole.DESTINATION))
        act(viewModel, ExchangeAction.AccountSelected(accountId))
    }

    private fun TestScope.fillReference(viewModel: ExchangeViewModel) {
        act(viewModel, ExchangeAction.AmountChanged("100"))
        pickDestination(viewModel, "ACC-002")
    }
}
