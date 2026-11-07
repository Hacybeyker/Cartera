package com.hacybeyker.cartera.feature.splash.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.feature.splash.domain.usecase.LoadAppConfigUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class SplashViewModel @Inject constructor(private val loadAppConfig: LoadAppConfigUseCase) : ViewModel() {

    private val state = MutableStateFlow<SplashUiState>(SplashUiState.Loading)
    val uiState: StateFlow<SplashUiState> = state.asStateFlow()

    init {
        load()
    }

    fun onAction(action: SplashAction) {
        when (action) {
            SplashAction.RetryClicked -> load()
        }
    }

    private fun load() {
        state.value = SplashUiState.Loading
        viewModelScope.launch {
            state.value = when (val outcome = loadAppConfig()) {
                is Outcome.Success -> SplashUiState.Ready
                is Outcome.Failure -> SplashUiState.Error(outcome.error)
            }
        }
    }
}
