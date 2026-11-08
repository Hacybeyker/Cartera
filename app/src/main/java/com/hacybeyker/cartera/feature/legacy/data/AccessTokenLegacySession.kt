package com.hacybeyker.cartera.feature.legacy.data

import com.hacybeyker.cartera.core.data.network.AccessTokenProvider
import com.hacybeyker.cartera.legacy.integration.LegacySession
import javax.inject.Inject

class AccessTokenLegacySession @Inject constructor(private val tokens: AccessTokenProvider) : LegacySession {
    override fun accessToken(): String? = tokens.accessToken()
}
