package com.hacybeyker.cartera.feature.legacy.ui

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey

fun EntryProviderScope<NavKey>.legacyEntry(onBack: () -> Unit) {
    entry<LegacyNavKey> { LegacyScreen(onBack = onBack) }
}
