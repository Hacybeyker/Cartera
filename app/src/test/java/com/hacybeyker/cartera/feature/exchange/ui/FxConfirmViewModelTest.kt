package com.hacybeyker.cartera.feature.exchange.ui

import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.core.domain.exchange.ExchangeField
import com.hacybeyker.cartera.core.domain.exchange.ExchangeFieldError
import com.hacybeyker.cartera.core.domain.market.GetFxRatesUseCase
import com.hacybeyker.cartera.core.testing.FakeAccountChanges
import com.hacybeyker.cartera.core.testing.FakeMarketRepository
import com.hacybeyker.cartera.core.testing.MainDispatcherRule
import com.hacybeyker.cartera.core.testing.SequentialKeys
import com.hacybeyker.cartera.core.testing.TestClock
import com.hacybeyker.cartera.core.testing.TestFxQuote
import com.hacybeyker.cartera.feature.exchange.FakeExchangeRepository
import com.hacybeyker.cartera.feature.exchange.ReferenceExchange
import com.hacybeyker.cartera.feature.exchange.ReferenceExchangeReceipt
import com.hacybeyker.cartera.feature.exchange.domain.usecase.ConfirmExchangeUseCase
import com.hacybeyker.cartera.feature.exchange.pen
import com.hacybeyker.cartera.feature.exchange.ui.confirm.FxConfirmAction
import com.hacybeyker.cartera.feature.exchange.ui.confirm.FxConfirmExit
import com.hacybeyker.cartera.feature.exchange.ui.confirm.FxConfirmFailure
import com.hacybeyker.cartera.feature.exchange.ui.confirm.FxConfirmStep
import com.hacybeyker.cartera.feature.exchange.ui.confirm.FxConfirmViewModel
import java.math.BigDecimal
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class FxConfirmViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val exchanges = FakeExchangeRepository()
    private val accountChanges = FakeAccountChanges()
    private val market = FakeMarketRepository()

    /** La cotización que se ofrece cuando vence la primera: 30 s después, con la compra un poco más alta. */
    private val fresh = TestFxQuote.copy(
        quoteId = "QTE-000002",
        buy = BigDecimal("3.745"),
        ts = TestFxQuote.ts.plusSeconds(30),
        expiresAt = TestFxQuote.ts.plusSeconds(60)
    )

    /** El reloj empieza a las 10:00:00, al emitirse la cotización de [ReferenceExchange] (vence a las 10:00:30). */
    private fun TestScope.viewModel() = FxConfirmViewModel(
        order = ReferenceExchange,
        confirmExchange = ConfirmExchangeUseCase(exchanges, accountChanges),
        getFxRates = GetFxRatesUseCase(market),
        keys = SequentialKeys(),
        clock = TestClock(testScheduler, TestFxQuote.ts.toInstant())
    )

    /** Envía [action] y deja correr lo que dispara, como la pantalla entre un frame y otro. */
    private fun TestScope.act(viewModel: FxConfirmViewModel, action: FxConfirmAction) {
        viewModel.onAction(action)
        runCurrent()
    }

    /** Recolecta como la pantalla en STARTED: la cuenta regresiva corre solo así. */
    private fun TestScope.show(viewModel: FxConfirmViewModel): Job =
        backgroundScope.launch { viewModel.uiState.collect {} }.also { runCurrent() }

    @Test
    fun `confirm sends the exchange with progress and ends with the receipt`() = runTest {
        val viewModel = viewModel()
        show(viewModel)
        assertEquals(30, viewModel.uiState.value.remainingSeconds)
        val gate = CompletableDeferred<Unit>().also { exchanges.gate = it }

        act(viewModel, FxConfirmAction.ConfirmClicked)
        assertTrue(viewModel.uiState.value.isSubmitting)
        assertTrue(viewModel.uiState.value.isBusy)
        // Un segundo toque mientras envía no repite el cambio.
        act(viewModel, FxConfirmAction.ConfirmClicked)
        gate.complete(Unit)
        runCurrent()

        assertFalse(viewModel.uiState.value.isSubmitting)
        assertEquals(FxConfirmExit.Done(ReferenceExchangeReceipt), viewModel.uiState.value.exit)
        assertEquals(listOf("key-0"), exchanges.sent.map { it.idempotencyKey })
        assertEquals(1, accountChanges.revision)
    }

    @Test
    fun `the countdown goes down once per second`() = runTest {
        val viewModel = viewModel()
        show(viewModel)

        advanceTimeBy(1_000)
        runCurrent()
        assertEquals(29, viewModel.uiState.value.remainingSeconds)

        advanceTimeBy(28_500)
        runCurrent()
        assertEquals(1, viewModel.uiState.value.remainingSeconds)
        assertFalse(viewModel.uiState.value.isQuoteExpired)
        assertEquals(0, market.fxCalls)
    }

    @Test
    fun `when the clock reaches the expiry it asks for a new quote and offers it`() = runTest {
        market.fxQuote = fresh
        val viewModel = viewModel()
        show(viewModel)

        advanceTimeBy(30_000)
        runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state.isQuoteExpired)
        assertEquals(1, market.fxCalls)
        assertEquals(fresh, state.newQuote)
        assertEquals(pen("374.50"), state.newCredit)
        assertTrue(exchanges.sent.isEmpty())

        // Con el modal abierto no se vuelve a pedir otra.
        advanceTimeBy(10_000)
        runCurrent()
        assertEquals(1, market.fxCalls)
    }

    @Test
    fun `accepting the new quote updates the summary, restarts the countdown and confirms with a new key`() = runTest {
        market.fxQuote = fresh
        val viewModel = viewModel()
        show(viewModel)
        advanceTimeBy(30_000)
        runCurrent()

        act(viewModel, FxConfirmAction.NewQuoteAccepted)
        runCurrent()

        val order = viewModel.uiState.value.order
        assertNull(viewModel.uiState.value.newQuote)
        assertEquals("QTE-000002", order.quoteId)
        assertEquals(BigDecimal("3.745"), order.rate)
        assertEquals(pen("374.50"), order.credit)
        assertEquals("key-1", order.idempotencyKey)
        assertEquals(30, viewModel.uiState.value.remainingSeconds)

        act(viewModel, FxConfirmAction.ConfirmClicked)
        runCurrent()
        assertEquals(listOf("QTE-000002" to "key-1"), exchanges.sent.map { it.quoteId to it.idempotencyKey })
        assertTrue(viewModel.uiState.value.exit is FxConfirmExit.Done)
    }

    @Test
    fun `the new quote can expire again and is renewed once more`() = runTest {
        market.fxQuote = fresh
        val viewModel = viewModel()
        show(viewModel)
        advanceTimeBy(30_000)
        runCurrent()
        act(viewModel, FxConfirmAction.NewQuoteAccepted)
        market.fxQuote = fresh.copy(quoteId = "QTE-000003", expiresAt = fresh.expiresAt.plusSeconds(30))

        advanceTimeBy(30_000)
        runCurrent()

        assertEquals(2, market.fxCalls)
        assertEquals("QTE-000003", viewModel.uiState.value.newQuote?.quoteId)
    }

    @Test
    fun `declining the new quote goes back to the exchange tab`() = runTest {
        market.fxQuote = fresh
        val viewModel = viewModel()
        show(viewModel)
        advanceTimeBy(30_000)
        runCurrent()

        act(viewModel, FxConfirmAction.NewQuoteDeclined)

        assertNull(viewModel.uiState.value.newQuote)
        assertEquals(FxConfirmExit.BackToForm, viewModel.uiState.value.exit)
        assertTrue(exchanges.sent.isEmpty())
    }

    @Test
    fun `a quote expired answer offers the quote of the problem without asking for another`() = runTest {
        exchanges.failures += AppError.QuoteExpired(fresh)
        val viewModel = viewModel()
        show(viewModel)

        act(viewModel, FxConfirmAction.ConfirmClicked)
        runCurrent()

        assertFalse(viewModel.uiState.value.isSubmitting)
        assertEquals(fresh, viewModel.uiState.value.newQuote)
        assertEquals(0, market.fxCalls)
        assertEquals(0, accountChanges.revision)

        act(viewModel, FxConfirmAction.NewQuoteAccepted)
        act(viewModel, FxConfirmAction.ConfirmClicked)
        runCurrent()

        // Otro cuerpo, otra clave: la primera nunca hizo el cambio.
        assertEquals(
            listOf("QTE-000001" to "key-0", "QTE-000002" to "key-1"),
            exchanges.sent.map { it.quoteId to it.idempotencyKey }
        )
        assertTrue(viewModel.uiState.value.exit is FxConfirmExit.Done)
    }

    @Test
    fun `a quote expired answer without a readable quote asks for a new one`() = runTest {
        exchanges.failures += AppError.QuoteExpired(null)
        market.fxQuote = fresh
        val viewModel = viewModel()
        show(viewModel)

        act(viewModel, FxConfirmAction.ConfirmClicked)
        runCurrent()

        assertEquals(1, market.fxCalls)
        assertEquals(fresh, viewModel.uiState.value.newQuote)
        // Cuando el reloj llega al vencimiento de esa cotización no se pide otra encima.
        advanceTimeBy(30_000)
        runCurrent()
        assertEquals(1, market.fxCalls)
    }

    @Test
    fun `retry after a network error reuses the same idempotency key`() = runTest {
        exchanges.failures += AppError.Network
        val viewModel = viewModel()
        show(viewModel)

        act(viewModel, FxConfirmAction.ConfirmClicked)
        runCurrent()
        assertEquals(FxConfirmFailure(AppError.Network, FxConfirmStep.CONFIRM), viewModel.uiState.value.failure)

        act(viewModel, FxConfirmAction.RetryClicked)
        runCurrent()

        assertNull(viewModel.uiState.value.failure)
        assertEquals(FxConfirmExit.Done(ReferenceExchangeReceipt), viewModel.uiState.value.exit)
        assertEquals(listOf("key-0", "key-0"), exchanges.sent.map { it.idempotencyKey })
    }

    @Test
    fun `while the error modal is open the expired quote is not renewed`() = runTest {
        exchanges.failures += AppError.Server
        market.fxQuote = fresh
        val viewModel = viewModel()
        show(viewModel)
        act(viewModel, FxConfirmAction.ConfirmClicked)
        runCurrent()

        advanceTimeBy(40_000)
        runCurrent()

        assertTrue(viewModel.uiState.value.isQuoteExpired)
        assertEquals(0, market.fxCalls)
        assertEquals(FxConfirmStep.CONFIRM, viewModel.uiState.value.failure?.step)
    }

    @Test
    fun `cancel in the error modal ends without a receipt, with the accounts already invalidated`() = runTest {
        exchanges.failures += AppError.Server
        val viewModel = viewModel()
        show(viewModel)
        act(viewModel, FxConfirmAction.ConfirmClicked)
        runCurrent()

        act(viewModel, FxConfirmAction.ErrorDismissed)

        assertNull(viewModel.uiState.value.failure)
        assertEquals(FxConfirmExit.Failed, viewModel.uiState.value.exit)
        assertEquals(1, accountChanges.revision)
    }

    @Test
    fun `a failed renewal shows the error modal, retry renews and cancel goes back to the tab`() = runTest {
        market.fxQuotes += Outcome.Failure(AppError.Network)
        market.fxQuote = fresh
        val viewModel = viewModel()
        show(viewModel)
        advanceTimeBy(30_000)
        runCurrent()
        assertEquals(FxConfirmFailure(AppError.Network, FxConfirmStep.RENEW_QUOTE), viewModel.uiState.value.failure)

        act(viewModel, FxConfirmAction.RetryClicked)
        runCurrent()
        assertNull(viewModel.uiState.value.failure)
        assertEquals(fresh, viewModel.uiState.value.newQuote)

        market.fxQuotes += Outcome.Failure(AppError.Server)
        val other = viewModel()
        show(other)
        advanceTimeBy(30_000)
        runCurrent()
        act(other, FxConfirmAction.ErrorDismissed)
        assertEquals(FxConfirmExit.BackToForm, other.uiState.value.exit)
        assertTrue(exchanges.sent.isEmpty())
    }

    @Test
    fun `an expired session while renewing leaves the global modal alone and does not insist`() = runTest {
        market.fxQuotes += Outcome.Failure(AppError.SessionExpired)
        val viewModel = viewModel()
        show(viewModel)

        advanceTimeBy(60_000)
        runCurrent()

        val state = viewModel.uiState.value
        assertEquals(1, market.fxCalls)
        assertNull(state.failure)
        assertNull(state.newQuote)
        assertFalse(state.isRenewingQuote)
    }

    @Test
    fun `an expiry while asking to cancel waits until the dialog closes`() = runTest {
        market.fxQuote = fresh
        val viewModel = viewModel()
        show(viewModel)
        act(viewModel, FxConfirmAction.CancelClicked)

        advanceTimeBy(31_000)
        runCurrent()
        assertEquals(0, market.fxCalls)

        act(viewModel, FxConfirmAction.CancelDismissed)
        runCurrent()
        assertEquals(1, market.fxCalls)
        assertEquals(fresh, viewModel.uiState.value.newQuote)
    }

    @Test
    fun `each business rejection opens its modal and goes back to the tab`() = runTest {
        listOf(AppError.InsufficientFunds, AppError.CurrencyMismatch).forEach { error ->
            exchanges.failures += error
            val viewModel = viewModel()
            show(viewModel)
            act(viewModel, FxConfirmAction.ConfirmClicked)
            runCurrent()
            assertEquals(error, viewModel.uiState.value.rejection)
            assertNull(viewModel.uiState.value.failure)

            act(viewModel, FxConfirmAction.RejectionAcknowledged)

            assertNull(viewModel.uiState.value.rejection)
            assertEquals(FxConfirmExit.BackToForm, viewModel.uiState.value.exit)
        }
        assertEquals(0, accountChanges.revision)
    }

    @Test
    fun `a validation error shows under each field of the summary and confirming again clears it`() = runTest {
        exchanges.failures += AppError.Validation(mapOf("amount.value" to "TOO_SMALL", "quoteId" to "NOT_FOUND"))
        val viewModel = viewModel()
        show(viewModel)
        act(viewModel, FxConfirmAction.ConfirmClicked)
        runCurrent()

        assertEquals(
            mapOf(
                ExchangeField.AMOUNT to ExchangeFieldError.TOO_SMALL,
                ExchangeField.QUOTE to ExchangeFieldError.NOT_FOUND
            ),
            viewModel.uiState.value.fieldErrors
        )
        assertNull(viewModel.uiState.value.exit)

        act(viewModel, FxConfirmAction.ConfirmClicked)
        assertTrue(viewModel.uiState.value.fieldErrors.isEmpty())
    }

    @Test
    fun `an expired session leaves the global modal alone`() = runTest {
        exchanges.failures += AppError.SessionExpired
        val viewModel = viewModel()
        show(viewModel)
        act(viewModel, FxConfirmAction.ConfirmClicked)
        runCurrent()

        val state = viewModel.uiState.value
        assertFalse(state.isSubmitting)
        assertNull(state.failure)
        assertNull(state.rejection)
        assertNull(state.exit)
    }

    @Test
    fun `cancel asks first and then leaves without sending`() = runTest {
        val viewModel = viewModel()
        show(viewModel)

        act(viewModel, FxConfirmAction.CancelClicked)
        assertTrue(viewModel.uiState.value.isCancelConfirmationVisible)
        act(viewModel, FxConfirmAction.CancelDismissed)
        assertFalse(viewModel.uiState.value.isCancelConfirmationVisible)
        assertNull(viewModel.uiState.value.exit)

        act(viewModel, FxConfirmAction.CancelClicked)
        act(viewModel, FxConfirmAction.CancelConfirmed)

        assertEquals(FxConfirmExit.Cancelled, viewModel.uiState.value.exit)
        assertTrue(exchanges.sent.isEmpty())
    }

    @Test
    fun `an order restored after its expiry asks for a new quote as soon as it is shown`() = runTest {
        market.fxQuote = fresh
        advanceTimeBy(45_000)
        val viewModel = viewModel()
        assertEquals(0, viewModel.uiState.value.remainingSeconds)

        show(viewModel)
        advanceUntilIdle()

        assertEquals(fresh, viewModel.uiState.value.newQuote)
        // Sin un modal de error, Reintentar no hace nada.
        act(viewModel, FxConfirmAction.RetryClicked)
        assertEquals(1, market.fxCalls)
    }
}
