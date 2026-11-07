package com.hacybeyker.cartera.feature.splash.ui

import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.testing.FakeAppConfigRepository
import com.hacybeyker.cartera.core.testing.MainDispatcherRule
import com.hacybeyker.cartera.feature.splash.domain.usecase.LoadAppConfigUseCase
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SplashViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val appConfig = FakeAppConfigRepository()

    @Test
    fun `starts loading and becomes ready with the config`() = runTest {
        val viewModel = SplashViewModel(LoadAppConfigUseCase(appConfig))
        assertEquals(SplashUiState.Loading, viewModel.uiState.value)

        advanceUntilIdle()

        assertEquals(SplashUiState.Ready, viewModel.uiState.value)
        assertEquals(1, appConfig.refreshes)
    }

    @Test
    fun `failure shows the error and retry loads again`() = runTest {
        appConfig.error = AppError.Server
        val viewModel = SplashViewModel(LoadAppConfigUseCase(appConfig))
        advanceUntilIdle()
        assertEquals(SplashUiState.Error(AppError.Server), viewModel.uiState.value)

        appConfig.error = null
        viewModel.onAction(SplashAction.RetryClicked)
        assertEquals(SplashUiState.Loading, viewModel.uiState.value)
        advanceUntilIdle()

        assertEquals(SplashUiState.Ready, viewModel.uiState.value)
        assertEquals(2, appConfig.refreshes)
    }
}
