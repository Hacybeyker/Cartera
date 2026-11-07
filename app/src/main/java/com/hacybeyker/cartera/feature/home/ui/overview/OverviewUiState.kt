package com.hacybeyker.cartera.feature.home.ui.overview

import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.domain.account.Account

data class OverviewUiState(
    val selectedTab: OverviewTab = OverviewTab.ACCOUNTS,
    val isLoading: Boolean = true,
    val error: AppError? = null,
    val firstName: String? = null,
    val accounts: List<Account> = emptyList(),
    /** Sube con cada carga o refresco correcto: es la clave de la marca `inicio_cuentas`. */
    val contentVersion: Int = 0
) {
    val contentKey: Int? get() = contentVersion.takeIf { !isLoading && error == null }
}

sealed interface OverviewAction {
    data class TabSelected(val tab: OverviewTab) : OverviewAction

    data object RetryClicked : OverviewAction

    data object Refresh : OverviewAction
}

enum class OverviewTab {
    ACCOUNTS,
    MOVEMENTS
}
