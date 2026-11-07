package com.hacybeyker.cartera.feature.movements.ui.account

import com.hacybeyker.cartera.core.domain.account.Account
import com.hacybeyker.cartera.core.ui.movement.MovementListUiState

data class AccountMovementsUiState(val account: Account? = null, val list: MovementListUiState = MovementListUiState())

sealed interface AccountMovementsAction {
    data object RetryClicked : AccountMovementsAction

    data object LoadMore : AccountMovementsAction

    data object RetryLoadMoreClicked : AccountMovementsAction
}
