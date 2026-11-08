package com.hacybeyker.cartera.feature.help.ui

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey

fun EntryProviderScope<NavKey>.helpEntry(onBack: () -> Unit) {
    entry<HelpNavKey> { HelpContent(onBack = onBack) }
}
