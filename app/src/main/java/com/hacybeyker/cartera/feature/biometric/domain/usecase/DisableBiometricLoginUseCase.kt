package com.hacybeyker.cartera.feature.biometric.domain.usecase

import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.core.domain.biometric.DeviceTokenVault
import com.hacybeyker.cartera.feature.biometric.domain.repository.BiometricEnrollmentRepository
import javax.inject.Inject

/** Revoca el dispositivo en el servidor y, solo si lo logró, borra el token cifrado y la clave. */
class DisableBiometricLoginUseCase @Inject constructor(
    private val vault: DeviceTokenVault,
    private val enrollment: BiometricEnrollmentRepository
) {
    suspend operator fun invoke(): Outcome<Unit> = enrollment.revoke().also { outcome ->
        if (outcome is Outcome.Success) vault.clear()
    }
}
