package com.hacybeyker.cartera.feature.exchange.ui.confirm

import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.domain.Money
import com.hacybeyker.cartera.core.domain.exchange.ExchangeField
import com.hacybeyker.cartera.core.domain.exchange.ExchangeFieldError
import com.hacybeyker.cartera.core.domain.exchange.ExchangeOrder
import com.hacybeyker.cartera.core.domain.market.FxQuote
import com.hacybeyker.cartera.feature.exchange.domain.model.ExchangeReceipt

data class FxConfirmUiState(
    val order: ExchangeOrder,
    /** Segundos que le quedan a la cotización; en 0, venció. */
    val remainingSeconds: Long,
    val isSubmitting: Boolean = false,
    /** Se está pidiendo una cotización nueva porque la de [order] venció. */
    val isRenewingQuote: Boolean = false,
    /** Red o servidor: modal con Reintentar y Cancelar. */
    val failure: FxConfirmFailure? = null,
    /** Fondos insuficientes o monedas que no corresponden: un modal propio que vuelve a Cambio. */
    val rejection: AppError? = null,
    /** `VALIDATION_ERROR`: el mensaje de cada campo bajo su fila del resumen. */
    val fieldErrors: Map<ExchangeField, ExchangeFieldError> = emptyMap(),
    /** La cotización venció y esta es la nueva que se ofrece en `modal_cotizacion_vencida`. */
    val newQuote: FxQuote? = null,
    val isCancelConfirmationVisible: Boolean = false,
    val exit: FxConfirmExit? = null
) {
    val isQuoteExpired: Boolean get() = remainingSeconds <= 0

    val isBusy: Boolean get() = isSubmitting || isRenewingQuote

    /** Sin nada en curso ni ningún modal a la vista. */
    val isIdle: Boolean
        get() = !isBusy &&
            failure == null &&
            rejection == null &&
            newQuote == null &&
            !isCancelConfirmationVisible &&
            exit == null

    /** Lo que se acreditaría con [newQuote]. */
    val newCredit: Money? get() = newQuote?.let { order.withQuote(it, order.idempotencyKey).credit }
}

/** Qué falló: el envío del cambio o el pedido de una cotización nueva. Reintentar repite lo mismo. */
data class FxConfirmFailure(val error: AppError, val step: FxConfirmStep)

enum class FxConfirmStep {
    CONFIRM,
    RENEW_QUOTE
}

sealed interface FxConfirmExit {
    data class Done(val receipt: ExchangeReceipt) : FxConfirmExit

    /** Canceló tras un error de red o del servidor: el resultado avisa que no se pudo confirmar. */
    data object Failed : FxConfirmExit

    /** Vuelve a Cambio sin haber cambiado nada (un rechazo, salir del modal de la cotización vencida). */
    data object BackToForm : FxConfirmExit

    data object Cancelled : FxConfirmExit
}

sealed interface FxConfirmAction {
    data object ConfirmClicked : FxConfirmAction

    data object RetryClicked : FxConfirmAction

    data object ErrorDismissed : FxConfirmAction

    data object RejectionAcknowledged : FxConfirmAction

    data object NewQuoteAccepted : FxConfirmAction

    data object NewQuoteDeclined : FxConfirmAction

    data object CancelClicked : FxConfirmAction

    data object CancelConfirmed : FxConfirmAction

    data object CancelDismissed : FxConfirmAction
}
