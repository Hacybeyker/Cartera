package com.hacybeyker.cartera.feature.home.data

import com.hacybeyker.cartera.core.data.network.ApiCaller
import com.hacybeyker.cartera.core.data.network.CarteraHttpClient
import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.core.testing.carteraApi
import com.hacybeyker.cartera.core.testing.enqueueJson
import com.hacybeyker.cartera.core.testing.problemJson
import com.hacybeyker.cartera.feature.home.TestProfile
import com.hacybeyker.cartera.feature.home.data.repository.RemoteProfileRepository
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class RemoteProfileRepositoryTest {
    private val server = MockWebServer()
    private var sessionExpiries = 0
    private lateinit var repository: RemoteProfileRepository

    @Before
    fun setUp() {
        server.start()
        repository =
            RemoteProfileRepository(server.carteraApi(), ApiCaller(CarteraHttpClient.json) { sessionExpiries++ })
    }

    @After
    fun tearDown() = server.close()

    @Test
    fun `reads the profile`() = runTest {
        server.enqueueJson(
            200,
            """{"id":"USR-001","fullName":"Ana Torres","documentMasked":"*****678","email":"ana.torres@cartera.invalid"}"""
        )

        assertEquals(Outcome.Success(TestProfile), repository.me())
        assertEquals("/api/v1/me", server.takeRequest().url.encodedPath)
    }

    @Test
    fun `an expired session fails and notifies the expiry`() = runTest {
        server.enqueueJson(401, problemJson("SESSION_EXPIRED", 401), "application/problem+json")

        assertEquals(Outcome.Failure(AppError.SessionExpired), repository.me())
        assertEquals(1, sessionExpiries)
    }
}
