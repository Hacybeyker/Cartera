package com.hacybeyker.cartera.core.domain.session

import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    /** `true` desde que una llamada autenticada da 401 hasta que el usuario lo confirma. */
    val isExpired: Flow<Boolean>

    fun start(accessToken: String)

    fun end()

    fun acknowledgeExpiry()
}
