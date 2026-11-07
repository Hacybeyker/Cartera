package com.hacybeyker.cartera.feature.splash.ui

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey

fun EntryProviderScope<NavKey>.splashEntry(onReady: () -> Unit) {
    entry<SplashNavKey> { SplashScreen(onReady = onReady) }
}
