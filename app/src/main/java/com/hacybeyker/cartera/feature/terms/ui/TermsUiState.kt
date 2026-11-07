package com.hacybeyker.cartera.feature.terms.ui

import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.domain.entry.EntryStep

data class TermsUiState(
    val isChecked: Boolean = false,
    val isSaving: Boolean = false,
    val error: AppError? = null,
    val exit: TermsExit? = null
) {
    val canAccept: Boolean get() = isChecked && !isSaving
}

sealed interface TermsExit {
    data class Accepted(val next: EntryStep) : TermsExit

    data object Rejected : TermsExit
}

sealed interface TermsAction {
    data class CheckedChanged(val checked: Boolean) : TermsAction

    data object AcceptClicked : TermsAction

    data object RejectClicked : TermsAction

    data object RetryClicked : TermsAction

    data object ErrorDismissed : TermsAction
}
