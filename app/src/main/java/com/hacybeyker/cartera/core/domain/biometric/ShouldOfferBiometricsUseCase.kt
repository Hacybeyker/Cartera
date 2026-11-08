package com.hacybeyker.cartera.core.domain.biometric

import com.hacybeyker.cartera.core.domain.firstrun.FirstRunRepository
import javax.inject.Inject

/**
 * La oferta se muestra una vez por instalación: hasta que se resuelve con Activar u Omitir, y solo si el dispositivo
 * tiene huella y todavía no está registrado.
 */
class ShouldOfferBiometricsUseCase @Inject constructor(
    private val firstRun: FirstRunRepository,
    private val getStatus: GetBiometricStatusUseCase
) {
    suspend operator fun invoke(): Boolean {
        if (firstRun.isBiometricOfferResolved()) return false
        val status = getStatus()
        return status.isAvailable && status.token != DeviceTokenState.READY
    }
}
