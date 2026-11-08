package com.hacybeyker.cartera.feature.biometric.domain.model

import com.hacybeyker.cartera.core.domain.AppError

sealed interface EnableBiometricResult {
    data object Enabled : EnableBiometricResult

    /** Se cerró el diálogo de huella: no se llamó al servidor ni quedó nada guardado. */
    data object Cancelled : EnableBiometricResult

    /** El dispositivo no tiene una huella registrada, o el Keystore no pudo crear o usar la clave. */
    data object Unavailable : EnableBiometricResult

    /** Falló el registro en el servidor: no quedó nada guardado. */
    data class Failure(val error: AppError) : EnableBiometricResult
}
