package com.hacybeyker.cartera.feature.login.domain.model

import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.domain.entry.EntryStep

sealed interface BiometricLoginResult {
    data class LoggedIn(val next: EntryStep) : BiometricLoginResult

    /** Se cerró el diálogo de huella: no pasó nada. */
    data object Cancelled : BiometricLoginResult

    /** El ingreso con huella dejó de servir y se borró lo guardado. */
    data class Disabled(val reason: BiometricDisabledReason) : BiometricLoginResult

    data class Failure(val error: AppError) : BiometricLoginResult
}

enum class BiometricDisabledReason {
    /** El servidor ya no reconoce el dispositivo (`INVALID_DEVICE`). */
    DEVICE_REVOKED,

    /** La clave quedó invalidada: se registró una huella nueva. */
    KEY_INVALIDATED
}
