package com.hacybeyker.cartera.feature.biometric.ui

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.hacybeyker.cartera.feature.biometric.ui.offer.BiometricOfferScreen
import com.hacybeyker.cartera.feature.biometric.ui.security.SecurityScreen

/** [onOfferResolved] sigue a Home tras Activar u Omitir. */
fun EntryProviderScope<NavKey>.biometricEntries(onOfferResolved: () -> Unit, onBack: () -> Unit) {
    entry<BiometricOfferNavKey> { BiometricOfferScreen(onFinished = onOfferResolved) }
    entry<SecurityNavKey> { SecurityScreen(onBack = onBack) }
}
