package com.hacybeyker.cartera.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey

/** Entradas de navegación que otras variantes suman a [AppNavDisplay]. En release el conjunto queda vacío. */
fun interface NavEntryInstaller {
    fun EntryProviderScope<NavKey>.install()
}
