package com.hacybeyker.cartera.feature.biometric.ui.offer

import com.hacybeyker.cartera.core.domain.AppError

data class BiometricOfferUiState(
    val isActivating: Boolean = false,
    /** El registro en el servidor falló: modal con Reintentar y Cancelar. */
    val dialogError: AppError? = null,
    /** El dispositivo ya no tiene huella o no se pudo crear la clave: queda Omitir. */
    val isUnavailable: Boolean = false,
    val isFinished: Boolean = false
)

sealed interface BiometricOfferAction {
    data object ActivateClicked : BiometricOfferAction

    data object SkipClicked : BiometricOfferAction

    data object RetryClicked : BiometricOfferAction

    data object ErrorDismissed : BiometricOfferAction
}
