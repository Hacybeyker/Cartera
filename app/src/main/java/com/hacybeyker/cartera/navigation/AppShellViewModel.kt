package com.hacybeyker.cartera.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hacybeyker.cartera.core.domain.session.AcknowledgeSessionExpiryUseCase
import com.hacybeyker.cartera.core.domain.session.ObserveSessionExpiredUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class AppShellUiState(val isSessionExpired: Boolean = false)

sealed interface AppShellAction {
    data object SessionExpiredAcknowledged : AppShellAction
}

/** Estado de la app por encima de las pantallas: hoy, solo el modal de sesión expirada. */
@HiltViewModel
class AppShellViewModel @Inject constructor(
    observeSessionExpired: ObserveSessionExpiredUseCase,
    private val acknowledgeSessionExpiry: AcknowledgeSessionExpiryUseCase
) : ViewModel() {

    val uiState: StateFlow<AppShellUiState> = observeSessionExpired()
        .map { AppShellUiState(isSessionExpired = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), AppShellUiState())

    fun onAction(action: AppShellAction) {
        when (action) {
            AppShellAction.SessionExpiredAcknowledged -> acknowledgeSessionExpiry()
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
