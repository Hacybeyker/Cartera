package com.hacybeyker.cartera.feature.home.ui

import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.domain.movement.GetMovementsPageUseCase
import com.hacybeyker.cartera.core.testing.FakeMovementRepository
import com.hacybeyker.cartera.core.testing.MainDispatcherRule
import com.hacybeyker.cartera.feature.home.ui.overview.AllMovementsAction
import com.hacybeyker.cartera.feature.home.ui.overview.AllMovementsViewModel
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AllMovementsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val movements = FakeMovementRepository()

    private fun viewModel() = AllMovementsViewModel(GetMovementsPageUseCase(movements))

    @Test
    fun `loads the first page of every account when created`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(20, state.movements.items.size)
        assertTrue(state.movements.hasMore)
        assertEquals(1, state.contentKey)
        assertEquals(listOf<String?>(null), movements.requestedAccounts)
    }

    @Test
    fun `loads the next pages with the cursor until the end of the list`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onAction(AllMovementsAction.LoadMore)
        assertTrue(viewModel.uiState.value.movements.isLoadingMore)
        // Mientras carga, otro aviso de scroll no repite la llamada.
        viewModel.onAction(AllMovementsAction.LoadMore)
        advanceUntilIdle()
        viewModel.onAction(AllMovementsAction.LoadMore)
        advanceUntilIdle()

        val list = viewModel.uiState.value.movements
        assertEquals(45, list.items.size)
        assertFalse(list.hasMore)
        assertEquals(listOf(null, "MOV-002-0020", "MOV-002-0040"), movements.requestedCursors)

        viewModel.onAction(AllMovementsAction.LoadMore)
        advanceUntilIdle()
        assertEquals(3, movements.requestedCursors.size)
        // Cargar más no es contenido nuevo para la marca.
        assertEquals(1, viewModel.uiState.value.contentKey)
    }

    @Test
    fun `a failed load more keeps the list and retry continues from the same cursor`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        movements.failures += AppError.Network
        viewModel.onAction(AllMovementsAction.LoadMore)
        advanceUntilIdle()
        var list = viewModel.uiState.value.movements
        assertEquals(20, list.items.size)
        assertEquals(AppError.Network, list.loadMoreError)

        // El scroll no reintenta solo: hace falta tocar Reintentar.
        viewModel.onAction(AllMovementsAction.LoadMore)
        advanceUntilIdle()
        assertEquals(2, movements.requestedCursors.size)

        viewModel.onAction(AllMovementsAction.RetryLoadMoreClicked)
        advanceUntilIdle()
        list = viewModel.uiState.value.movements
        assertEquals(40, list.items.size)
        assertNull(list.loadMoreError)
        assertEquals(listOf(null, "MOV-002-0020", "MOV-002-0020"), movements.requestedCursors)
    }

    @Test
    fun `retry load more does nothing without a failure`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onAction(AllMovementsAction.RetryLoadMoreClicked)
        advanceUntilIdle()

        assertEquals(1, movements.requestedCursors.size)
    }

    @Test
    fun `a failed first page shows the error and retry loads it`() = runTest {
        movements.failures += AppError.Server
        val viewModel = viewModel()
        advanceUntilIdle()
        assertEquals(AppError.Server, viewModel.uiState.value.error)
        assertNull(viewModel.uiState.value.contentKey)

        viewModel.onAction(AllMovementsAction.RetryClicked)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.error)
        assertEquals(20, viewModel.uiState.value.movements.items.size)
    }

    @Test
    fun `no movements is the empty state`() = runTest {
        movements.movements = emptyList()
        val viewModel = viewModel()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isEmpty)
    }
}
