package com.hacybeyker.cartera.feature.home.ui

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey

fun EntryProviderScope<NavKey>.homeEntry() {
    entry<HomeNavKey> { HomeScreen() }
}
