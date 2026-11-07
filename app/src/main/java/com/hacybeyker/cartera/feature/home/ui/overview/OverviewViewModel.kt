package com.hacybeyker.cartera.feature.home.ui.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.feature.home.domain.usecase.LoadOverviewUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class OverviewViewModel @Inject constructor(private val loadOverview: LoadOverviewUseCase) : ViewModel() {

    private val state = MutableStateFlow(OverviewUiState())
    val uiState: StateFlow<OverviewUiState> = state.asStateFlow()

    init {
        load()
    }

    fun onAction(action: OverviewAction) {
        when (action) {
            is OverviewAction.TabSelected -> state.update { it.copy(selectedTab = action.tab) }
            OverviewAction.RetryClicked, OverviewAction.Refresh -> load()
        }
    }

    private fun load() {
        state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val outcome = loadOverview()
            state.update { current ->
                when (outcome) {
                    is Outcome.Success -> current.copy(
                        isLoading = false,
                        firstName = outcome.value.profile.firstName,
                        accounts = outcome.value.accounts,
                        contentVersion = current.contentVersion + 1
                    )
                    is Outcome.Failure -> current.copy(isLoading = false, error = outcome.error)
                }
            }
        }
    }
}
