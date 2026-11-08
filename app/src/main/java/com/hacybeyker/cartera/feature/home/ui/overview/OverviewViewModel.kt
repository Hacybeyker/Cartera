package com.hacybeyker.cartera.feature.home.ui.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.core.domain.account.GetAccountsRevisionUseCase
import com.hacybeyker.cartera.core.domain.account.RefreshAccountsUseCase
import com.hacybeyker.cartera.feature.home.domain.usecase.LoadOverviewUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class OverviewViewModel @Inject constructor(
    private val loadOverview: LoadOverviewUseCase,
    private val refreshAccounts: RefreshAccountsUseCase,
    private val getAccountsRevision: GetAccountsRevisionUseCase
) : ViewModel() {

    private val state = MutableStateFlow(OverviewUiState())
    val uiState: StateFlow<OverviewUiState> = state.asStateFlow()

    // Revisión de las cuentas que muestra la pantalla: si la de ahora es otra, hubo una transferencia.
    private var shownRevision = getAccountsRevision()

    init {
        load()
    }

    fun onAction(action: OverviewAction) {
        when (action) {
            is OverviewAction.TabSelected -> state.update { it.copy(selectedTab = action.tab) }
            OverviewAction.RetryClicked -> load()
            OverviewAction.Shown -> if (hasChanged()) refresh()
        }
    }

    private fun hasChanged(): Boolean = !state.value.isLoading && getAccountsRevision() != shownRevision

    private fun load() {
        shownRevision = getAccountsRevision()
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

    // El saludo no cambia con una transferencia: solo se vuelven a pedir las cuentas, y la lista anterior sigue a la
    // vista hasta que llegan las nuevas.
    private fun refresh() {
        shownRevision = getAccountsRevision()
        viewModelScope.launch {
            val outcome = refreshAccounts()
            state.update { current ->
                when (outcome) {
                    is Outcome.Success -> current.copy(
                        accounts = outcome.value,
                        contentVersion = current.contentVersion + 1
                    )
                    is Outcome.Failure -> current.copy(error = outcome.error)
                }
            }
        }
    }
}
