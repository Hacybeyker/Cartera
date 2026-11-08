package com.hacybeyker.cartera.feature.terms.ui

import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.domain.entry.EntryStep
import com.hacybeyker.cartera.core.testing.FakeAppConfigRepository
import com.hacybeyker.cartera.core.testing.FakeFirstRunRepository
import com.hacybeyker.cartera.core.testing.FakeSessionRepository
import com.hacybeyker.cartera.core.testing.MainDispatcherRule
import com.hacybeyker.cartera.core.testing.resolveEntryStep
import com.hacybeyker.cartera.feature.terms.domain.usecase.AcceptTermsUseCase
import com.hacybeyker.cartera.feature.terms.domain.usecase.RejectTermsUseCase
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TermsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val appConfig = FakeAppConfigRepository()
    private val firstRun = FakeFirstRunRepository()
    private val session = FakeSessionRepository().apply { start("jwt") }
    private val viewModel = TermsViewModel(
        acceptTerms = AcceptTermsUseCase(appConfig, firstRun, resolveEntryStep(appConfig, firstRun)),
        rejectTerms = RejectTermsUseCase(session)
    )

    @Test
    fun `accept is enabled only after checking the box`() {
        assertFalse(viewModel.uiState.value.canAccept)

        viewModel.onAction(TermsAction.CheckedChanged(true))

        assertTrue(viewModel.uiState.value.canAccept)
    }

    @Test
    fun `accept without the box checked does nothing`() = runTest {
        viewModel.onAction(TermsAction.AcceptClicked)
        advanceUntilIdle()

        assertNull(firstRun.acceptedTermsVersion)
        assertNull(viewModel.uiState.value.exit)
    }

    @Test
    fun `accepting exits to the next step`() = runTest {
        viewModel.onAction(TermsAction.CheckedChanged(true))

        viewModel.onAction(TermsAction.AcceptClicked)
        assertTrue(viewModel.uiState.value.isSaving)
        advanceUntilIdle()

        assertEquals(TermsExit.Accepted(EntryStep.ONBOARDING), viewModel.uiState.value.exit)
        assertFalse(viewModel.uiState.value.isSaving)
    }

    @Test
    fun `failure opens the modal, retry accepts and cancel closes it`() = runTest {
        appConfig.error = AppError.Network
        viewModel.onAction(TermsAction.CheckedChanged(true))
        viewModel.onAction(TermsAction.AcceptClicked)
        advanceUntilIdle()
        assertEquals(AppError.Network, viewModel.uiState.value.error)

        viewModel.onAction(TermsAction.ErrorDismissed)
        assertNull(viewModel.uiState.value.error)

        appConfig.error = null
        viewModel.onAction(TermsAction.RetryClicked)
        advanceUntilIdle()
        assertEquals(TermsExit.Accepted(EntryStep.ONBOARDING), viewModel.uiState.value.exit)
    }

    @Test
    fun `rejecting ends the session and exits`() {
        viewModel.onAction(TermsAction.RejectClicked)

        assertEquals(TermsExit.Rejected, viewModel.uiState.value.exit)
        assertNull(session.accessToken)
    }
}
