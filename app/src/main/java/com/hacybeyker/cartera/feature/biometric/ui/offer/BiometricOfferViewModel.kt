package com.hacybeyker.cartera.feature.biometric.ui.offer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hacybeyker.cartera.feature.biometric.domain.model.EnableBiometricResult
import com.hacybeyker.cartera.feature.biometric.domain.usecase.EnableBiometricLoginUseCase
import com.hacybeyker.cartera.feature.biometric.domain.usecase.SkipBiometricOfferUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class BiometricOfferViewModel @Inject constructor(
    private val enableBiometricLogin: EnableBiometricLoginUseCase,
    private val skipBiometricOffer: SkipBiometricOfferUseCase
) : ViewModel() {

    private val state = MutableStateFlow(BiometricOfferUiState())
    val uiState: StateFlow<BiometricOfferUiState> = state.asStateFlow()

    fun onAction(action: BiometricOfferAction) {
        when (action) {
            BiometricOfferAction.ActivateClicked, BiometricOfferAction.RetryClicked -> activate()
            BiometricOfferAction.SkipClicked -> skip()
            BiometricOfferAction.ErrorDismissed -> state.update { it.copy(dialogError = null) }
        }
    }

    private fun activate() {
        if (state.value.isActivating) return
        state.update { it.copy(isActivating = true, dialogError = null, isUnavailable = false) }
        viewModelScope.launch {
            val result = enableBiometricLogin()
            state.update { current ->
                when (result) {
                    EnableBiometricResult.Enabled -> current.copy(isActivating = false, isFinished = true)
                    EnableBiometricResult.Cancelled -> current.copy(isActivating = false)
                    EnableBiometricResult.Unavailable -> current.copy(isActivating = false, isUnavailable = true)
                    is EnableBiometricResult.Failure -> current.copy(isActivating = false, dialogError = result.error)
                }
            }
        }
    }

    private fun skip() {
        if (state.value.isActivating) return
        viewModelScope.launch {
            skipBiometricOffer()
            state.update { it.copy(isFinished = true) }
        }
    }
}
