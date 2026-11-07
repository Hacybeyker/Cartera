package com.hacybeyker.cartera.feature.terms.ui

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.hacybeyker.cartera.core.domain.entry.EntryStep

fun EntryProviderScope<NavKey>.termsEntry(onAccepted: (EntryStep) -> Unit, onRejected: () -> Unit) {
    entry<TermsNavKey> { TermsScreen(onAccepted = onAccepted, onRejected = onRejected) }
}
