package com.hacybeyker.cartera.feature.biometric.data.repository

import com.hacybeyker.cartera.core.data.network.ApiCaller
import com.hacybeyker.cartera.core.data.network.BiometricApi
import com.hacybeyker.cartera.core.data.network.toOutcome
import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.feature.biometric.domain.repository.BiometricEnrollmentRepository
import javax.inject.Inject
import retrofit2.HttpException

class RemoteBiometricEnrollmentRepository @Inject constructor(
    private val api: BiometricApi,
    private val call: ApiCaller
) : BiometricEnrollmentRepository {

    override suspend fun enroll(): Outcome<String> = call { api.enroll().deviceToken }.toOutcome()

    override suspend fun revoke(): Outcome<Unit> = call {
        val response = api.delete()
        if (!response.isSuccessful) throw HttpException(response)
    }.toOutcome()
}
