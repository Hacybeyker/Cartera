package com.hacybeyker.cartera.feature.login.data.repository

import com.hacybeyker.cartera.core.data.network.ApiCaller
import com.hacybeyker.cartera.core.data.network.CarteraApi
import com.hacybeyker.cartera.core.data.network.dto.LoginRequestDto
import com.hacybeyker.cartera.core.data.network.toOutcome
import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.feature.login.domain.model.AuthSession
import com.hacybeyker.cartera.feature.login.domain.model.Credentials
import com.hacybeyker.cartera.feature.login.domain.repository.AuthRepository
import javax.inject.Inject

class RemoteAuthRepository @Inject constructor(private val api: CarteraApi, private val call: ApiCaller) :
    AuthRepository {

    override suspend fun logIn(credentials: Credentials): Outcome<AuthSession> =
        call { api.login(LoginRequestDto(credentials.username, credentials.password)) }
            .toOutcome { AuthSession(accessToken = it.accessToken, expiresInSeconds = it.expiresIn) }
}
