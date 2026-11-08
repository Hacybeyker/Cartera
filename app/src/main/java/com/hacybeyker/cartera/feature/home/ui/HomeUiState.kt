package com.hacybeyker.cartera.feature.home.ui

import com.hacybeyker.cartera.feature.home.domain.model.CoachMark

data class HomeUiState(
    /** El paso de los coach marks a la vista, o `null` si no hay capa. */
    val coachMark: CoachMark? = null,
    val isLogoutConfirmationVisible: Boolean = false,
    val isLoggedOut: Boolean = false
)

sealed interface HomeAction {
    /** Siguiente, o Entendido en el último paso. */
    data object CoachNextClicked : HomeAction

    data object CoachSkipClicked : HomeAction

    /** Cerrar sesión desde el menú lateral: pide la misma confirmación que Más. */
    data object LogoutClicked : HomeAction

    data object LogoutConfirmed : HomeAction

    data object LogoutCancelled : HomeAction
}
