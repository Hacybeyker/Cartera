package com.hacybeyker.cartera.feature.transfer.ui

import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.testing.FakeAccountChanges
import com.hacybeyker.cartera.core.testing.MainDispatcherRule
import com.hacybeyker.cartera.feature.transfer.FakeTransferRepository
import com.hacybeyker.cartera.feature.transfer.ReferenceOrder
import com.hacybeyker.cartera.feature.transfer.ReferenceReceipt
import com.hacybeyker.cartera.feature.transfer.domain.model.TransferField
import com.hacybeyker.cartera.feature.transfer.domain.model.TransferFieldError
import com.hacybeyker.cartera.feature.transfer.domain.usecase.ConfirmTransferUseCase
import com.hacybeyker.cartera.feature.transfer.ui.confirm.TransferConfirmAction
import com.hacybeyker.cartera.feature.transfer.ui.confirm.TransferConfirmExit
import com.hacybeyker.cartera.feature.transfer.ui.confirm.TransferConfirmViewModel
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TransferConfirmViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val transfers = FakeTransferRepository()
    private val changes = FakeAccountChanges()

    private fun viewModel() = TransferConfirmViewModel(ReferenceOrder, ConfirmTransferUseCase(transfers, changes))

    @Test
    fun `confirm sends the order with progress and ends with the receipt`() = runTest {
        val viewModel = viewModel()

        viewModel.onAction(TransferConfirmAction.ConfirmClicked)
        assertTrue(viewModel.uiState.value.isSubmitting)
        // Un segundo toque mientras envía no repite la operación.
        viewModel.onAction(TransferConfirmAction.ConfirmClicked)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSubmitting)
        assertEquals(TransferConfirmExit.Done(ReferenceReceipt), viewModel.uiState.value.exit)
        assertEquals(1, transfers.sent.size)
    }

    @Test
    fun `retry after a network error reuses the same idempotency key`() = runTest {
        transfers.failures += AppError.Network
        val viewModel = viewModel()

        viewModel.onAction(TransferConfirmAction.ConfirmClicked)
        advanceUntilIdle()
        assertEquals(AppError.Network, viewModel.uiState.value.dialogError)

        viewModel.onAction(TransferConfirmAction.RetryClicked)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.dialogError)
        assertEquals(TransferConfirmExit.Done(ReferenceReceipt), viewModel.uiState.value.exit)
        assertEquals(listOf("key-1", "key-1"), transfers.sent.map { it.idempotencyKey })
    }

    @Test
    fun `cancel in the error modal ends without a receipt`() = runTest {
        transfers.failures += AppError.Server
        val viewModel = viewModel()
        viewModel.onAction(TransferConfirmAction.ConfirmClicked)
        advanceUntilIdle()

        viewModel.onAction(TransferConfirmAction.ErrorDismissed)

        assertNull(viewModel.uiState.value.dialogError)
        assertEquals(TransferConfirmExit.Failed, viewModel.uiState.value.exit)
    }

    @Test
    fun `each business rejection opens its modal and goes back to the form`() = runTest {
        listOf(AppError.InsufficientFunds, AppError.DestinationNotFound, AppError.CurrencyMismatch).forEach { error ->
            transfers.failures += error
            val viewModel = viewModel()
            viewModel.onAction(TransferConfirmAction.ConfirmClicked)
            advanceUntilIdle()
            assertEquals(error, viewModel.uiState.value.rejection)
            assertNull(viewModel.uiState.value.dialogError)

            viewModel.onAction(TransferConfirmAction.RejectionAcknowledged)

            assertNull(viewModel.uiState.value.rejection)
            assertEquals(TransferConfirmExit.BackToForm, viewModel.uiState.value.exit)
        }
    }

    @Test
    fun `a validation error shows the message of each field`() = runTest {
        transfers.failures += AppError.Validation(mapOf("amount.value" to "MUST_BE_POSITIVE"))
        val viewModel = viewModel()

        viewModel.onAction(TransferConfirmAction.ConfirmClicked)
        advanceUntilIdle()

        assertEquals(
            mapOf(TransferField.AMOUNT to TransferFieldError.MUST_BE_POSITIVE),
            viewModel.uiState.value.fieldErrors
        )
        assertNull(viewModel.uiState.value.exit)
    }

    @Test
    fun `an expired session leaves the global modal alone`() = runTest {
        transfers.failures += AppError.SessionExpired
        val viewModel = viewModel()

        viewModel.onAction(TransferConfirmAction.ConfirmClicked)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.dialogError)
        assertFalse(viewModel.uiState.value.isSubmitting)
    }

    @Test
    fun `cancel asks for confirmation`() = runTest {
        val viewModel = viewModel()

        viewModel.onAction(TransferConfirmAction.CancelClicked)
        assertTrue(viewModel.uiState.value.isCancelConfirmationVisible)
        viewModel.onAction(TransferConfirmAction.CancelDismissed)
        assertFalse(viewModel.uiState.value.isCancelConfirmationVisible)
        assertNull(viewModel.uiState.value.exit)

        viewModel.onAction(TransferConfirmAction.CancelClicked)
        viewModel.onAction(TransferConfirmAction.CancelConfirmed)
        assertEquals(TransferConfirmExit.Cancelled, viewModel.uiState.value.exit)
        assertTrue(transfers.sent.isEmpty())
    }
}
