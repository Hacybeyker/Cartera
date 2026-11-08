package com.hacybeyker.cartera.feature.login.ui

import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.domain.entry.EntryStep

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val isSubmitting: Boolean = false,
    /** Se muestra bajo el formulario. */
    val formError: LoginFormError? = null,
    /** Se muestra en un modal con Reintentar y Cancelar. */
    val dialogError: AppError? = null,
    val nextStep: EntryStep? = null,
    /** Ya se revisó si hay un ingreso con huella guardado; hasta entonces no se avisa la marca. */
    val isBiometricChecked: Boolean = false,
    val isBiometricVisible: Boolean = false
) {
    val canSubmit: Boolean get() = username.isNotBlank() && password.isNotBlank() && !isSubmitting
}

enum class LoginFormError {
    INVALID_CREDENTIALS,
    INVALID_INPUT,

    /** El servidor ya no reconoce el dispositivo para ingresar con huella. */
    DEVICE_REVOKED,

    /** Se registró una huella nueva y la clave del ingreso con huella quedó invalidada. */
    BIOMETRIC_CHANGED
}

sealed interface LoginAction {
    data class UsernameChanged(val value: String) : LoginAction

    data class PasswordChanged(val value: String) : LoginAction

    data object PasswordVisibilityToggled : LoginAction

    data object SubmitClicked : LoginAction

    data object BiometricClicked : LoginAction

    data object RetryClicked : LoginAction

    data object ErrorDismissed : LoginAction
}
