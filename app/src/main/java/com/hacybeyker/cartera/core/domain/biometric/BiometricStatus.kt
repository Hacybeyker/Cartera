package com.hacybeyker.cartera.core.domain.biometric

data class BiometricStatus(val isAvailable: Boolean, val token: DeviceTokenState) {
    /** Hay un token guardado que la huella puede abrir. */
    val isEnabled: Boolean get() = isAvailable && token == DeviceTokenState.READY
}
