package com.hacybeyker.cartera.feature.login.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.feature.login.domain.model.Credentials
import com.hacybeyker.cartera.feature.login.domain.usecase.LogInUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val logIn: LogInUseCase,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Solo el usuario sobrevive a la muerte del proceso: la contraseña no se escribe en el estado guardado.
    private val state = MutableStateFlow(LoginUiState(username = savedStateHandle[KEY_USERNAME] ?: ""))
    val uiState: StateFlow<LoginUiState> = state.asStateFlow()

    fun onAction(action: LoginAction) {
        when (action) {
            is LoginAction.UsernameChanged -> {
                savedStateHandle[KEY_USERNAME] = action.value
                state.update { it.copy(username = action.value, formError = null) }
            }
            is LoginAction.PasswordChanged -> state.update { it.copy(password = action.value, formError = null) }
            LoginAction.PasswordVisibilityToggled -> state.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
            LoginAction.SubmitClicked, LoginAction.RetryClicked -> submit()
            LoginAction.ErrorDismissed -> state.update { it.copy(dialogError = null) }
        }
    }

    private fun submit() {
        val form = state.value
        if (!form.canSubmit) return
        state.update { it.copy(isSubmitting = true, formError = null, dialogError = null) }
        viewModelScope.launch {
            val outcome = logIn(Credentials(username = form.username.trim(), password = form.password))
            state.update { current ->
                when (outcome) {
                    is Outcome.Success -> current.copy(isSubmitting = false, nextStep = outcome.value)
                    is Outcome.Failure -> current.withFailure(outcome.error)
                }
            }
        }
    }

    private fun LoginUiState.withFailure(error: AppError): LoginUiState = when (error) {
        AppError.InvalidCredentials -> copy(isSubmitting = false, formError = LoginFormError.INVALID_CREDENTIALS)
        is AppError.Validation -> copy(isSubmitting = false, formError = LoginFormError.INVALID_INPUT)
        else -> copy(isSubmitting = false, dialogError = error)
    }

    private companion object {
        const val KEY_USERNAME = "username"
    }
}
