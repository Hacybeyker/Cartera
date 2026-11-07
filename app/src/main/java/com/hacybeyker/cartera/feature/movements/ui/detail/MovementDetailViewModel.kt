package com.hacybeyker.cartera.feature.movements.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hacybeyker.cartera.core.ui.LoadUiState
import com.hacybeyker.cartera.feature.movements.domain.model.MovementDetail
import com.hacybeyker.cartera.feature.movements.domain.usecase.GetMovementDetailUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface MovementDetailAction {
    data object RetryClicked : MovementDetailAction
}

@HiltViewModel(assistedFactory = MovementDetailViewModel.Factory::class)
class MovementDetailViewModel @AssistedInject constructor(
    @Assisted private val movementId: String,
    private val getMovementDetail: GetMovementDetailUseCase
) : ViewModel() {

    private val state = MutableStateFlow(LoadUiState<MovementDetail>())
    val uiState: StateFlow<LoadUiState<MovementDetail>> = state.asStateFlow()

    init {
        load()
    }

    fun onAction(action: MovementDetailAction) {
        when (action) {
            MovementDetailAction.RetryClicked -> load()
        }
    }

    private fun load() {
        state.update { it.loading() }
        viewModelScope.launch {
            val outcome = getMovementDetail(movementId)
            state.update { it.loaded(outcome) }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(movementId: String): MovementDetailViewModel
    }
}
