package com.hacybeyker.cartera.feature.transfer.ui.confirm

import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.feature.transfer.domain.model.TransferField
import com.hacybeyker.cartera.feature.transfer.domain.model.TransferFieldError
import com.hacybeyker.cartera.feature.transfer.domain.model.TransferOrder
import com.hacybeyker.cartera.feature.transfer.domain.model.TransferReceipt

data class TransferConfirmUiState(
    val order: TransferOrder,
    val isSubmitting: Boolean = false,
    /** Red o servidor: modal con Reintentar (misma `Idempotency-Key`) y Cancelar. */
    val dialogError: AppError? = null,
    /** Fondos insuficientes, destino inexistente o monedas distintas: un modal propio que vuelve al formulario. */
    val rejection: AppError? = null,
    /** `VALIDATION_ERROR`: el mensaje de cada campo bajo su fila del resumen. */
    val fieldErrors: Map<TransferField, TransferFieldError> = emptyMap(),
    val isCancelConfirmationVisible: Boolean = false,
    val exit: TransferConfirmExit? = null
)

sealed interface TransferConfirmExit {
    data class Done(val receipt: TransferReceipt) : TransferConfirmExit

    /** Canceló tras un error de red o del servidor: el resultado avisa que no se pudo confirmar. */
    data object Failed : TransferConfirmExit

    data object BackToForm : TransferConfirmExit

    data object Cancelled : TransferConfirmExit
}

sealed interface TransferConfirmAction {
    data object ConfirmClicked : TransferConfirmAction

    data object RetryClicked : TransferConfirmAction

    data object ErrorDismissed : TransferConfirmAction

    data object RejectionAcknowledged : TransferConfirmAction

    data object CancelClicked : TransferConfirmAction

    data object CancelConfirmed : TransferConfirmAction

    data object CancelDismissed : TransferConfirmAction
}
