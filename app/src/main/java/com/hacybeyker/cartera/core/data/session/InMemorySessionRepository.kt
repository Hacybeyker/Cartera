package com.hacybeyker.cartera.core.data.session

import com.hacybeyker.cartera.core.data.network.SessionExpiryListener
import com.hacybeyker.cartera.core.domain.session.SessionRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class InMemorySessionRepository @Inject constructor(private val tokens: InMemoryAccessTokenStore) :
    SessionRepository,
    SessionExpiryListener {

    private val expired = MutableStateFlow(false)

    override val isExpired: StateFlow<Boolean> = expired.asStateFlow()

    override fun start(accessToken: String) {
        tokens.update(accessToken)
        expired.value = false
    }

    override fun end() {
        tokens.clear()
    }

    override fun acknowledgeExpiry() {
        expired.value = false
    }

    override fun onSessionExpired() {
        tokens.clear()
        expired.value = true
    }
}
