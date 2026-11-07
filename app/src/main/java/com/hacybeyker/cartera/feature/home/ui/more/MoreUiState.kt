package com.hacybeyker.cartera.feature.home.ui.more

import com.hacybeyker.cartera.core.domain.AppError

data class MoreUiState(
    val isLoading: Boolean = true,
    val error: AppError? = null,
    val fullName: String? = null,
    val isLogoutConfirmationVisible: Boolean = false,
    val isLoggedOut: Boolean = false,
    /** Llegó al séptimo toque en Versión; la UI abre la pantalla oculta y lo confirma con HiddenScreenOpened. */
    val isHiddenScreenRequested: Boolean = false,
    val contentVersion: Int = 0
) {
    val contentKey: Int? get() = contentVersion.takeIf { !isLoading && error == null }
}

sealed interface MoreAction {
    data object RetryClicked : MoreAction

    data object LogoutClicked : MoreAction

    data object LogoutConfirmed : MoreAction

    data object LogoutCancelled : MoreAction

    data object VersionTapped : MoreAction

    data object HiddenScreenOpened : MoreAction
}
