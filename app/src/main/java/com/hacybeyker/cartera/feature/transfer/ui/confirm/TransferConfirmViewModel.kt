package com.hacybeyker.cartera.feature.transfer.ui.confirm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.feature.transfer.domain.model.TransferOrder
import com.hacybeyker.cartera.feature.transfer.domain.model.transferFieldErrors
import com.hacybeyker.cartera.feature.transfer.domain.usecase.ConfirmTransferUseCase
import com.hacybeyker.cartera.feature.transfer.ui.isTransferRejection
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = TransferConfirmViewModel.Factory::class)
class TransferConfirmViewModel @AssistedInject constructor(
    @Assisted order: TransferOrder,
    private val confirmTransfer: ConfirmTransferUseCase
) : ViewModel() {

    private val state = MutableStateFlow(TransferConfirmUiState(order = order))
    val uiState: StateFlow<TransferConfirmUiState> = state.asStateFlow()

    fun onAction(action: TransferConfirmAction) {
        when (action) {
            TransferConfirmAction.ConfirmClicked, TransferConfirmAction.RetryClicked -> submit()
            TransferConfirmAction.ErrorDismissed ->
                state.update { it.copy(dialogError = null, exit = TransferConfirmExit.Failed) }
            TransferConfirmAction.RejectionAcknowledged ->
                state.update { it.copy(rejection = null, exit = TransferConfirmExit.BackToForm) }
            TransferConfirmAction.CancelClicked -> state.update { it.copy(isCancelConfirmationVisible = true) }
            TransferConfirmAction.CancelConfirmed -> state.update {
                it.copy(isCancelConfirmationVisible = false, exit = TransferConfirmExit.Cancelled)
            }
            TransferConfirmAction.CancelDismissed -> state.update { it.copy(isCancelConfirmationVisible = false) }
        }
    }

    // La orden lleva su Idempotency-Key: Reintentar la reenvía igual y el servidor no cobra dos veces.
    private fun submit() {
        if (state.value.isSubmitting) return
        state.update { it.copy(isSubmitting = true, dialogError = null, fieldErrors = emptyMap()) }
        viewModelScope.launch {
            val outcome = confirmTransfer(state.value.order)
            state.update { current ->
                when (outcome) {
                    is Outcome.Success ->
                        current.copy(isSubmitting = false, exit = TransferConfirmExit.Done(outcome.value))
                    is Outcome.Failure -> current.withFailure(outcome.error)
                }
            }
        }
    }

    private fun TransferConfirmUiState.withFailure(error: AppError): TransferConfirmUiState = when {
        error is AppError.Validation -> copy(isSubmitting = false, fieldErrors = transferFieldErrors(error.fieldCodes))
        error.isTransferRejection -> copy(isSubmitting = false, rejection = error)
        // El modal global de sesión expirada ya se ocupa; otro modal encima solo estorbaría.
        error == AppError.SessionExpired -> copy(isSubmitting = false)
        else -> copy(isSubmitting = false, dialogError = error)
    }

    @AssistedFactory
    interface Factory {
        fun create(order: TransferOrder): TransferConfirmViewModel
    }
}
