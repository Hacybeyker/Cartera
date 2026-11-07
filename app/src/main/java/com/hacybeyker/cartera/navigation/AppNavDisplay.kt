package com.hacybeyker.cartera.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.hacybeyker.cartera.feature.home.ui.HomeNavKey
import com.hacybeyker.cartera.feature.home.ui.homeEntry

@Composable
fun AppNavDisplay(modifier: Modifier = Modifier, extraEntries: Set<NavEntryInstaller> = emptySet()) {
    val backStack = rememberNavBackStack(HomeNavKey)
    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        entryProvider = entryProvider {
            homeEntry()
            extraEntries.forEach { installer -> with(installer) { install() } }
        }
    )
}
