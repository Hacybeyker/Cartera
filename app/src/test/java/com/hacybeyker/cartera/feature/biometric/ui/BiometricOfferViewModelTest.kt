package com.hacybeyker.cartera.feature.biometric.ui

import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.domain.biometric.VaultResult
import com.hacybeyker.cartera.core.testing.FakeBiometricAvailability
import com.hacybeyker.cartera.core.testing.FakeDeviceTokenVault
import com.hacybeyker.cartera.core.testing.FakeFirstRunRepository
import com.hacybeyker.cartera.core.testing.MainDispatcherRule
import com.hacybeyker.cartera.feature.biometric.FakeBiometricEnrollmentRepository
import com.hacybeyker.cartera.feature.biometric.domain.usecase.EnableBiometricLoginUseCase
import com.hacybeyker.cartera.feature.biometric.domain.usecase.SkipBiometricOfferUseCase
import com.hacybeyker.cartera.feature.biometric.ui.offer.BiometricOfferAction
import com.hacybeyker.cartera.feature.biometric.ui.offer.BiometricOfferViewModel
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class BiometricOfferViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val availability = FakeBiometricAvailability()
    private val vault = FakeDeviceTokenVault()
    private val enrollment = FakeBiometricEnrollmentRepository()
    private val firstRun = FakeFirstRunRepository()
    private val viewModel = BiometricOfferViewModel(
        enableBiometricLogin = EnableBiometricLoginUseCase(availability, vault, enrollment, firstRun),
        skipBiometricOffer = SkipBiometricOfferUseCase(firstRun)
    )

    @Test
    fun `activate blocks while it works and finishes with the token sealed`() = runTest {
        viewModel.onAction(BiometricOfferAction.ActivateClicked)
        assertTrue(viewModel.uiState.value.isActivating)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isActivating)
        assertTrue(viewModel.uiState.value.isFinished)
        assertEquals("dvc_1", vault.token)
        assertTrue(firstRun.biometricOfferResolved)
    }

    @Test
    fun `cancelling the dialog stays on the offer`() = runTest {
        vault.promptFailure = VaultResult.Cancelled

        viewModel.onAction(BiometricOfferAction.ActivateClicked)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isActivating)
        assertFalse(state.isFinished)
        assertFalse(firstRun.biometricOfferResolved)
    }

    @Test
    fun `a network error opens the modal, retry asks again and cancel closes it`() = runTest {
        enrollment.enrollError = AppError.Network
        viewModel.onAction(BiometricOfferAction.ActivateClicked)
        advanceUntilIdle()
        assertEquals(AppError.Network, viewModel.uiState.value.dialogError)

        viewModel.onAction(BiometricOfferAction.RetryClicked)
        advanceUntilIdle()
        assertEquals(2, vault.prompts)
        assertEquals(AppError.Network, viewModel.uiState.value.dialogError)

        viewModel.onAction(BiometricOfferAction.ErrorDismissed)
        assertNull(viewModel.uiState.value.dialogError)
        assertFalse(viewModel.uiState.value.isFinished)
    }

    @Test
    fun `without a fingerprint the offer says so and skip still works`() = runTest {
        availability.available = false
        viewModel.onAction(BiometricOfferAction.ActivateClicked)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isUnavailable)

        viewModel.onAction(BiometricOfferAction.SkipClicked)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isFinished)
        assertTrue(firstRun.biometricOfferResolved)
    }

    @Test
    fun `skip resolves the offer without asking for the fingerprint`() = runTest {
        viewModel.onAction(BiometricOfferAction.SkipClicked)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isFinished)
        assertTrue(firstRun.biometricOfferResolved)
        assertEquals(0, vault.prompts)
        assertEquals(0, enrollment.enrolls)
    }

    @Test
    fun `taps while activating are ignored`() = runTest {
        viewModel.onAction(BiometricOfferAction.ActivateClicked)
        viewModel.onAction(BiometricOfferAction.ActivateClicked)
        viewModel.onAction(BiometricOfferAction.SkipClicked)
        advanceUntilIdle()

        assertEquals(1, enrollment.enrolls)
        assertEquals("dvc_1", vault.token)
    }
}
