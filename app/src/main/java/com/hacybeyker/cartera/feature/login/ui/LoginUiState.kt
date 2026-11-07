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
    val nextStep: EntryStep? = null
) {
    val canSubmit: Boolean get() = username.isNotBlank() && password.isNotBlank() && !isSubmitting
}

enum class LoginFormError {
    INVALID_CREDENTIALS,
    INVALID_INPUT
}

sealed interface LoginAction {
    data class UsernameChanged(val value: String) : LoginAction

    data class PasswordChanged(val value: String) : LoginAction

    data object PasswordVisibilityToggled : LoginAction

    data object SubmitClicked : LoginAction

    data object RetryClicked : LoginAction

    data object ErrorDismissed : LoginAction
}
