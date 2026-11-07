package com.hacybeyker.cartera.feature.movements.ui

import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.domain.movement.GetMovementsPageUseCase
import com.hacybeyker.cartera.core.testing.FakeAccountRepository
import com.hacybeyker.cartera.core.testing.FakeMovementRepository
import com.hacybeyker.cartera.core.testing.MainDispatcherRule
import com.hacybeyker.cartera.core.testing.TestAccounts
import com.hacybeyker.cartera.core.testing.testMovement
import com.hacybeyker.cartera.feature.movements.domain.usecase.LoadAccountMovementsUseCase
import com.hacybeyker.cartera.feature.movements.ui.account.AccountMovementsAction
import com.hacybeyker.cartera.feature.movements.ui.account.AccountMovementsViewModel
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class AccountMovementsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val accounts = FakeAccountRepository()
    private val movements = FakeMovementRepository(
        movements = (1..25).map { testMovement(it, "ACC-002") } + (1..5).map { testMovement(it, "ACC-001") }
    )

    private fun viewModel(accountId: String = "ACC-002") = AccountMovementsViewModel(
        accountId = accountId,
        loadAccountMovements = LoadAccountMovementsUseCase(accounts, movements),
        getMovementsPage = GetMovementsPageUseCase(movements)
    )

    @Test
    fun `loads the account header and its first page`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(TestAccounts[1], state.account)
        assertEquals(20, state.list.movements.items.size)
        assertEquals(1, state.list.contentKey)
    }

    @Test
    fun `next page stays in the same account and ends the list`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onAction(AccountMovementsAction.LoadMore)
        advanceUntilIdle()

        val list = viewModel.uiState.value.list.movements
        assertEquals(25, list.items.size)
        assertFalse(list.hasMore)
        assertEquals(listOf<String?>("ACC-002", "ACC-002"), movements.requestedAccounts)
    }

    @Test
    fun `a failed load more shows the retry row and retry recovers`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        movements.failures += AppError.Server
        viewModel.onAction(AccountMovementsAction.LoadMore)
        advanceUntilIdle()
        assertEquals(AppError.Server, viewModel.uiState.value.list.movements.loadMoreError)
        assertEquals(20, viewModel.uiState.value.list.movements.items.size)

        viewModel.onAction(AccountMovementsAction.RetryLoadMoreClicked)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.list.movements.loadMoreError)
        assertEquals(25, viewModel.uiState.value.list.movements.items.size)
    }

    @Test
    fun `an unknown account is not found and retry tries again`() = runTest {
        val viewModel = viewModel(accountId = "ACC-999")
        advanceUntilIdle()
        assertEquals(AppError.NotFound, viewModel.uiState.value.list.error)
        assertNull(viewModel.uiState.value.list.contentKey)

        viewModel.onAction(AccountMovementsAction.RetryClicked)
        advanceUntilIdle()

        assertEquals(AppError.NotFound, viewModel.uiState.value.list.error)
    }
}
