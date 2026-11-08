package com.hacybeyker.cartera.feature.transfer.ui.form

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.core.domain.account.GetAccountsUseCase
import com.hacybeyker.cartera.feature.transfer.domain.model.TransferDraft
import com.hacybeyker.cartera.feature.transfer.domain.model.TransferField
import com.hacybeyker.cartera.feature.transfer.domain.model.TransferValidation
import com.hacybeyker.cartera.feature.transfer.domain.usecase.ValidateTransferUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class TransferFormViewModel @Inject constructor(
    private val getAccounts: GetAccountsUseCase,
    private val validateTransfer: ValidateTransferUseCase
) : ViewModel() {

    private val state = MutableStateFlow(TransferFormUiState())
    val uiState: StateFlow<TransferFormUiState> = state.asStateFlow()

    init {
        load()
    }

    fun onAction(action: TransferFormAction) {
        when (action) {
            is TransferFormAction.SourceSelected ->
                edit(TransferField.SOURCE) { copy(sourceAccountId = action.accountId) }
            is TransferFormAction.DestinationChanged ->
                edit(TransferField.DESTINATION) { copy(destination = action.value) }
            is TransferFormAction.AmountChanged -> edit(TransferField.AMOUNT) { copy(amount = action.value) }
            is TransferFormAction.DescriptionChanged ->
                edit(TransferField.DESCRIPTION) { copy(description = action.value) }
            TransferFormAction.ContinueClicked -> validate()
            TransferFormAction.OrderOpened -> state.update { it.copy(order = null) }
            TransferFormAction.BackClicked -> state.update {
                if (it.hasData) it.copy(isExitConfirmationVisible = true) else it.copy(isExited = true)
            }
            TransferFormAction.ExitConfirmed ->
                state.update { it.copy(isExitConfirmationVisible = false, isExited = true) }
            TransferFormAction.ExitCancelled -> state.update { it.copy(isExitConfirmationVisible = false) }
            TransferFormAction.RetryClicked -> load()
        }
    }

    // Editar un campo borra su error: el aviso era sobre el valor anterior.
    private fun edit(field: TransferField, change: TransferFormUiState.() -> TransferFormUiState) {
        state.update { it.change().copy(fieldErrors = it.fieldErrors - field) }
    }

    private fun validate() {
        val form = state.value
        val draft = TransferDraft(
            sourceAccountId = form.sourceAccountId,
            destinationAccountNumber = form.destination,
            amount = form.amount,
            description = form.description
        )
        state.update {
            when (val validation = validateTransfer(draft, form.accounts)) {
                is TransferValidation.Valid -> it.copy(fieldErrors = emptyMap(), order = validation.order)
                is TransferValidation.Invalid -> it.copy(fieldErrors = validation.errors)
            }
        }
    }

    private fun load() {
        state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val outcome = getAccounts()
            state.update { current ->
                when (outcome) {
                    is Outcome.Success -> current.copy(
                        isLoading = false,
                        accounts = outcome.value,
                        contentVersion = current.contentVersion + 1
                    )
                    is Outcome.Failure -> current.copy(isLoading = false, error = outcome.error)
                }
            }
        }
    }
}
