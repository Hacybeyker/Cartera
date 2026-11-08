package com.hacybeyker.cartera.feature.login.ui

import androidx.lifecycle.SavedStateHandle
import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.domain.biometric.VaultResult
import com.hacybeyker.cartera.core.domain.entry.EntryStep
import com.hacybeyker.cartera.core.testing.FakeBiometricAvailability
import com.hacybeyker.cartera.core.testing.FakeDeviceTokenVault
import com.hacybeyker.cartera.core.testing.FakeSessionRepository
import com.hacybeyker.cartera.core.testing.MainDispatcherRule
import com.hacybeyker.cartera.core.testing.biometricStatus
import com.hacybeyker.cartera.core.testing.resolveEntryStep
import com.hacybeyker.cartera.feature.login.FakeAuthRepository
import com.hacybeyker.cartera.feature.login.domain.model.Credentials
import com.hacybeyker.cartera.feature.login.domain.usecase.LogInUseCase
import com.hacybeyker.cartera.feature.login.domain.usecase.LogInWithBiometricsUseCase
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LoginViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val auth = FakeAuthRepository()
    private val session = FakeSessionRepository()
    private val availability = FakeBiometricAvailability()
    private val vault = FakeDeviceTokenVault()
    private val savedState = SavedStateHandle()
    private val viewModel by lazy { viewModel(savedState) }

    @Test
    fun `submit is enabled only with both fields`() {
        assertFalse(viewModel.uiState.value.canSubmit)

        viewModel.onAction(LoginAction.UsernameChanged("ana.torres"))
        assertFalse(viewModel.uiState.value.canSubmit)
        viewModel.onAction(LoginAction.PasswordChanged("Cartera2026"))

        assertTrue(viewModel.uiState.value.canSubmit)
    }

    @Test
    fun `submit with empty fields does nothing`() = runTest {
        viewModel.onAction(LoginAction.SubmitClicked)
        advanceUntilIdle()

        assertTrue(auth.attempts.isEmpty())
    }

    @Test
    fun `success blocks while submitting and exposes the next step`() = runTest {
        fill()

        viewModel.onAction(LoginAction.SubmitClicked)
        assertTrue(viewModel.uiState.value.isSubmitting)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSubmitting)
        assertEquals(EntryStep.TERMS, viewModel.uiState.value.nextStep)
        assertEquals(listOf(Credentials("ana.torres", "Cartera2026")), auth.attempts)
    }

    @Test
    fun `invalid credentials show under the form and clear when typing`() = runTest {
        auth.error = AppError.InvalidCredentials
        fill()

        viewModel.onAction(LoginAction.SubmitClicked)
        advanceUntilIdle()

        assertEquals(LoginFormError.INVALID_CREDENTIALS, viewModel.uiState.value.formError)
        assertNull(viewModel.uiState.value.dialogError)
        viewModel.onAction(LoginAction.PasswordChanged("otra"))
        assertNull(viewModel.uiState.value.formError)
    }

    @Test
    fun `validation errors show under the form too`() = runTest {
        auth.error = AppError.Validation(mapOf("username" to "REQUIRED"))
        fill()

        viewModel.onAction(LoginAction.SubmitClicked)
        advanceUntilIdle()

        assertEquals(LoginFormError.INVALID_INPUT, viewModel.uiState.value.formError)
    }

    @Test
    fun `network error opens the modal, retry repeats and cancel closes it`() = runTest {
        auth.error = AppError.Network
        fill()
        viewModel.onAction(LoginAction.SubmitClicked)
        advanceUntilIdle()
        assertEquals(AppError.Network, viewModel.uiState.value.dialogError)

        viewModel.onAction(LoginAction.RetryClicked)
        advanceUntilIdle()
        assertEquals(2, auth.attempts.size)

        viewModel.onAction(LoginAction.ErrorDismissed)
        assertNull(viewModel.uiState.value.dialogError)
    }

    @Test
    fun `password visibility toggles`() {
        viewModel.onAction(LoginAction.PasswordVisibilityToggled)
        assertTrue(viewModel.uiState.value.isPasswordVisible)

        viewModel.onAction(LoginAction.PasswordVisibilityToggled)
        assertFalse(viewModel.uiState.value.isPasswordVisible)
    }

    @Test
    fun `only the username survives in the saved state`() {
        fill()

        val restored = viewModel(savedState).uiState.value

        assertEquals("ana.torres", restored.username)
        assertEquals("", restored.password)
    }

    @Test
    fun `username is trimmed before logging in`() = runTest {
        viewModel.onAction(LoginAction.UsernameChanged(" ana.torres "))
        viewModel.onAction(LoginAction.PasswordChanged("Cartera2026"))

        viewModel.onAction(LoginAction.SubmitClicked)
        advanceUntilIdle()

        assertEquals("ana.torres", auth.attempts.single().username)
    }

    @Test
    fun `without a registered device the biometric button stays hidden and the mark waits for the check`() = runTest {
        assertFalse(viewModel.uiState.value.isBiometricChecked)
        loaded()

        assertTrue(viewModel.uiState.value.isBiometricChecked)
        assertFalse(viewModel.uiState.value.isBiometricVisible)
    }

    @Test
    fun `a registered device logs in with the fingerprint and follows the entry`() = runTest {
        vault.token = "dvc_1"
        loaded()
        assertTrue(viewModel.uiState.value.isBiometricVisible)

        viewModel.onAction(LoginAction.BiometricClicked)
        assertTrue(viewModel.uiState.value.isSubmitting)
        advanceUntilIdle()

        assertEquals(EntryStep.TERMS, viewModel.uiState.value.nextStep)
        assertEquals(listOf("dvc_1"), auth.deviceTokens)
        assertEquals("jwt-device-1", session.accessToken)
        assertTrue(auth.attempts.isEmpty())
    }

    @Test
    fun `without a fingerprint on the device the button stays hidden even with a token`() = runTest {
        vault.token = "dvc_1"
        availability.available = false
        loaded()

        assertFalse(viewModel.uiState.value.isBiometricVisible)
        viewModel.onAction(LoginAction.BiometricClicked)
        advanceUntilIdle()
        assertEquals(0, vault.prompts)
    }

    @Test
    fun `cancelling the dialog does nothing`() = runTest {
        vault.token = "dvc_1"
        vault.promptFailure = VaultResult.Cancelled
        loaded()

        viewModel.onAction(LoginAction.BiometricClicked)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isSubmitting)
        assertTrue(state.isBiometricVisible)
        assertNull(state.formError)
        assertNull(state.dialogError)
        assertNull(state.nextStep)
        assertEquals("dvc_1", vault.token)
    }

    @Test
    fun `a revoked device hides the button and explains it under the form`() = runTest {
        vault.token = "dvc_1"
        auth.error = AppError.InvalidDevice
        loaded()

        viewModel.onAction(LoginAction.BiometricClicked)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isBiometricVisible)
        assertEquals(LoginFormError.DEVICE_REVOKED, state.formError)
        assertNull(state.dialogError)
        assertNull(vault.token)
    }

    @Test
    fun `a key invalidated by a new fingerprint hides the button with its message`() = runTest {
        vault.token = "dvc_1"
        loaded()
        vault.keyInvalidated = true

        viewModel.onAction(LoginAction.BiometricClicked)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isBiometricVisible)
        assertEquals(LoginFormError.BIOMETRIC_CHANGED, viewModel.uiState.value.formError)
        assertTrue(auth.deviceTokens.isEmpty())
    }

    @Test
    fun `a key already invalidated when the screen opens shows the message and no button`() = runTest {
        vault.token = "dvc_1"
        vault.keyInvalidated = true
        loaded()

        assertFalse(viewModel.uiState.value.isBiometricVisible)
        assertEquals(LoginFormError.BIOMETRIC_CHANGED, viewModel.uiState.value.formError)
    }

    @Test
    fun `a network error with the fingerprint retries with the fingerprint`() = runTest {
        vault.token = "dvc_1"
        auth.error = AppError.Network
        loaded()
        viewModel.onAction(LoginAction.BiometricClicked)
        advanceUntilIdle()
        assertEquals(AppError.Network, viewModel.uiState.value.dialogError)

        auth.error = null
        viewModel.onAction(LoginAction.RetryClicked)
        advanceUntilIdle()

        assertEquals(2, vault.prompts)
        assertEquals(EntryStep.TERMS, viewModel.uiState.value.nextStep)
        assertTrue(auth.attempts.isEmpty())
    }

    private fun fill() {
        viewModel.onAction(LoginAction.UsernameChanged("ana.torres"))
        viewModel.onAction(LoginAction.PasswordChanged("Cartera2026"))
    }

    private fun TestScope.loaded(): LoginViewModel = viewModel.also { advanceUntilIdle() }

    private fun viewModel(savedStateHandle: SavedStateHandle) = LoginViewModel(
        logIn = LogInUseCase(auth = auth, session = session, resolveEntryStep = resolveEntryStep()),
        logInWithBiometrics = LogInWithBiometricsUseCase(vault, auth, session, resolveEntryStep()),
        getBiometricStatus = biometricStatus(availability, vault),
        savedStateHandle = savedStateHandle
    )
}
