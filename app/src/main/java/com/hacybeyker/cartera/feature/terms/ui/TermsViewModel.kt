package com.hacybeyker.cartera.feature.terms.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.feature.terms.domain.usecase.AcceptTermsUseCase
import com.hacybeyker.cartera.feature.terms.domain.usecase.RejectTermsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class TermsViewModel @Inject constructor(
    private val acceptTerms: AcceptTermsUseCase,
    private val rejectTerms: RejectTermsUseCase
) : ViewModel() {

    private val state = MutableStateFlow(TermsUiState())
    val uiState: StateFlow<TermsUiState> = state.asStateFlow()

    fun onAction(action: TermsAction) {
        when (action) {
            is TermsAction.CheckedChanged -> state.update { it.copy(isChecked = action.checked) }
            TermsAction.AcceptClicked, TermsAction.RetryClicked -> accept()
            TermsAction.RejectClicked -> {
                rejectTerms()
                state.update { it.copy(exit = TermsExit.Rejected) }
            }
            TermsAction.ErrorDismissed -> state.update { it.copy(error = null) }
        }
    }

    private fun accept() {
        if (!state.value.canAccept) return
        state.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            val outcome = acceptTerms()
            state.update { current ->
                when (outcome) {
                    is Outcome.Success -> current.copy(isSaving = false, exit = TermsExit.Accepted(outcome.value))
                    is Outcome.Failure -> current.copy(isSaving = false, error = outcome.error)
                }
            }
        }
    }
}
