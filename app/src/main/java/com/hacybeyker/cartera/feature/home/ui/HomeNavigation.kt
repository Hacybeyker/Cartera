package com.hacybeyker.cartera.feature.home.ui

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey

/** [tabRequest] pide mostrar un tab al volver a Home; Home lo aplica y lo consume. */
fun EntryProviderScope<NavKey>.homeEntry(actions: HomeActions, tabRequest: HomeTabRequest? = null) {
    entry<HomeNavKey> { HomeScreen(actions = actions, tabRequest = tabRequest) }
}
