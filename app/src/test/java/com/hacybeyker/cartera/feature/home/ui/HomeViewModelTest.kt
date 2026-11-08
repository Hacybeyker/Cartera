package com.hacybeyker.cartera.feature.home.ui

import com.hacybeyker.cartera.core.testing.FakeFirstRunRepository
import com.hacybeyker.cartera.core.testing.FakeSessionRepository
import com.hacybeyker.cartera.core.testing.MainDispatcherRule
import com.hacybeyker.cartera.feature.home.domain.model.CoachMark
import com.hacybeyker.cartera.feature.home.domain.usecase.CompleteCoachMarksUseCase
import com.hacybeyker.cartera.feature.home.domain.usecase.GetPendingCoachMarkUseCase
import com.hacybeyker.cartera.feature.home.domain.usecase.LogOutUseCase
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HomeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val firstRun = FakeFirstRunRepository()
    private val session = FakeSessionRepository().apply { start("token") }

    private fun viewModel() = HomeViewModel(
        GetPendingCoachMarkUseCase(firstRun),
        CompleteCoachMarksUseCase(firstRun),
        LogOutUseCase(session)
    )

    @Test
    fun `the first visit shows the first coach mark`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        assertEquals(CoachMark.TRANSFER, viewModel.uiState.value.coachMark)
    }

    @Test
    fun `next walks the three steps and done on the last one finishes them for good`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onAction(HomeAction.CoachNextClicked)
        assertEquals(CoachMark.MARKET_TAB, viewModel.uiState.value.coachMark)
        viewModel.onAction(HomeAction.CoachNextClicked)
        assertEquals(CoachMark.MENU, viewModel.uiState.value.coachMark)
        assertFalse(firstRun.coachMarksSeen)

        viewModel.onAction(HomeAction.CoachNextClicked)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.coachMark)
        assertTrue(firstRun.coachMarksSeen)
    }

    @Test
    fun `skip on any step finishes them for good`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.onAction(HomeAction.CoachNextClicked)

        viewModel.onAction(HomeAction.CoachSkipClicked)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.coachMark)
        assertTrue(firstRun.coachMarksSeen)
    }

    @Test
    fun `a later visit, after logging out or a cold start, shows no coach marks`() = runTest {
        val first = viewModel()
        advanceUntilIdle()
        first.onAction(HomeAction.CoachSkipClicked)
        advanceUntilIdle()

        val again = viewModel()
        advanceUntilIdle()

        assertNull(again.uiState.value.coachMark)
    }

    @Test
    fun `steps not finished come back on the next visit`() = runTest {
        val first = viewModel()
        advanceUntilIdle()
        first.onAction(HomeAction.CoachNextClicked)

        val again = viewModel()
        advanceUntilIdle()

        assertEquals(CoachMark.TRANSFER, again.uiState.value.coachMark)
        assertFalse(firstRun.coachMarksSeen)
    }

    @Test
    fun `next and skip without coach marks change nothing`() = runTest {
        firstRun.coachMarksSeen = true
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onAction(HomeAction.CoachNextClicked)
        viewModel.onAction(HomeAction.CoachSkipClicked)
        advanceUntilIdle()

        assertEquals(HomeUiState(), viewModel.uiState.value)
    }

    @Test
    fun `log out from the menu asks for confirmation and cancel keeps the session`() = runTest {
        val viewModel = viewModel()

        viewModel.onAction(HomeAction.LogoutClicked)
        assertTrue(viewModel.uiState.value.isLogoutConfirmationVisible)
        viewModel.onAction(HomeAction.LogoutCancelled)

        assertFalse(viewModel.uiState.value.isLogoutConfirmationVisible)
        assertFalse(viewModel.uiState.value.isLoggedOut)
        assertEquals("token", session.accessToken)
    }

    @Test
    fun `confirming log out from the menu ends the session`() = runTest {
        val viewModel = viewModel()

        viewModel.onAction(HomeAction.LogoutClicked)
        viewModel.onAction(HomeAction.LogoutConfirmed)

        assertTrue(viewModel.uiState.value.isLoggedOut)
        assertFalse(viewModel.uiState.value.isLogoutConfirmationVisible)
        assertNull(session.accessToken)
    }
}
