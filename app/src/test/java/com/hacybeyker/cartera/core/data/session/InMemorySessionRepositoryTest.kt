package com.hacybeyker.cartera.core.data.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InMemorySessionRepositoryTest {
    private val tokens = InMemoryAccessTokenStore()
    private val session = InMemorySessionRepository(tokens)

    @Test
    fun `start keeps the token in memory`() {
        session.start("jwt")

        assertEquals("jwt", tokens.accessToken())
        assertFalse(session.isExpired.value)
    }

    @Test
    fun `expiry drops the token and stays expired until acknowledged`() {
        session.start("jwt")

        session.onSessionExpired()

        assertNull(tokens.accessToken())
        assertTrue(session.isExpired.value)
        session.acknowledgeExpiry()
        assertFalse(session.isExpired.value)
    }

    @Test
    fun `a new login clears a pending expiry`() {
        session.onSessionExpired()

        session.start("new")

        assertFalse(session.isExpired.value)
    }

    @Test
    fun `end drops the token without showing the expiry`() {
        session.start("jwt")

        session.end()

        assertNull(tokens.accessToken())
        assertFalse(session.isExpired.value)
    }
}
