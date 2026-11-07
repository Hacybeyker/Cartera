package com.hacybeyker.cartera.core.data.config

import com.hacybeyker.cartera.core.data.network.ApiCaller
import com.hacybeyker.cartera.core.data.network.CarteraHttpClient
import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.core.testing.TestAppConfig
import com.hacybeyker.cartera.core.testing.carteraApi
import com.hacybeyker.cartera.core.testing.enqueueJson
import com.hacybeyker.cartera.core.testing.problemJson
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class RemoteAppConfigRepositoryTest {
    private val server = MockWebServer()
    private lateinit var repository: RemoteAppConfigRepository

    @Before
    fun setUp() {
        server.start()
        repository = RemoteAppConfigRepository(server.carteraApi(), ApiCaller(CarteraHttpClient.json) {})
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun `refresh maps the config and config reuses it without calling again`() = runTest {
        server.enqueueJson(200, """{"minSupportedVersion":"0.1.0","termsVersion":"2026-11-01"}""")

        assertEquals(Outcome.Success(TestAppConfig), repository.refresh())
        assertEquals(Outcome.Success(TestAppConfig), repository.config())
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `config fetches when nothing is cached`() = runTest {
        server.enqueueJson(200, """{"minSupportedVersion":"0.1.0","termsVersion":"2026-11-01"}""")

        assertEquals(Outcome.Success(TestAppConfig), repository.config())
        assertEquals("/api/v1/app/config", server.takeRequest().url.encodedPath)
    }

    @Test
    fun `backend down maps to a server error and caches nothing`() = runTest {
        server.enqueueJson(503, problemJson("SERVICE_UNAVAILABLE", 503), "application/problem+json")
        server.enqueueJson(200, """{"minSupportedVersion":"0.1.0","termsVersion":"2026-11-01"}""")

        assertEquals(Outcome.Failure(AppError.Server), repository.refresh())
        assertEquals(Outcome.Success(TestAppConfig), repository.config())
    }
}
