package com.hacybeyker.cartera.feature.exchange.ui

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.hacybeyker.cartera.feature.exchange.ui.confirm.FxConfirmNavigation
import com.hacybeyker.cartera.feature.exchange.ui.confirm.FxConfirmScreen
import com.hacybeyker.cartera.feature.exchange.ui.result.FxResultScreen

/** [onBackToExchange] cierra el flujo y vuelve al tab Cambio, desde el resultado o al cancelar. */
fun EntryProviderScope<NavKey>.exchangeEntries(
    onBack: () -> Unit,
    onNavigate: (NavKey) -> Unit,
    onBackToExchange: () -> Unit
) {
    entry<FxConfirmNavKey> { key ->
        FxConfirmScreen(
            order = key.order,
            navigation = FxConfirmNavigation(
                onBack = onBack,
                onResult = { onNavigate(FxResultNavKey(it)) },
                onCancelled = onBackToExchange
            )
        )
    }
    entry<FxResultNavKey> { key ->
        FxResultScreen(receipt = key.receipt, onBackToExchange = onBackToExchange)
    }
}
