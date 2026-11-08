package com.hacybeyker.cartera.feature.home.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hacybeyker.cartera.feature.home.domain.usecase.CompleteCoachMarksUseCase
import com.hacybeyker.cartera.feature.home.domain.usecase.GetPendingCoachMarkUseCase
import com.hacybeyker.cartera.feature.home.domain.usecase.LogOutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Lo que es de Home y no de un tab: los coach marks de primera vez y Cerrar sesión desde el menú lateral. */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getPendingCoachMark: GetPendingCoachMarkUseCase,
    private val completeCoachMarks: CompleteCoachMarksUseCase,
    private val logOut: LogOutUseCase
) : ViewModel() {

    private val state = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = state.asStateFlow()

    init {
        viewModelScope.launch {
            val pending = getPendingCoachMark()
            state.update { it.copy(coachMark = pending) }
        }
    }

    fun onAction(action: HomeAction) {
        when (action) {
            HomeAction.CoachNextClicked -> nextCoachMark()
            HomeAction.CoachSkipClicked -> finishCoachMarks()
            HomeAction.LogoutClicked -> state.update { it.copy(isLogoutConfirmationVisible = true) }
            HomeAction.LogoutCancelled -> state.update { it.copy(isLogoutConfirmationVisible = false) }
            HomeAction.LogoutConfirmed -> {
                logOut()
                state.update { it.copy(isLogoutConfirmationVisible = false, isLoggedOut = true) }
            }
        }
    }

    private fun nextCoachMark() {
        val current = state.value.coachMark ?: return
        val next = current.next()
        if (next == null) finishCoachMarks() else state.update { it.copy(coachMark = next) }
    }

    private fun finishCoachMarks() {
        if (state.value.coachMark == null) return
        state.update { it.copy(coachMark = null) }
        viewModelScope.launch { completeCoachMarks() }
    }
}
