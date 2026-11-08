package com.hacybeyker.cartera.feature.biometric.ui.security

import com.hacybeyker.cartera.core.domain.AppError

data class SecurityUiState(
    val isLoading: Boolean = true,
    /** El dispositivo tiene una huella registrada; si no, el interruptor queda deshabilitado. */
    val isAvailable: Boolean = false,
    val isBiometricEnabled: Boolean = false,
    val isWorking: Boolean = false,
    val dialogError: AppError? = null,
    /** Qué cambio falló: decide el título del modal y qué repite Reintentar. */
    val failedChange: SecurityChange? = null,
    val contentVersion: Int = 0
) {
    val contentKey: Int? get() = contentVersion.takeIf { !isLoading }
}

enum class SecurityChange {
    ENABLE,
    DISABLE
}

sealed interface SecurityAction {
    data class BiometricToggled(val enabled: Boolean) : SecurityAction

    data object RetryClicked : SecurityAction

    data object ErrorDismissed : SecurityAction
}
