package com.hacybeyker.cartera.feature.order.ui.confirm

import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.feature.order.domain.model.MarketOrder
import com.hacybeyker.cartera.feature.order.domain.model.OrderField
import com.hacybeyker.cartera.feature.order.domain.model.OrderFieldError
import com.hacybeyker.cartera.feature.order.domain.model.OrderReceipt

data class OrderConfirmUiState(
    val order: MarketOrder,
    val isSubmitting: Boolean = false,
    /** Red o servidor: modal con Reintentar (misma `Idempotency-Key`) y Cancelar. */
    val dialogError: AppError? = null,
    /** Fondos o tenencia insuficientes: un modal propio que vuelve al formulario. */
    val rejection: AppError? = null,
    /** `VALIDATION_ERROR`: el mensaje de cada campo bajo su fila del resumen. */
    val fieldErrors: Map<OrderField, OrderFieldError> = emptyMap(),
    val isCancelConfirmationVisible: Boolean = false,
    val exit: OrderConfirmExit? = null
)

sealed interface OrderConfirmExit {
    data class Done(val receipt: OrderReceipt) : OrderConfirmExit

    /** Canceló tras un error de red o del servidor: el resultado avisa que no se pudo confirmar. */
    data object Failed : OrderConfirmExit

    data object BackToForm : OrderConfirmExit

    data object Cancelled : OrderConfirmExit
}

sealed interface OrderConfirmAction {
    data object ConfirmClicked : OrderConfirmAction

    data object RetryClicked : OrderConfirmAction

    data object ErrorDismissed : OrderConfirmAction

    data object RejectionAcknowledged : OrderConfirmAction

    data object CancelClicked : OrderConfirmAction

    data object CancelConfirmed : OrderConfirmAction

    data object CancelDismissed : OrderConfirmAction
}
