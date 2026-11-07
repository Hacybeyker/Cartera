package com.hacybeyker.cartera.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey

/**
 * Entradas de navegación que otras variantes suman a [AppNavDisplay]; [install] recibe cómo volver atrás. En
 * release el conjunto queda vacío.
 */
fun interface NavEntryInstaller {
    fun EntryProviderScope<NavKey>.install(onBack: () -> Unit)
}

/** Pantalla que no figura en ningún menú: se abre con 7 toques en Versión. Release no tiene ninguna. */
fun interface HiddenScreen {
    fun navKey(): NavKey
}
