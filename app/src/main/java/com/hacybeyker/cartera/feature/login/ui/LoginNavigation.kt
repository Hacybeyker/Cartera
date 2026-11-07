package com.hacybeyker.cartera.feature.login.ui

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.hacybeyker.cartera.core.domain.entry.EntryStep

fun EntryProviderScope<NavKey>.loginEntry(onLoggedIn: (EntryStep) -> Unit) {
    entry<LoginNavKey> { LoginScreen(onLoggedIn = onLoggedIn) }
}
