package com.hacybeyker.cartera.feature.legacy.data

import com.hacybeyker.cartera.core.data.network.SessionExpiryListener
import com.hacybeyker.cartera.legacy.integration.LegacySessionExpiry
import javax.inject.Inject

class ListenerLegacySessionExpiry @Inject constructor(private val listener: SessionExpiryListener) :
    LegacySessionExpiry {
    override fun onSessionExpired() = listener.onSessionExpired()
}
