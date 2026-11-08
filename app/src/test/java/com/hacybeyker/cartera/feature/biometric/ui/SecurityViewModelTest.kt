package com.hacybeyker.cartera.feature.biometric.ui

import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.domain.biometric.VaultResult
import com.hacybeyker.cartera.core.testing.FakeBiometricAvailability
import com.hacybeyker.cartera.core.testing.FakeDeviceTokenVault
import com.hacybeyker.cartera.core.testing.FakeFirstRunRepository
import com.hacybeyker.cartera.core.testing.MainDispatcherRule
import com.hacybeyker.cartera.core.testing.biometricStatus
import com.hacybeyker.cartera.feature.biometric.FakeBiometricEnrollmentRepository
import com.hacybeyker.cartera.feature.biometric.domain.usecase.DisableBiometricLoginUseCase
import com.hacybeyker.cartera.feature.biometric.domain.usecase.EnableBiometricLoginUseCase
import com.hacybeyker.cartera.feature.biometric.ui.security.SecurityAction
import com.hacybeyker.cartera.feature.biometric.ui.security.SecurityChange
import com.hacybeyker.cartera.feature.biometric.ui.security.SecurityViewModel
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SecurityViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val availability = FakeBiometricAvailability()
    private val vault = FakeDeviceTokenVault()
    private val enrollment = FakeBiometricEnrollmentRepository()
    private val viewModel by lazy {
        SecurityViewModel(
            getBiometricStatus = biometricStatus(availability, vault),
            enableBiometricLogin = EnableBiometricLoginUseCase(
                availability,
                vault,
                enrollment,
                FakeFirstRunRepository()
            ),
            disableBiometricLogin = DisableBiometricLoginUseCase(vault, enrollment)
        )
    }

    @Test
    fun `loads the status once and reports the content`() = runTest {
        vault.token = "dvc_1"
        assertTrue(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.contentKey)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isAvailable)
        assertTrue(state.isBiometricEnabled)
        assertEquals(1, state.contentKey)
    }

    @Test
    fun `without a fingerprint the switch cannot be turned on`() = runTest {
        availability.available = false
        loaded()
        assertFalse(viewModel.uiState.value.isAvailable)

        viewModel.onAction(SecurityAction.BiometricToggled(true))
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isBiometricEnabled)
        assertEquals(0, vault.prompts)
    }

    @Test
    fun `turning it on registers the device`() = runTest {
        loaded()

        viewModel.onAction(SecurityAction.BiometricToggled(true))
        assertTrue(viewModel.uiState.value.isWorking)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isWorking)
        assertTrue(viewModel.uiState.value.isBiometricEnabled)
        assertEquals("dvc_1", vault.token)
        assertEquals(1, viewModel.uiState.value.contentKey)
    }

    @Test
    fun `cancelling the dialog leaves it off`() = runTest {
        vault.promptFailure = VaultResult.Cancelled
        loaded()

        viewModel.onAction(SecurityAction.BiometricToggled(true))
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isWorking)
        assertFalse(viewModel.uiState.value.isBiometricEnabled)
        assertNull(viewModel.uiState.value.dialogError)
    }

    @Test
    fun `a fingerprint removed meanwhile turns the switch unavailable`() = runTest {
        loaded()
        vault.promptFailure = VaultResult.Unavailable

        viewModel.onAction(SecurityAction.BiometricToggled(true))
        availability.available = false
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isWorking)
        assertFalse(viewModel.uiState.value.isAvailable)
    }

    @Test
    fun `turning it off revokes and clears the token`() = runTest {
        vault.token = "dvc_1"
        loaded()

        viewModel.onAction(SecurityAction.BiometricToggled(false))
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isBiometricEnabled)
        assertEquals(1, enrollment.revokes)
        assertNull(vault.token)
    }

    @Test
    fun `a failed change opens the modal and retry repeats the same change`() = runTest {
        vault.token = "dvc_1"
        enrollment.revokeError = AppError.Network
        loaded()
        viewModel.onAction(SecurityAction.BiometricToggled(false))
        advanceUntilIdle()
        assertEquals(AppError.Network, viewModel.uiState.value.dialogError)
        assertEquals(SecurityChange.DISABLE, viewModel.uiState.value.failedChange)
        assertTrue(viewModel.uiState.value.isBiometricEnabled)

        enrollment.revokeError = null
        viewModel.onAction(SecurityAction.RetryClicked)
        advanceUntilIdle()

        assertEquals(2, enrollment.revokes)
        assertFalse(viewModel.uiState.value.isBiometricEnabled)
        assertNull(viewModel.uiState.value.dialogError)
    }

    @Test
    fun `a failed enable can be dismissed`() = runTest {
        enrollment.enrollError = AppError.Server
        loaded()
        viewModel.onAction(SecurityAction.BiometricToggled(true))
        advanceUntilIdle()
        assertEquals(SecurityChange.ENABLE, viewModel.uiState.value.failedChange)

        viewModel.onAction(SecurityAction.ErrorDismissed)

        assertNull(viewModel.uiState.value.dialogError)
        assertNull(viewModel.uiState.value.failedChange)
        viewModel.onAction(SecurityAction.RetryClicked)
        advanceUntilIdle()
        assertEquals(1, enrollment.enrolls)
    }

    @Test
    fun `changes are ignored while loading`() = runTest {
        viewModel.onAction(SecurityAction.BiometricToggled(true))
        advanceUntilIdle()

        assertEquals(0, vault.prompts)
    }

    private fun TestScope.loaded(): SecurityViewModel = viewModel.also { advanceUntilIdle() }
}
