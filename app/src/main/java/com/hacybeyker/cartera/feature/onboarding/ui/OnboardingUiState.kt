package com.hacybeyker.cartera.feature.onboarding.ui

data class OnboardingUiState(val isFinished: Boolean = false)

sealed interface OnboardingAction {
    data object SkipClicked : OnboardingAction

    data object StartClicked : OnboardingAction
}
