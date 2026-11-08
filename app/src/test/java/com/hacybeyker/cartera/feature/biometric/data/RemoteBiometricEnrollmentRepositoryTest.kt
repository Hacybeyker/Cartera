package com.hacybeyker.cartera.feature.biometric.data

import com.hacybeyker.cartera.core.data.network.ApiCaller
import com.hacybeyker.cartera.core.data.network.CarteraHttpClient
import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.core.testing.biometricApi
import com.hacybeyker.cartera.core.testing.enqueueJson
import com.hacybeyker.cartera.core.testing.problemJson
import com.hacybeyker.cartera.feature.biometric.data.repository.RemoteBiometricEnrollmentRepository
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class RemoteBiometricEnrollmentRepositoryTest {
    private val server = MockWebServer()
    private var expired = 0
    private lateinit var repository: RemoteBiometricEnrollmentRepository

    @Before
    fun setUp() {
        server.start()
        repository =
            RemoteBiometricEnrollmentRepository(server.biometricApi(), ApiCaller(CarteraHttpClient.json) { expired++ })
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun `enroll posts without a body and returns the device token`() = runTest {
        server.enqueueJson(201, """{"deviceToken":"dvc_1"}""")

        assertEquals(Outcome.Success("dvc_1"), repository.enroll())

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/auth/biometric/enroll", request.url.encodedPath)
    }

    @Test
    fun `enroll failures map like any call`() = runTest {
        server.enqueueJson(503, problemJson("SERVICE_UNAVAILABLE", 503), "application/problem+json")

        assertEquals(Outcome.Failure(AppError.Server), repository.enroll())
    }

    @Test
    fun `revoke deletes and accepts the empty 204`() = runTest {
        server.enqueue(MockResponse.Builder().code(204).build())

        assertEquals(Outcome.Success(Unit), repository.revoke())

        val request = server.takeRequest()
        assertEquals("DELETE", request.method)
        assertEquals("/api/v1/auth/biometric", request.url.encodedPath)
    }

    @Test
    fun `a revoke with an expired session reports it`() = runTest {
        server.enqueueJson(401, problemJson("SESSION_EXPIRED", 401), "application/problem+json")

        assertEquals(Outcome.Failure(AppError.SessionExpired), repository.revoke())
        assertEquals(1, expired)
    }

    @Test
    fun `a revoke that fails on the server is a server error`() = runTest {
        server.enqueueJson(500, problemJson("INTERNAL_ERROR", 500), "application/problem+json")

        assertEquals(Outcome.Failure(AppError.Server), repository.revoke())
    }
}
