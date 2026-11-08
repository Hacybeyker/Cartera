package com.hacybeyker.cartera.feature.order.ui

import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.testing.FakeAccountChanges
import com.hacybeyker.cartera.core.testing.FakePortfolioChanges
import com.hacybeyker.cartera.core.testing.MainDispatcherRule
import com.hacybeyker.cartera.feature.order.FakeOrderRepository
import com.hacybeyker.cartera.feature.order.ReferenceBuy
import com.hacybeyker.cartera.feature.order.ReferenceBuyReceipt
import com.hacybeyker.cartera.feature.order.domain.model.OrderField
import com.hacybeyker.cartera.feature.order.domain.model.OrderFieldError
import com.hacybeyker.cartera.feature.order.domain.usecase.ConfirmOrderUseCase
import com.hacybeyker.cartera.feature.order.ui.confirm.OrderConfirmAction
import com.hacybeyker.cartera.feature.order.ui.confirm.OrderConfirmExit
import com.hacybeyker.cartera.feature.order.ui.confirm.OrderConfirmViewModel
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class OrderConfirmViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val orders = FakeOrderRepository()
    private val accountChanges = FakeAccountChanges()
    private val portfolioChanges = FakePortfolioChanges()

    private fun viewModel() =
        OrderConfirmViewModel(ReferenceBuy, ConfirmOrderUseCase(orders, accountChanges, portfolioChanges))

    @Test
    fun `confirm sends the order with progress and ends with the receipt`() = runTest {
        val viewModel = viewModel()

        viewModel.onAction(OrderConfirmAction.ConfirmClicked)
        assertTrue(viewModel.uiState.value.isSubmitting)
        // Un segundo toque mientras envía no repite la orden.
        viewModel.onAction(OrderConfirmAction.ConfirmClicked)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSubmitting)
        assertEquals(OrderConfirmExit.Done(ReferenceBuyReceipt), viewModel.uiState.value.exit)
        assertEquals(1, orders.sent.size)
        assertEquals(1, accountChanges.revision)
        assertEquals(1, portfolioChanges.revision)
    }

    @Test
    fun `retry after a network error reuses the same idempotency key`() = runTest {
        orders.failures += AppError.Network
        val viewModel = viewModel()

        viewModel.onAction(OrderConfirmAction.ConfirmClicked)
        advanceUntilIdle()
        assertEquals(AppError.Network, viewModel.uiState.value.dialogError)

        viewModel.onAction(OrderConfirmAction.RetryClicked)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.dialogError)
        assertEquals(OrderConfirmExit.Done(ReferenceBuyReceipt), viewModel.uiState.value.exit)
        assertEquals(listOf("key-1", "key-1"), orders.sent.map { it.idempotencyKey })
    }

    @Test
    fun `cancel in the error modal ends without a receipt, with the revisions already up`() = runTest {
        orders.failures += AppError.Server
        val viewModel = viewModel()
        viewModel.onAction(OrderConfirmAction.ConfirmClicked)
        advanceUntilIdle()

        viewModel.onAction(OrderConfirmAction.ErrorDismissed)

        assertNull(viewModel.uiState.value.dialogError)
        assertEquals(OrderConfirmExit.Failed, viewModel.uiState.value.exit)
        assertEquals(1, accountChanges.revision)
        assertEquals(1, portfolioChanges.revision)
    }

    @Test
    fun `each business rejection opens its modal and goes back to the form`() = runTest {
        listOf(AppError.InsufficientFunds, AppError.InsufficientHoldings).forEach { error ->
            orders.failures += error
            val viewModel = viewModel()
            viewModel.onAction(OrderConfirmAction.ConfirmClicked)
            advanceUntilIdle()
            assertEquals(error, viewModel.uiState.value.rejection)
            assertNull(viewModel.uiState.value.dialogError)

            viewModel.onAction(OrderConfirmAction.RejectionAcknowledged)

            assertNull(viewModel.uiState.value.rejection)
            assertEquals(OrderConfirmExit.BackToForm, viewModel.uiState.value.exit)
        }
        assertEquals(0, portfolioChanges.revision)
    }

    @Test
    fun `a validation error shows under each field of the summary and confirming again clears it`() = runTest {
        orders.failures += AppError.Validation(mapOf("accountId" to "UNSUPPORTED_CURRENCY", "quantity" to "X"))
        val viewModel = viewModel()
        viewModel.onAction(OrderConfirmAction.ConfirmClicked)
        advanceUntilIdle()

        assertEquals(
            mapOf(
                OrderField.ACCOUNT to OrderFieldError.UNSUPPORTED_CURRENCY,
                OrderField.QUANTITY to OrderFieldError.INVALID_FORMAT
            ),
            viewModel.uiState.value.fieldErrors
        )
        assertNull(viewModel.uiState.value.exit)

        viewModel.onAction(OrderConfirmAction.ConfirmClicked)
        assertTrue(viewModel.uiState.value.fieldErrors.isEmpty())
    }

    @Test
    fun `an expired session leaves the global modal alone`() = runTest {
        orders.failures += AppError.SessionExpired
        val viewModel = viewModel()
        viewModel.onAction(OrderConfirmAction.ConfirmClicked)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isSubmitting)
        assertNull(state.dialogError)
        assertNull(state.rejection)
        assertNull(state.exit)
    }

    @Test
    fun `cancel asks first and then leaves without sending`() = runTest {
        val viewModel = viewModel()

        viewModel.onAction(OrderConfirmAction.CancelClicked)
        assertTrue(viewModel.uiState.value.isCancelConfirmationVisible)
        viewModel.onAction(OrderConfirmAction.CancelDismissed)
        assertFalse(viewModel.uiState.value.isCancelConfirmationVisible)
        assertNull(viewModel.uiState.value.exit)

        viewModel.onAction(OrderConfirmAction.CancelClicked)
        viewModel.onAction(OrderConfirmAction.CancelConfirmed)

        assertEquals(OrderConfirmExit.Cancelled, viewModel.uiState.value.exit)
        assertTrue(orders.sent.isEmpty())
    }
}
