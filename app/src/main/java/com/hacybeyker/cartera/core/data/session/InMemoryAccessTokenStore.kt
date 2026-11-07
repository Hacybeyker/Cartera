package com.hacybeyker.cartera.core.data.session

import com.hacybeyker.cartera.core.data.network.AccessTokenProvider
import javax.inject.Inject
import javax.inject.Singleton

// Solo en memoria a propósito: cada arranque en frío pide login y no queda ningún token guardado.
@Singleton
class InMemoryAccessTokenStore @Inject constructor() : AccessTokenProvider {
    @Volatile
    private var token: String? = null

    override fun accessToken(): String? = token

    fun update(accessToken: String) {
        token = accessToken
    }

    fun clear() {
        token = null
    }
}
