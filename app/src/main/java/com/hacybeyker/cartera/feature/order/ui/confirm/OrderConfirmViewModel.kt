package com.hacybeyker.cartera.feature.order.ui.confirm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.feature.order.domain.model.MarketOrder
import com.hacybeyker.cartera.feature.order.domain.model.orderFieldErrors
import com.hacybeyker.cartera.feature.order.domain.usecase.ConfirmOrderUseCase
import com.hacybeyker.cartera.feature.order.ui.isOrderRejection
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = OrderConfirmViewModel.Factory::class)
class OrderConfirmViewModel @AssistedInject constructor(
    @Assisted order: MarketOrder,
    private val confirmOrder: ConfirmOrderUseCase
) : ViewModel() {

    private val state = MutableStateFlow(OrderConfirmUiState(order = order))
    val uiState: StateFlow<OrderConfirmUiState> = state.asStateFlow()

    fun onAction(action: OrderConfirmAction) {
        when (action) {
            OrderConfirmAction.ConfirmClicked, OrderConfirmAction.RetryClicked -> submit()
            OrderConfirmAction.ErrorDismissed ->
                state.update { it.copy(dialogError = null, exit = OrderConfirmExit.Failed) }
            OrderConfirmAction.RejectionAcknowledged ->
                state.update { it.copy(rejection = null, exit = OrderConfirmExit.BackToForm) }
            OrderConfirmAction.CancelClicked -> state.update { it.copy(isCancelConfirmationVisible = true) }
            OrderConfirmAction.CancelConfirmed -> state.update {
                it.copy(isCancelConfirmationVisible = false, exit = OrderConfirmExit.Cancelled)
            }
            OrderConfirmAction.CancelDismissed -> state.update { it.copy(isCancelConfirmationVisible = false) }
        }
    }

    // La orden lleva su Idempotency-Key: Reintentar la reenvía igual y el servidor no la ejecuta dos veces.
    private fun submit() {
        if (state.value.isSubmitting) return
        state.update { it.copy(isSubmitting = true, dialogError = null, fieldErrors = emptyMap()) }
        viewModelScope.launch {
            val outcome = confirmOrder(state.value.order)
            state.update { current ->
                when (outcome) {
                    is Outcome.Success ->
                        current.copy(isSubmitting = false, exit = OrderConfirmExit.Done(outcome.value))
                    is Outcome.Failure -> current.withFailure(outcome.error)
                }
            }
        }
    }

    private fun OrderConfirmUiState.withFailure(error: AppError): OrderConfirmUiState = when {
        error is AppError.Validation -> copy(isSubmitting = false, fieldErrors = orderFieldErrors(error.fieldCodes))
        error.isOrderRejection -> copy(isSubmitting = false, rejection = error)
        // El modal global de sesión expirada ya se ocupa; otro modal encima solo estorbaría.
        error == AppError.SessionExpired -> copy(isSubmitting = false)
        else -> copy(isSubmitting = false, dialogError = error)
    }

    @AssistedFactory
    interface Factory {
        fun create(order: MarketOrder): OrderConfirmViewModel
    }
}
