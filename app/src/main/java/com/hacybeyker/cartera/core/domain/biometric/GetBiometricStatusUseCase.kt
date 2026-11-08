package com.hacybeyker.cartera.core.domain.biometric

import javax.inject.Inject

class GetBiometricStatusUseCase @Inject constructor(
    private val availability: BiometricAvailability,
    private val vault: DeviceTokenVault
) {
    suspend operator fun invoke(): BiometricStatus =
        BiometricStatus(isAvailable = availability.isAvailable(), token = vault.state())
}
