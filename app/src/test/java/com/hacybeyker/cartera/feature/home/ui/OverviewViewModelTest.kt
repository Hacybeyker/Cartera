package com.hacybeyker.cartera.feature.home.ui

import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.domain.account.GetAccountsRevisionUseCase
import com.hacybeyker.cartera.core.domain.account.RefreshAccountsUseCase
import com.hacybeyker.cartera.core.domain.flags.FeatureFlag
import com.hacybeyker.cartera.core.testing.FakeAccountChanges
import com.hacybeyker.cartera.core.testing.FakeAccountRepository
import com.hacybeyker.cartera.core.testing.FakeFeatureFlags
import com.hacybeyker.cartera.core.testing.MainDispatcherRule
import com.hacybeyker.cartera.core.testing.TestAccounts
import com.hacybeyker.cartera.core.testing.pen
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

    private val changes = FakeAccountChanges()
    private val flags = FakeFeatureFlags()

    private fun viewModel() = OverviewViewModel(
        loadOverview = LoadOverviewUseCase(profile, accounts),
        refreshAccounts = RefreshAccountsUseCase(accounts, flags),
        getAccountsRevision = GetAccountsRevisionUseCase(changes)
    )

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
    fun `showing Inicio without changes does not ask again`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onAction(OverviewAction.Shown)
        advanceUntilIdle()

        assertEquals(1, accounts.accountsCalls)
        assertEquals(1, profile.calls)
        assertEquals(1, viewModel.uiState.value.contentKey)
    }

    @Test
    fun `after a transfer, showing Inicio refreshes only the accounts, once`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        changes.notifyChanged()
        accounts.accounts = TestAccounts.map { it.copy(availableBalance = pen("1.00")) }
        viewModel.onAction(OverviewAction.Shown)
        // La lista anterior sigue a la vista mientras llegan las nuevas.
        assertEquals(TestAccounts, viewModel.uiState.value.accounts)
        advanceUntilIdle()
        viewModel.onAction(OverviewAction.Shown)
        advanceUntilIdle()

        assertEquals(2, accounts.accountsCalls)
        assertEquals(1, profile.calls)
        assertEquals(pen("1.00"), viewModel.uiState.value.accounts.first().availableBalance)
        // Contenido nuevo: la marca inicio_cuentas se dispara otra vez.
        assertEquals(2, viewModel.uiState.value.contentKey)
    }

    @Test
    fun `a failed refresh shows the error and retry loads everything`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        changes.notifyChanged()
        accounts.error = AppError.Network
        viewModel.onAction(OverviewAction.Shown)
        advanceUntilIdle()
        assertEquals(AppError.Network, viewModel.uiState.value.error)

        accounts.error = null
        viewModel.onAction(OverviewAction.RetryClicked)
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.error)
        assertEquals(2, profile.calls)
    }

    @Test
    fun `remembers the selected top tab`() = runTest {
        val viewModel = viewModel()
        assertEquals(OverviewTab.ACCOUNTS, viewModel.uiState.value.selectedTab)

        viewModel.onAction(OverviewAction.TabSelected(OverviewTab.MOVEMENTS))

        assertEquals(OverviewTab.MOVEMENTS, viewModel.uiState.value.selectedTab)
    }

    @Test
    fun `with the eager refresh, coming back after a transfer asks the server twice, skipping the cache`() = runTest {
        flags.enabled += FeatureFlag.ACCOUNTS_EAGER_REFRESH
        val viewModel = viewModel()
        advanceUntilIdle()

        changes.notifyChanged()
        viewModel.onAction(OverviewAction.Shown)
        advanceUntilIdle()

        assertEquals(1, accounts.accountsCalls)
        assertEquals(2, accounts.fetchCalls)
        assertEquals(1, profile.calls)
        assertEquals(2, viewModel.uiState.value.contentKey)
    }
}
