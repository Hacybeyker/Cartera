package com.hacybeyker.cartera.feature.login

import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.feature.login.domain.model.AuthSession
import com.hacybeyker.cartera.feature.login.domain.model.Credentials
import com.hacybeyker.cartera.feature.login.domain.repository.AuthRepository

class FakeAuthRepository(var error: AppError? = null) : AuthRepository {
    val attempts = mutableListOf<Credentials>()

    override suspend fun logIn(credentials: Credentials): Outcome<AuthSession> {
        attempts += credentials
        return error?.let { Outcome.Failure(it) } ?: Outcome.Success(AuthSession("jwt-${attempts.size}", 900))
    }
}
