package com.hacybeyker.cartera.feature.onboarding.ui

import com.hacybeyker.cartera.core.domain.entry.EntryStep
import com.hacybeyker.cartera.core.testing.FakeBiometricAvailability
import com.hacybeyker.cartera.core.testing.FakeFirstRunRepository
import com.hacybeyker.cartera.core.testing.MainDispatcherRule
import com.hacybeyker.cartera.core.testing.resolveEntryStep
import com.hacybeyker.cartera.feature.onboarding.domain.usecase.CompleteOnboardingUseCase
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class OnboardingViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val firstRun = FakeFirstRunRepository()
    private val availability = FakeBiometricAvailability(available = false)
    private val viewModel = OnboardingViewModel(
        CompleteOnboardingUseCase(firstRun, resolveEntryStep(firstRun = firstRun, availability = availability))
    )

    @Test
    fun `skip marks the onboarding as seen and goes home`() = runTest {
        assertNull(viewModel.uiState.value.next)

        viewModel.onAction(OnboardingAction.SkipClicked)
        advanceUntilIdle()

        assertTrue(firstRun.onboardingSeen)
        assertEquals(EntryStep.HOME, viewModel.uiState.value.next)
    }

    @Test
    fun `start on the last page does the same`() = runTest {
        viewModel.onAction(OnboardingAction.StartClicked)
        advanceUntilIdle()

        assertTrue(firstRun.onboardingSeen)
        assertEquals(EntryStep.HOME, viewModel.uiState.value.next)
    }

    @Test
    fun `with a fingerprint on the device the biometric offer follows`() = runTest {
        availability.available = true

        viewModel.onAction(OnboardingAction.StartClicked)
        advanceUntilIdle()

        assertEquals(EntryStep.BIOMETRIC_OFFER, viewModel.uiState.value.next)
    }
}
