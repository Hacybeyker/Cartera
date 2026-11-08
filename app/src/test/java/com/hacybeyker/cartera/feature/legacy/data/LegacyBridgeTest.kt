package com.hacybeyker.cartera.feature.legacy.data

import com.hacybeyker.cartera.BuildConfig
import com.hacybeyker.cartera.core.data.network.BearerInterceptor
import com.hacybeyker.cartera.core.data.network.CarteraHttpClient
import com.hacybeyker.cartera.core.data.pinpad.PinPadSeedSource
import com.hacybeyker.cartera.core.data.pinpad.SourcedPinPadSeed
import com.hacybeyker.cartera.core.data.session.InMemoryAccessTokenStore
import com.hacybeyker.cartera.core.data.session.InMemorySessionRepository
import com.hacybeyker.cartera.core.ui.marks.VisibleContentMarker
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.Request
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
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

    @Test
    fun `the module gets the http client of the app, with its interceptors`() {
        val client = CarteraHttpClient.okHttpClient(10, listOf(BearerInterceptor(tokens)))
        val legacyClient = OkHttpLegacyHttpClient(client)
        sessionRepository.start("jwt")

        assertSame(client, legacyClient.callFactory())
        MockWebServer().use { server ->
            server.start()
            server.enqueue(MockResponse.Builder().code(204).build())
            legacyClient.callFactory().newCall(Request.Builder().url(server.url("/api/v1/billers")).build())
                .execute().close()

            assertEquals("Bearer jwt", server.takeRequest().headers["Authorization"])
        }
    }

    @Test
    fun `the keyboard seed of the module is the one of the app, and fixed without a source`() {
        assertEquals(0, PinPadSeedLegacySeed(SourcedPinPadSeed(emptySet())).current())

        var seed: Int? = 7
        val legacy = PinPadSeedLegacySeed(SourcedPinPadSeed(setOf(PinPadSeedSource { seed })))
        assertEquals(7, legacy.current())
        seed = 12
        assertEquals(12, legacy.current())
    }
}
