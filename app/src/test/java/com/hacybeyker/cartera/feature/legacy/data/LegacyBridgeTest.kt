package com.hacybeyker.cartera.feature.legacy.data

import com.hacybeyker.cartera.BuildConfig
import com.hacybeyker.cartera.core.data.session.InMemoryAccessTokenStore
import com.hacybeyker.cartera.core.data.session.InMemorySessionRepository
import com.hacybeyker.cartera.core.ui.marks.VisibleContentMarker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LegacyBridgeTest {
    private val tokens = InMemoryAccessTokenStore()
    private val sessionRepository = InMemorySessionRepository(tokens)

    @Test
    fun `the session exposes the current access token and none after it is cleared`() {
        val session = AccessTokenLegacySession(tokens)
        assertNull(session.accessToken())

        sessionRepository.start("jwt")
        assertEquals("jwt", session.accessToken())

        sessionRepository.end()
        assertNull(session.accessToken())
    }

    @Test
    fun `a session expiry reported by the module opens the global expiry and drops the token`() {
        val expiry = ListenerLegacySessionExpiry(sessionRepository)
        sessionRepository.start("jwt")

        expiry.onSessionExpired()

        assertTrue(sessionRepository.isExpired.value)
        assertNull(tokens.accessToken())
    }

    @Test
    fun `the base url is the one of the app and ends with a slash`() {
        val config = BuildConfigLegacyApiConfig()

        assertEquals(BuildConfig.BASE_URL, config.baseUrl)
        assertTrue(config.baseUrl.endsWith("/"))
    }

    @Test
    fun `a content mark reaches every marker of the app`() {
        val first = mutableListOf<String>()
        val second = mutableListOf<String>()
        val marker = VisibleContentLegacyMarker(
            setOf(VisibleContentMarker { first += it }, VisibleContentMarker { second += it })
        )

        marker.onContentVisible("legacy_empresas")
        marker.onContentVisible("legacy_recibos")

        assertEquals(listOf("legacy_empresas", "legacy_recibos"), first)
        assertEquals(first, second)
    }

    @Test
    fun `a content mark without markers installed does nothing`() {
        val marker = VisibleContentLegacyMarker(emptySet())

        marker.onContentVisible("legacy_constancia")
    }
}
