package com.hacybeyker.cartera.feature.order.ui

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.hacybeyker.cartera.feature.order.ui.confirm.OrderConfirmNavigation
import com.hacybeyker.cartera.feature.order.ui.confirm.OrderConfirmScreen
import com.hacybeyker.cartera.feature.order.ui.form.OrderFormScreen
import com.hacybeyker.cartera.feature.order.ui.result.OrderResultScreen

/**
 * [onCancelled] recibe el símbolo: cancelar en la confirmación vuelve a la acción. [onBackToMarket] cierra el flujo
 * desde el resultado.
 */
fun EntryProviderScope<NavKey>.orderEntries(
    onBack: () -> Unit,
    onNavigate: (NavKey) -> Unit,
    onCancelled: (symbol: String) -> Unit,
    onBackToMarket: () -> Unit
) {
    entry<OrderFormNavKey> { key ->
        OrderFormScreen(
            symbol = key.symbol,
            side = key.side,
            onContinue = { onNavigate(OrderConfirmNavKey(it)) },
            onExit = onBack
        )
    }
    entry<OrderConfirmNavKey> { key ->
        OrderConfirmScreen(
            order = key.order,
            navigation = OrderConfirmNavigation(
                onBack = onBack,
                onResult = { onNavigate(OrderResultNavKey(it)) },
                onCancelled = { onCancelled(key.order.symbol) }
            )
        )
    }
    entry<OrderResultNavKey> { key ->
        OrderResultScreen(receipt = key.receipt, onBackToMarket = onBackToMarket)
    }
}
