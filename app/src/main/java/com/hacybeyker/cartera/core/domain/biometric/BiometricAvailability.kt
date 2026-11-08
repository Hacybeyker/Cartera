package com.hacybeyker.cartera.core.domain.biometric

/** Si el dispositivo tiene una huella (biometría fuerte) registrada y lista para usar. */
fun interface BiometricAvailability {
    fun isAvailable(): Boolean
}
