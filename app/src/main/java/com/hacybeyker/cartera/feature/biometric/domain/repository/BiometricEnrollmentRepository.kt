package com.hacybeyker.cartera.feature.biometric.domain.repository

import com.hacybeyker.cartera.core.domain.Outcome

interface BiometricEnrollmentRepository {
    /** Registra el dispositivo y devuelve su `deviceToken`. */
    suspend fun enroll(): Outcome<String>

    /** Revoca todos los dispositivos registrados de la persona. */
    suspend fun revoke(): Outcome<Unit>
}
