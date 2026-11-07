package com.hacybeyker.cartera.feature.home.ui

import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.testing.FakeSessionRepository
import com.hacybeyker.cartera.core.testing.MainDispatcherRule
import com.hacybeyker.cartera.feature.home.FakeProfileRepository
import com.hacybeyker.cartera.feature.home.domain.usecase.GetProfileUseCase
import com.hacybeyker.cartera.feature.home.domain.usecase.LogOutUseCase
import com.hacybeyker.cartera.feature.home.ui.more.MoreAction
import com.hacybeyker.cartera.feature.home.ui.more.MoreViewModel
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MoreViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val profile = FakeProfileRepository()
    private val session = FakeSessionRepository().apply { start("token") }

    private fun viewModel() = MoreViewModel(GetProfileUseCase(profile), LogOutUseCase(session))

    @Test
    fun `loads the full name`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        assertEquals("Ana Torres", viewModel.uiState.value.fullName)
        assertEquals(1, viewModel.uiState.value.contentKey)
    }

    @Test
    fun `failure shows the error and retry loads again`() = runTest {
        profile.error = AppError.Network
        val viewModel = viewModel()
        advanceUntilIdle()
        assertEquals(AppError.Network, viewModel.uiState.value.error)

        profile.error = null
        viewModel.onAction(MoreAction.RetryClicked)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.error)
        assertEquals("Ana Torres", viewModel.uiState.value.fullName)
    }

    @Test
    fun `log out asks for confirmation and cancel keeps the session`() = runTest {
        val viewModel = viewModel()

        viewModel.onAction(MoreAction.LogoutClicked)
        assertTrue(viewModel.uiState.value.isLogoutConfirmationVisible)
        viewModel.onAction(MoreAction.LogoutCancelled)

        assertFalse(viewModel.uiState.value.isLogoutConfirmationVisible)
        assertFalse(viewModel.uiState.value.isLoggedOut)
        assertEquals("token", session.accessToken)
    }

    @Test
    fun `confirming log out ends the session`() = runTest {
        val viewModel = viewModel()

        viewModel.onAction(MoreAction.LogoutClicked)
        viewModel.onAction(MoreAction.LogoutConfirmed)

        assertTrue(viewModel.uiState.value.isLoggedOut)
        assertFalse(viewModel.uiState.value.isLogoutConfirmationVisible)
        assertNull(session.accessToken)
    }

    @Test
    fun `the seventh tap on version requests the hidden screen once`() = runTest {
        val viewModel = viewModel()

        repeat(6) { viewModel.onAction(MoreAction.VersionTapped) }
        assertFalse(viewModel.uiState.value.isHiddenScreenRequested)
        viewModel.onAction(MoreAction.VersionTapped)
        assertTrue(viewModel.uiState.value.isHiddenScreenRequested)

        viewModel.onAction(MoreAction.HiddenScreenOpened)
        assertFalse(viewModel.uiState.value.isHiddenScreenRequested)
        // La cuenta vuelve a empezar: un toque más no la abre de nuevo.
        viewModel.onAction(MoreAction.VersionTapped)
        assertFalse(viewModel.uiState.value.isHiddenScreenRequested)
    }
}
