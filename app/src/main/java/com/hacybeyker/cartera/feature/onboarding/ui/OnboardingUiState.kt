package com.hacybeyker.cartera.feature.onboarding.ui

import com.hacybeyker.cartera.core.domain.entry.EntryStep

data class OnboardingUiState(val next: EntryStep? = null)

sealed interface OnboardingAction {
    data object SkipClicked : OnboardingAction

    data object StartClicked : OnboardingAction
}
