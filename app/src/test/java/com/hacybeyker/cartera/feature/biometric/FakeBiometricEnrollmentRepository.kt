package com.hacybeyker.cartera.feature.biometric

import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.feature.biometric.domain.repository.BiometricEnrollmentRepository

class FakeBiometricEnrollmentRepository : BiometricEnrollmentRepository {
    var enrollError: AppError? = null
    var revokeError: AppError? = null
    var enrolls = 0
    var revokes = 0

    override suspend fun enroll(): Outcome<String> {
        enrolls++
        return enrollError?.let { Outcome.Failure(it) } ?: Outcome.Success("dvc_$enrolls")
    }

    override suspend fun revoke(): Outcome<Unit> {
        revokes++
        return revokeError?.let { Outcome.Failure(it) } ?: Outcome.Success(Unit)
    }
}
