package com.hacybeyker.cartera.feature.onboarding.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hacybeyker.cartera.feature.onboarding.domain.usecase.CompleteOnboardingUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class OnboardingViewModel @Inject constructor(private val completeOnboarding: CompleteOnboardingUseCase) :
    ViewModel() {

    private val state = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = state.asStateFlow()

    fun onAction(action: OnboardingAction) {
        when (action) {
            OnboardingAction.SkipClicked, OnboardingAction.StartClicked -> finish()
        }
    }

    private fun finish() {
        viewModelScope.launch {
            completeOnboarding()
            state.value = OnboardingUiState(isFinished = true)
        }
    }
}
