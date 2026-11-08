package com.hacybeyker.cartera.feature.transfer.ui

import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.domain.account.GetAccountsUseCase
import com.hacybeyker.cartera.core.testing.FakeAccountRepository
import com.hacybeyker.cartera.core.testing.MainDispatcherRule
import com.hacybeyker.cartera.core.testing.TestAccounts
import com.hacybeyker.cartera.feature.transfer.SequentialKeys
import com.hacybeyker.cartera.feature.transfer.domain.model.TransferField
import com.hacybeyker.cartera.feature.transfer.domain.model.TransferFieldError
import com.hacybeyker.cartera.feature.transfer.domain.usecase.ValidateTransferUseCase
import com.hacybeyker.cartera.feature.transfer.ui.form.TransferFormAction
import com.hacybeyker.cartera.feature.transfer.ui.form.TransferFormViewModel
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TransferFormViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val accounts = FakeAccountRepository()

    private fun viewModel() =
        TransferFormViewModel(GetAccountsUseCase(accounts), ValidateTransferUseCase(SequentialKeys()))

    private fun TransferFormViewModel.fillReference() {
        onAction(TransferFormAction.SourceSelected("ACC-002"))
        onAction(TransferFormAction.DestinationChanged("999-0000001-0-01"))
        onAction(TransferFormAction.AmountChanged("150"))
    }

    @Test
    fun `loads the own accounts to choose the source`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        assertEquals(TestAccounts, viewModel.uiState.value.accounts)
        assertEquals(1, viewModel.uiState.value.contentKey)
        assertEquals(1, accounts.accountsCalls)
    }

    @Test
    fun `continue with valid data creates the order to confirm`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.fillReference()
        assertEquals("PEN", viewModel.uiState.value.currency)

        viewModel.onAction(TransferFormAction.ContinueClicked)

        val order = viewModel.uiState.value.order
        assertEquals("ACC-002", order?.sourceAccountId)
        assertEquals("150.00", order?.amount?.amount?.toPlainString())
        assertEquals("key-1", order?.idempotencyKey)

        viewModel.onAction(TransferFormAction.OrderOpened)
        assertNull(viewModel.uiState.value.order)
        // Los datos quedan: Atrás en Confirmación vuelve al formulario con ellos.
        assertEquals("150", viewModel.uiState.value.amount)
    }

    @Test
    fun `continue with invalid data shows the error of each field`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.onAction(TransferFormAction.SourceSelected("ACC-002"))
        viewModel.onAction(TransferFormAction.DestinationChanged("123"))
        viewModel.onAction(TransferFormAction.AmountChanged("5000"))

        viewModel.onAction(TransferFormAction.ContinueClicked)

        assertEquals(
            mapOf(
                TransferField.DESTINATION to TransferFieldError.INVALID_FORMAT,
                TransferField.AMOUNT to TransferFieldError.EXCEEDS_BALANCE
            ),
            viewModel.uiState.value.fieldErrors
        )
        assertNull(viewModel.uiState.value.order)
    }

    @Test
    fun `editing a field clears only its error`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.onAction(TransferFormAction.ContinueClicked)
        assertEquals(3, viewModel.uiState.value.fieldErrors.size)

        viewModel.onAction(TransferFormAction.AmountChanged("10"))
        viewModel.onAction(TransferFormAction.DescriptionChanged("Pago"))

        assertEquals(setOf(TransferField.SOURCE, TransferField.DESTINATION), viewModel.uiState.value.fieldErrors.keys)
    }

    @Test
    fun `back without data leaves at once`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onAction(TransferFormAction.BackClicked)

        assertTrue(viewModel.uiState.value.isExited)
        assertFalse(viewModel.uiState.value.isExitConfirmationVisible)
    }

    @Test
    fun `back with data asks before leaving`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.onAction(TransferFormAction.DescriptionChanged("Pago"))

        viewModel.onAction(TransferFormAction.BackClicked)
        assertTrue(viewModel.uiState.value.isExitConfirmationVisible)
        viewModel.onAction(TransferFormAction.ExitCancelled)
        assertFalse(viewModel.uiState.value.isExitConfirmationVisible)
        assertFalse(viewModel.uiState.value.isExited)

        viewModel.onAction(TransferFormAction.BackClicked)
        viewModel.onAction(TransferFormAction.ExitConfirmed)
        assertTrue(viewModel.uiState.value.isExited)
    }

    @Test
    fun `failure loading the accounts shows the error and retry loads again`() = runTest {
        accounts.error = AppError.Network
        val viewModel = viewModel()
        advanceUntilIdle()
        assertEquals(AppError.Network, viewModel.uiState.value.error)

        accounts.error = null
        viewModel.onAction(TransferFormAction.RetryClicked)
        advanceUntilIdle()

        assertEquals(TestAccounts, viewModel.uiState.value.accounts)
    }
}
