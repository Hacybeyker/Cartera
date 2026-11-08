package com.hacybeyker.cartera.feature.biometric.domain.usecase

import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.core.domain.biometric.BiometricAvailability
import com.hacybeyker.cartera.core.domain.biometric.DeviceTokenVault
import com.hacybeyker.cartera.core.domain.biometric.PendingSeal
import com.hacybeyker.cartera.core.domain.biometric.VaultResult
import com.hacybeyker.cartera.core.domain.firstrun.FirstRunRepository
import com.hacybeyker.cartera.feature.biometric.domain.model.EnableBiometricResult
import com.hacybeyker.cartera.feature.biometric.domain.repository.BiometricEnrollmentRepository
import javax.inject.Inject

/**
 * Primero la huella y después el registro en el servidor: si se cancela el diálogo, el servidor no emite ningún
 * `deviceToken`. Activarlo, desde la oferta o desde Seguridad, resuelve la oferta.
 */
class EnableBiometricLoginUseCase @Inject constructor(
    private val availability: BiometricAvailability,
    private val vault: DeviceTokenVault,
    private val enrollment: BiometricEnrollmentRepository,
    private val firstRun: FirstRunRepository
) {
    suspend operator fun invoke(): EnableBiometricResult {
        if (!availability.isAvailable()) return EnableBiometricResult.Unavailable
        return when (val unlocked = vault.unlockForSealing()) {
            is VaultResult.Success -> register(unlocked.value)
            VaultResult.Cancelled -> EnableBiometricResult.Cancelled
            VaultResult.Invalidated, VaultResult.Unavailable -> EnableBiometricResult.Unavailable
        }
    }

    private suspend fun register(pending: PendingSeal): EnableBiometricResult =
        when (val enrolled = enrollment.enroll()) {
            is Outcome.Success -> seal(pending, enrolled.value)
            is Outcome.Failure -> {
                pending.discard()
                EnableBiometricResult.Failure(enrolled.error)
            }
        }

    private suspend fun seal(pending: PendingSeal, deviceToken: String): EnableBiometricResult {
        if (!pending.seal(deviceToken)) return EnableBiometricResult.Unavailable
        firstRun.markBiometricOfferResolved()
        return EnableBiometricResult.Enabled
    }
}
