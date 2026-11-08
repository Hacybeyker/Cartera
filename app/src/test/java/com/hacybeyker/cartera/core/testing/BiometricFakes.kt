package com.hacybeyker.cartera.core.testing

import com.hacybeyker.cartera.core.domain.biometric.BiometricAvailability
import com.hacybeyker.cartera.core.domain.biometric.DeviceTokenState
import com.hacybeyker.cartera.core.domain.biometric.DeviceTokenVault
import com.hacybeyker.cartera.core.domain.biometric.GetBiometricStatusUseCase
import com.hacybeyker.cartera.core.domain.biometric.PendingSeal
import com.hacybeyker.cartera.core.domain.biometric.ShouldOfferBiometricsUseCase
import com.hacybeyker.cartera.core.domain.biometric.VaultResult
import com.hacybeyker.cartera.core.domain.config.AppConfigRepository
import com.hacybeyker.cartera.core.domain.entry.ResolveEntryStepUseCase
import com.hacybeyker.cartera.core.domain.firstrun.FirstRunRepository

class FakeBiometricAvailability(var available: Boolean = true) : BiometricAvailability {
    override fun isAvailable(): Boolean = available
}

/** Un baúl en memoria: la "huella" responde con [promptFailure], o con éxito si es `null`. */
class FakeDeviceTokenVault(var token: String? = null) : DeviceTokenVault {
    var promptFailure: VaultResult<Nothing>? = null
    var keyInvalidated = false
    var sealFails = false
    var prompts = 0
    var discards = 0
    var clears = 0

    override suspend fun state(): DeviceTokenState = when {
        token == null -> DeviceTokenState.NONE
        keyInvalidated -> {
            token = null
            DeviceTokenState.INVALIDATED
        }
        else -> DeviceTokenState.READY
    }

    override suspend fun unlockForSealing(): VaultResult<PendingSeal> {
        prompts++
        token = null
        return promptFailure ?: VaultResult.Success(
            object : PendingSeal {
                override suspend fun seal(deviceToken: String): Boolean {
                    if (!sealFails) token = deviceToken
                    return !sealFails
                }

                override suspend fun discard() {
                    discards++
                }
            }
        )
    }

    override suspend fun open(): VaultResult<String> {
        prompts++
        val stored = token
        if (stored == null || keyInvalidated) {
            token = null
            return VaultResult.Invalidated
        }
        return promptFailure ?: VaultResult.Success(stored)
    }

    override suspend fun clear() {
        clears++
        token = null
    }
}

fun biometricStatus(
    availability: BiometricAvailability = FakeBiometricAvailability(),
    vault: DeviceTokenVault = FakeDeviceTokenVault()
) = GetBiometricStatusUseCase(availability, vault)

/** Si no se indica, el dispositivo no tiene huella: la entrada sigue como en la fase 1, sin la oferta. */
fun resolveEntryStep(
    appConfig: AppConfigRepository = FakeAppConfigRepository(),
    firstRun: FirstRunRepository = FakeFirstRunRepository(),
    availability: BiometricAvailability = FakeBiometricAvailability(available = false),
    vault: DeviceTokenVault = FakeDeviceTokenVault()
) = ResolveEntryStepUseCase(
    appConfig = appConfig,
    firstRun = firstRun,
    shouldOfferBiometrics = ShouldOfferBiometricsUseCase(firstRun, biometricStatus(availability, vault))
)
