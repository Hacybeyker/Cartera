package com.hacybeyker.cartera.feature.transfer.ui

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.hacybeyker.cartera.feature.transfer.ui.confirm.TransferConfirmNavigation
import com.hacybeyker.cartera.feature.transfer.ui.confirm.TransferConfirmScreen
import com.hacybeyker.cartera.feature.transfer.ui.form.TransferFormScreen
import com.hacybeyker.cartera.feature.transfer.ui.result.TransferResultScreen

fun EntryProviderScope<NavKey>.transferEntries(
    onBack: () -> Unit,
    onNavigate: (NavKey) -> Unit,
    onBackToHome: () -> Unit
) {
    entry<TransferFormNavKey> {
        TransferFormScreen(onContinue = { onNavigate(TransferConfirmNavKey(it)) }, onExit = onBack)
    }
    entry<TransferConfirmNavKey> { key ->
        TransferConfirmScreen(
            order = key.order,
            navigation = TransferConfirmNavigation(
                onBack = onBack,
                onResult = { onNavigate(TransferResultNavKey(it)) },
                onCancelled = onBackToHome
            )
        )
    }
    entry<TransferResultNavKey> { key ->
        TransferResultScreen(receipt = key.receipt, onBackToHome = onBackToHome)
    }
}
