package com.hacybeyker.cartera.core.data.session

import com.hacybeyker.cartera.core.data.network.AccessTokenProvider
import com.hacybeyker.cartera.core.data.network.AccessTokenUpdates
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Solo en memoria a propósito: cada arranque en frío pide login y no queda ningún token guardado.
@Singleton
class InMemoryAccessTokenStore @Inject constructor() :
    AccessTokenProvider,
    AccessTokenUpdates {
    private val token = MutableStateFlow<String?>(null)

    override val accessTokens: StateFlow<String?> = token.asStateFlow()

    override fun accessToken(): String? = token.value

    fun update(accessToken: String) {
        token.value = accessToken
    }

    fun clear() {
        token.value = null
    }
}
