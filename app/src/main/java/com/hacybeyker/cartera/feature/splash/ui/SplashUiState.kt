package com.hacybeyker.cartera.feature.splash.ui

import com.hacybeyker.cartera.core.domain.AppError

sealed interface SplashUiState {
    data object Loading : SplashUiState

    data object Ready : SplashUiState

    data class Error(val error: AppError) : SplashUiState
}

sealed interface SplashAction {
    data object RetryClicked : SplashAction
}
