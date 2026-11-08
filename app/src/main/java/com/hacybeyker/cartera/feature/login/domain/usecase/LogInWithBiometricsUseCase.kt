package com.hacybeyker.cartera.feature.login.domain.usecase

import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.core.domain.biometric.DeviceTokenVault
import com.hacybeyker.cartera.core.domain.biometric.VaultResult
import com.hacybeyker.cartera.core.domain.entry.ResolveEntryStepUseCase
import com.hacybeyker.cartera.core.domain.session.SessionRepository
import com.hacybeyker.cartera.feature.login.domain.model.BiometricDisabledReason
import com.hacybeyker.cartera.feature.login.domain.model.BiometricLoginResult
import com.hacybeyker.cartera.feature.login.domain.repository.AuthRepository
import javax.inject.Inject

/** La huella abre el `deviceToken` y el servidor lo cambia por la misma sesión en memoria que el login con clave. */
class LogInWithBiometricsUseCase @Inject constructor(
    private val vault: DeviceTokenVault,
    private val auth: AuthRepository,
    private val session: SessionRepository,
    private val resolveEntryStep: ResolveEntryStepUseCase
) {
    suspend operator fun invoke(): BiometricLoginResult = when (val opened = vault.open()) {
        is VaultResult.Success -> logIn(opened.value)
        VaultResult.Cancelled, VaultResult.Unavailable -> BiometricLoginResult.Cancelled
        VaultResult.Invalidated -> BiometricLoginResult.Disabled(BiometricDisabledReason.KEY_INVALIDATED)
    }

    private suspend fun logIn(deviceToken: String): BiometricLoginResult {
        val authSession = when (val outcome = auth.logInWithDevice(deviceToken)) {
            is Outcome.Success -> outcome.value
            is Outcome.Failure -> return failure(outcome.error)
        }
        session.start(authSession.accessToken)
        return when (val next = resolveEntryStep()) {
            is Outcome.Success -> BiometricLoginResult.LoggedIn(next.value)
            is Outcome.Failure -> BiometricLoginResult.Failure(next.error)
        }
    }

    private suspend fun failure(error: AppError): BiometricLoginResult {
        if (error != AppError.InvalidDevice) return BiometricLoginResult.Failure(error)
        vault.clear()
        return BiometricLoginResult.Disabled(BiometricDisabledReason.DEVICE_REVOKED)
    }
}
