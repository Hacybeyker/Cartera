package com.hacybeyker.cartera.feature.login.data.repository

import com.hacybeyker.cartera.core.data.network.ApiCaller
import com.hacybeyker.cartera.core.data.network.BiometricApi
import com.hacybeyker.cartera.core.data.network.CarteraApi
import com.hacybeyker.cartera.core.data.network.dto.BiometricLoginRequestDto
import com.hacybeyker.cartera.core.data.network.dto.LoginRequestDto
import com.hacybeyker.cartera.core.data.network.dto.LoginResponseDto
import com.hacybeyker.cartera.core.data.network.toOutcome
import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.feature.login.domain.model.AuthSession
import com.hacybeyker.cartera.feature.login.domain.model.Credentials
import com.hacybeyker.cartera.feature.login.domain.repository.AuthRepository
import javax.inject.Inject

class RemoteAuthRepository @Inject constructor(
    private val api: CarteraApi,
    private val biometricApi: BiometricApi,
    private val call: ApiCaller
) : AuthRepository {

    override suspend fun logIn(credentials: Credentials): Outcome<AuthSession> =
        call { api.login(LoginRequestDto(credentials.username, credentials.password)) }
            .toOutcome { it.toAuthSession() }

    override suspend fun logInWithDevice(deviceToken: String): Outcome<AuthSession> =
        call { biometricApi.login(BiometricLoginRequestDto(deviceToken)) }.toOutcome { it.toAuthSession() }

    private fun LoginResponseDto.toAuthSession() = AuthSession(accessToken = accessToken, expiresInSeconds = expiresIn)
}
