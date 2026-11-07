package com.hacybeyker.cartera.feature.home.ui

import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.testing.FakeAccountRepository
import com.hacybeyker.cartera.core.testing.MainDispatcherRule
import com.hacybeyker.cartera.core.testing.TestAccounts
import com.hacybeyker.cartera.feature.home.FakeProfileRepository
import com.hacybeyker.cartera.feature.home.domain.usecase.LoadOverviewUseCase
import com.hacybeyker.cartera.feature.home.ui.overview.OverviewAction
import com.hacybeyker.cartera.feature.home.ui.overview.OverviewTab
import com.hacybeyker.cartera.feature.home.ui.overview.OverviewViewModel
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class OverviewViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val profile = FakeProfileRepository()
    private val accounts = FakeAccountRepository()

    private fun viewModel() = OverviewViewModel(LoadOverviewUseCase(profile, accounts))

    @Test
    fun `loads the greeting and the accounts`() = runTest {
        val viewModel = viewModel()
        assertTrue(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.contentKey)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Ana", state.firstName)
        assertEquals(TestAccounts, state.accounts)
        assertEquals(1, state.contentKey)
    }

    @Test
    fun `failure shows the error and retry loads again`() = runTest {
        accounts.error = AppError.Network
        val viewModel = viewModel()
        advanceUntilIdle()
        assertEquals(AppError.Network, viewModel.uiState.value.error)
        assertNull(viewModel.uiState.value.contentKey)

        accounts.error = null
        viewModel.onAction(OverviewAction.RetryClicked)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.error)
        assertEquals(TestAccounts, viewModel.uiState.value.accounts)
    }

    @Test
    fun `each refresh is new content for the visible mark`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onAction(OverviewAction.Refresh)
        assertNull(viewModel.uiState.value.contentKey)
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.contentKey)
        assertEquals(2, accounts.accountsCalls)
    }

    @Test
    fun `remembers the selected top tab`() = runTest {
        val viewModel = viewModel()
        assertEquals(OverviewTab.ACCOUNTS, viewModel.uiState.value.selectedTab)

        viewModel.onAction(OverviewAction.TabSelected(OverviewTab.MOVEMENTS))

        assertEquals(OverviewTab.MOVEMENTS, viewModel.uiState.value.selectedTab)
    }
}
