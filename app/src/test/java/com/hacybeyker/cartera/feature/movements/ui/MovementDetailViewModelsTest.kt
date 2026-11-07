package com.hacybeyker.cartera.feature.movements.ui

import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.testing.MainDispatcherRule
import com.hacybeyker.cartera.feature.movements.FakeMovementDetailRepository
import com.hacybeyker.cartera.feature.movements.TestBreakdown
import com.hacybeyker.cartera.feature.movements.TestDetail
import com.hacybeyker.cartera.feature.movements.domain.usecase.GetBreakdownUseCase
import com.hacybeyker.cartera.feature.movements.domain.usecase.GetMovementDetailUseCase
import com.hacybeyker.cartera.feature.movements.ui.breakdown.MovementBreakdownAction
import com.hacybeyker.cartera.feature.movements.ui.breakdown.MovementBreakdownViewModel
import com.hacybeyker.cartera.feature.movements.ui.detail.MovementDetailAction
import com.hacybeyker.cartera.feature.movements.ui.detail.MovementDetailViewModel
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MovementDetailViewModelsTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeMovementDetailRepository()

    @Test
    fun `detail loads the movement`() = runTest {
        val viewModel = MovementDetailViewModel("MOV-002-0001", GetMovementDetailUseCase(repository))
        assertTrue(viewModel.uiState.value.isLoading)
        advanceUntilIdle()

        assertEquals(TestDetail, viewModel.uiState.value.content)
        assertEquals(1, viewModel.uiState.value.contentKey)
        assertEquals(listOf("MOV-002-0001"), repository.requested)
    }

    @Test
    fun `detail failure shows the error and retry loads again`() = runTest {
        repository.failures += AppError.Network
        val viewModel = MovementDetailViewModel("MOV-002-0001", GetMovementDetailUseCase(repository))
        advanceUntilIdle()
        assertEquals(AppError.Network, viewModel.uiState.value.error)
        assertNull(viewModel.uiState.value.contentKey)

        viewModel.onAction(MovementDetailAction.RetryClicked)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.error)
        assertEquals(TestDetail, viewModel.uiState.value.content)
    }

    @Test
    fun `breakdown loads the lines`() = runTest {
        val viewModel = MovementBreakdownViewModel("MOV-002-0001", GetBreakdownUseCase(repository))
        advanceUntilIdle()

        assertEquals(TestBreakdown, viewModel.uiState.value.content)
        assertEquals(1, viewModel.uiState.value.contentKey)
    }

    @Test
    fun `breakdown not found shows the error and retry loads again`() = runTest {
        repository.failures += AppError.NotFound
        val viewModel = MovementBreakdownViewModel("MOV-999", GetBreakdownUseCase(repository))
        advanceUntilIdle()
        assertEquals(AppError.NotFound, viewModel.uiState.value.error)

        viewModel.onAction(MovementBreakdownAction.RetryClicked)
        advanceUntilIdle()

        assertEquals(TestBreakdown, viewModel.uiState.value.content)
    }
}
