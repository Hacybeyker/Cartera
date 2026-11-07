package com.hacybeyker.cartera.feature.movements.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.core.domain.movement.GetMovementsPageUseCase
import com.hacybeyker.cartera.core.ui.paging.NextPageLoader
import com.hacybeyker.cartera.core.ui.paging.PagedList
import com.hacybeyker.cartera.feature.movements.domain.usecase.LoadAccountMovementsUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = AccountMovementsViewModel.Factory::class)
class AccountMovementsViewModel @AssistedInject constructor(
    @Assisted private val accountId: String,
    private val loadAccountMovements: LoadAccountMovementsUseCase,
    private val getMovementsPage: GetMovementsPageUseCase
) : ViewModel() {

    private val state = MutableStateFlow(AccountMovementsUiState())
    val uiState: StateFlow<AccountMovementsUiState> = state.asStateFlow()

    private val nextPages = NextPageLoader(
        scope = viewModelScope,
        current = { state.value.list.movements },
        update = { transform ->
            state.update { it.copy(list = it.list.copy(movements = transform(it.list.movements))) }
        },
        fetch = { cursor -> getMovementsPage(accountId = accountId, cursor = cursor) }
    )

    init {
        load()
    }

    fun onAction(action: AccountMovementsAction) {
        when (action) {
            AccountMovementsAction.RetryClicked -> load()
            AccountMovementsAction.LoadMore -> nextPages.loadMore()
            AccountMovementsAction.RetryLoadMoreClicked -> nextPages.retry()
        }
    }

    private fun load() {
        state.update { it.copy(list = it.list.copy(isLoading = true, error = null)) }
        viewModelScope.launch {
            val outcome = loadAccountMovements(accountId)
            state.update { current ->
                when (outcome) {
                    is Outcome.Success -> current.copy(
                        account = outcome.value.account,
                        list = current.list.copy(
                            isLoading = false,
                            movements = PagedList.of(outcome.value.firstPage),
                            contentVersion = current.list.contentVersion + 1
                        )
                    )
                    is Outcome.Failure -> current.copy(
                        list = current.list.copy(isLoading = false, error = outcome.error)
                    )
                }
            }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(accountId: String): AccountMovementsViewModel
    }
}
