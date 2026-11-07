package com.hacybeyker.cartera.feature.movements.ui.breakdown

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hacybeyker.cartera.core.ui.LoadUiState
import com.hacybeyker.cartera.feature.movements.domain.model.Breakdown
import com.hacybeyker.cartera.feature.movements.domain.usecase.GetBreakdownUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface MovementBreakdownAction {
    data object RetryClicked : MovementBreakdownAction
}

@HiltViewModel(assistedFactory = MovementBreakdownViewModel.Factory::class)
class MovementBreakdownViewModel @AssistedInject constructor(
    @Assisted private val movementId: String,
    private val getBreakdown: GetBreakdownUseCase
) : ViewModel() {

    private val state = MutableStateFlow(LoadUiState<Breakdown>())
    val uiState: StateFlow<LoadUiState<Breakdown>> = state.asStateFlow()

    init {
        load()
    }

    fun onAction(action: MovementBreakdownAction) {
        when (action) {
            MovementBreakdownAction.RetryClicked -> load()
        }
    }

    private fun load() {
        state.update { it.loading() }
        viewModelScope.launch {
            val outcome = getBreakdown(movementId)
            state.update { it.loaded(outcome) }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(movementId: String): MovementBreakdownViewModel
    }
}
