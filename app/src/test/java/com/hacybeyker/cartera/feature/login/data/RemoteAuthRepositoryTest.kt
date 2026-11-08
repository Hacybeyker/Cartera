package com.hacybeyker.cartera.feature.login.data

import com.hacybeyker.cartera.core.data.network.ApiCaller
import com.hacybeyker.cartera.core.data.network.CarteraHttpClient
import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.core.testing.biometricApi
import com.hacybeyker.cartera.core.testing.carteraApi
import com.hacybeyker.cartera.core.testing.enqueueJson
import com.hacybeyker.cartera.core.testing.problemJson
import com.hacybeyker.cartera.feature.login.data.repository.RemoteAuthRepository
import com.hacybeyker.cartera.feature.login.domain.model.AuthSession
import com.hacybeyker.cartera.feature.login.domain.model.Credentials
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class RemoteAuthRepositoryTest {
    private val server = MockWebServer()
    private lateinit var repository: RemoteAuthRepository

    @Before
    fun setUp() {
        server.start()
        repository =
            RemoteAuthRepository(server.carteraApi(), server.biometricApi(), ApiCaller(CarteraHttpClient.json) {})
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun `login posts the credentials and maps the token`() = runTest {
        server.enqueueJson(200, """{"accessToken":"jwt","tokenType":"Bearer","expiresIn":900}""")

        val outcome = repository.logIn(Credentials("ana.torres", "Cartera2026"))

        assertEquals(Outcome.Success(AuthSession("jwt", 900)), outcome)
        val request = server.takeRequest()
        assertEquals("/api/v1/auth/login", request.url.encodedPath)
        assertEquals("""{"username":"ana.torres","password":"Cartera2026"}""", request.body?.utf8())
    }

    @Test
    fun `biometric login posts the device token and maps the same session`() = runTest {
        server.enqueueJson(200, """{"accessToken":"jwt","tokenType":"Bearer","expiresIn":60}""")

        val outcome = repository.logInWithDevice("dvc_1")

        assertEquals(Outcome.Success(AuthSession("jwt", 60)), outcome)
        val request = server.takeRequest()
        assertEquals("/api/v1/auth/biometric/login", request.url.encodedPath)
        assertEquals("""{"deviceToken":"dvc_1"}""", request.body?.utf8())
    }

    @Test
    fun `an unknown device maps to invalid device`() = runTest {
        server.enqueueJson(401, problemJson("INVALID_DEVICE", 401), "application/problem+json")

        assertEquals(Outcome.Failure(AppError.InvalidDevice), repository.logInWithDevice("dvc_x"))
    }

    @Test
    fun `wrong credentials map to invalid credentials`() = runTest {
        server.enqueueJson(401, problemJson("INVALID_CREDENTIALS", 401), "application/problem+json")

        assertEquals(Outcome.Failure(AppError.InvalidCredentials), repository.logIn(Credentials("ana.torres", "x")))
    }
}
