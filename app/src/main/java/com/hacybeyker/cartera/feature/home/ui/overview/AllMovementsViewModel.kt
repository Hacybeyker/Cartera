package com.hacybeyker.cartera.feature.home.ui.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.core.domain.movement.GetMovementsPageUseCase
import com.hacybeyker.cartera.core.ui.movement.MovementListUiState
import com.hacybeyker.cartera.core.ui.paging.NextPageLoader
import com.hacybeyker.cartera.core.ui.paging.PagedList
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface AllMovementsAction {
    data object RetryClicked : AllMovementsAction

    data object LoadMore : AllMovementsAction

    data object RetryLoadMoreClicked : AllMovementsAction
}

/** Inicio · Movimientos: se crea, y carga, la primera vez que se abre el tab. */
@HiltViewModel
class AllMovementsViewModel @Inject constructor(private val getMovementsPage: GetMovementsPageUseCase) :
    ViewModel() {

    private val state = MutableStateFlow(MovementListUiState())
    val uiState: StateFlow<MovementListUiState> = state.asStateFlow()

    private val nextPages = NextPageLoader(
        scope = viewModelScope,
        current = { state.value.movements },
        update = { transform -> state.update { it.copy(movements = transform(it.movements)) } },
        fetch = { cursor -> getMovementsPage(cursor = cursor) }
    )

    init {
        load()
    }

    fun onAction(action: AllMovementsAction) {
        when (action) {
            AllMovementsAction.RetryClicked -> load()
            AllMovementsAction.LoadMore -> nextPages.loadMore()
            AllMovementsAction.RetryLoadMoreClicked -> nextPages.retry()
        }
    }

    private fun load() {
        state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val outcome = getMovementsPage()
            state.update { current ->
                when (outcome) {
                    is Outcome.Success -> current.copy(
                        isLoading = false,
                        movements = PagedList.of(outcome.value),
                        contentVersion = current.contentVersion + 1
                    )
                    is Outcome.Failure -> current.copy(isLoading = false, error = outcome.error)
                }
            }
        }
    }
}
