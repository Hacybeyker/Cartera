package com.hacybeyker.cartera.feature.onboarding.ui

import com.hacybeyker.cartera.core.testing.FakeFirstRunRepository
import com.hacybeyker.cartera.core.testing.MainDispatcherRule
import com.hacybeyker.cartera.feature.onboarding.domain.usecase.CompleteOnboardingUseCase
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class OnboardingViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val firstRun = FakeFirstRunRepository()
    private val viewModel = OnboardingViewModel(CompleteOnboardingUseCase(firstRun))

    @Test
    fun `skip marks the onboarding as seen and finishes`() = runTest {
        assertFalse(viewModel.uiState.value.isFinished)

        viewModel.onAction(OnboardingAction.SkipClicked)
        advanceUntilIdle()

        assertTrue(firstRun.onboardingSeen)
        assertTrue(viewModel.uiState.value.isFinished)
    }

    @Test
    fun `start on the last page does the same`() = runTest {
        viewModel.onAction(OnboardingAction.StartClicked)
        advanceUntilIdle()

        assertTrue(firstRun.onboardingSeen)
        assertTrue(viewModel.uiState.value.isFinished)
    }
}
