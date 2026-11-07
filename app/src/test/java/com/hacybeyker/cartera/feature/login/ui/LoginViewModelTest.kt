package com.hacybeyker.cartera.feature.login.ui

import androidx.lifecycle.SavedStateHandle
import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.domain.entry.EntryStep
import com.hacybeyker.cartera.core.domain.entry.ResolveEntryStepUseCase
import com.hacybeyker.cartera.core.testing.FakeAppConfigRepository
import com.hacybeyker.cartera.core.testing.FakeFirstRunRepository
import com.hacybeyker.cartera.core.testing.FakeSessionRepository
import com.hacybeyker.cartera.core.testing.MainDispatcherRule
import com.hacybeyker.cartera.feature.login.FakeAuthRepository
import com.hacybeyker.cartera.feature.login.domain.model.Credentials
import com.hacybeyker.cartera.feature.login.domain.usecase.LogInUseCase
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
    private val savedState = SavedStateHandle()
    private val viewModel = viewModel(savedState)

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

    private fun fill() {
        viewModel.onAction(LoginAction.UsernameChanged("ana.torres"))
        viewModel.onAction(LoginAction.PasswordChanged("Cartera2026"))
    }

    private fun viewModel(savedStateHandle: SavedStateHandle) = LoginViewModel(
        logIn = LogInUseCase(
            auth = auth,
            session = FakeSessionRepository(),
            resolveEntryStep = ResolveEntryStepUseCase(FakeAppConfigRepository(), FakeFirstRunRepository())
        ),
        savedStateHandle = savedStateHandle
    )
}
