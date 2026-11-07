package com.hacybeyker.cartera.feature.home.ui.more

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.feature.home.domain.usecase.GetProfileUseCase
import com.hacybeyker.cartera.feature.home.domain.usecase.LogOutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class MoreViewModel @Inject constructor(private val getProfile: GetProfileUseCase, private val logOut: LogOutUseCase) :
    ViewModel() {

    private val state = MutableStateFlow(MoreUiState())
    val uiState: StateFlow<MoreUiState> = state.asStateFlow()

    private var versionTaps = 0

    init {
        load()
    }

    fun onAction(action: MoreAction) {
        when (action) {
            MoreAction.RetryClicked -> load()
            MoreAction.LogoutClicked -> state.update { it.copy(isLogoutConfirmationVisible = true) }
            MoreAction.LogoutCancelled -> state.update { it.copy(isLogoutConfirmationVisible = false) }
            MoreAction.LogoutConfirmed -> {
                logOut()
                state.update { it.copy(isLogoutConfirmationVisible = false, isLoggedOut = true) }
            }
            MoreAction.VersionTapped -> onVersionTapped()
            MoreAction.HiddenScreenOpened -> state.update { it.copy(isHiddenScreenRequested = false) }
        }
    }

    private fun onVersionTapped() {
        versionTaps++
        if (versionTaps < HIDDEN_SCREEN_TAPS) return
        versionTaps = 0
        state.update { it.copy(isHiddenScreenRequested = true) }
    }

    private fun load() {
        state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val outcome = getProfile()
            state.update { current ->
                when (outcome) {
                    is Outcome.Success -> current.copy(
                        isLoading = false,
                        fullName = outcome.value.fullName,
                        contentVersion = current.contentVersion + 1
                    )
                    is Outcome.Failure -> current.copy(isLoading = false, error = outcome.error)
                }
            }
        }
    }

    private companion object {
        const val HIDDEN_SCREEN_TAPS = 7
    }
}
