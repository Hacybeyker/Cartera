package com.hacybeyker.cartera.feature.movements.ui

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.hacybeyker.cartera.feature.movements.ui.account.AccountMovementsScreen
import com.hacybeyker.cartera.feature.movements.ui.breakdown.MovementBreakdownScreen
import com.hacybeyker.cartera.feature.movements.ui.detail.MovementDetailScreen

fun EntryProviderScope<NavKey>.movementsEntries(
    onBack: () -> Unit,
    onMovementClick: (movementId: String) -> Unit,
    onBreakdownClick: (movementId: String) -> Unit
) {
    entry<AccountMovementsNavKey> { key ->
        AccountMovementsScreen(accountId = key.accountId, onBack = onBack, onMovementClick = onMovementClick)
    }
    entry<MovementDetailNavKey> { key ->
        MovementDetailScreen(movementId = key.movementId, onBack = onBack, onBreakdownClick = onBreakdownClick)
    }
    entry<MovementBreakdownNavKey> { key ->
        MovementBreakdownScreen(movementId = key.movementId, onBack = onBack)
    }
}
