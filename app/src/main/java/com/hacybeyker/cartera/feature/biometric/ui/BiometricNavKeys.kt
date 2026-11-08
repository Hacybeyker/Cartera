package com.hacybeyker.cartera.feature.biometric.ui

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** La oferta de ingreso con huella, entre el onboarding y Home. */
@Serializable
data object BiometricOfferNavKey : NavKey

@Serializable
data object SecurityNavKey : NavKey
