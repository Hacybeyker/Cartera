package com.hacybeyker.cartera.feature.login.domain.repository

import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.feature.login.domain.model.AuthSession
import com.hacybeyker.cartera.feature.login.domain.model.Credentials

interface AuthRepository {
    suspend fun logIn(credentials: Credentials): Outcome<AuthSession>

    /** Ingreso con huella: cambia el `deviceToken` del dispositivo por una sesión, igual que [logIn]. */
    suspend fun logInWithDevice(deviceToken: String): Outcome<AuthSession>
}
