package com.hacybeyker.cartera.feature.biometric.ui.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.core.domain.biometric.GetBiometricStatusUseCase
import com.hacybeyker.cartera.feature.biometric.domain.model.EnableBiometricResult
import com.hacybeyker.cartera.feature.biometric.domain.usecase.DisableBiometricLoginUseCase
import com.hacybeyker.cartera.feature.biometric.domain.usecase.EnableBiometricLoginUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SecurityViewModel @Inject constructor(
    private val getBiometricStatus: GetBiometricStatusUseCase,
    private val enableBiometricLogin: EnableBiometricLoginUseCase,
    private val disableBiometricLogin: DisableBiometricLoginUseCase
) : ViewModel() {

    private val state = MutableStateFlow(SecurityUiState())
    val uiState: StateFlow<SecurityUiState> = state.asStateFlow()

    init {
        viewModelScope.launch { refreshStatus() }
    }

    fun onAction(action: SecurityAction) {
        when (action) {
            is SecurityAction.BiometricToggled -> {
                change(if (action.enabled) SecurityChange.ENABLE else SecurityChange.DISABLE)
            }
            SecurityAction.RetryClicked -> state.value.failedChange?.let(::change)
            SecurityAction.ErrorDismissed -> state.update { it.copy(dialogError = null, failedChange = null) }
        }
    }

    private fun change(change: SecurityChange) {
        val current = state.value
        if (current.isLoading || current.isWorking) return
        state.update { it.copy(isWorking = true, dialogError = null, failedChange = null) }
        viewModelScope.launch {
            when (change) {
                SecurityChange.ENABLE -> enable()
                SecurityChange.DISABLE -> disable()
            }
        }
    }

    private suspend fun enable() {
        when (val result = enableBiometricLogin()) {
            EnableBiometricResult.Enabled -> state.update { it.copy(isWorking = false, isBiometricEnabled = true) }
            EnableBiometricResult.Cancelled -> state.update { it.copy(isWorking = false) }
            // Pudo quitarse la huella del dispositivo: se vuelve a leer para deshabilitar el interruptor.
            EnableBiometricResult.Unavailable -> refreshStatus()
            is EnableBiometricResult.Failure -> state.update {
                it.copy(isWorking = false, dialogError = result.error, failedChange = SecurityChange.ENABLE)
            }
        }
    }

    private suspend fun disable() {
        when (val outcome = disableBiometricLogin()) {
            is Outcome.Success -> state.update { it.copy(isWorking = false, isBiometricEnabled = false) }
            is Outcome.Failure -> state.update {
                it.copy(isWorking = false, dialogError = outcome.error, failedChange = SecurityChange.DISABLE)
            }
        }
    }

    private suspend fun refreshStatus() {
        val status = getBiometricStatus()
        state.update {
            it.copy(
                isLoading = false,
                isWorking = false,
                isAvailable = status.isAvailable,
                isBiometricEnabled = status.isEnabled,
                contentVersion = if (it.isLoading) it.contentVersion + 1 else it.contentVersion
            )
        }
    }
}
