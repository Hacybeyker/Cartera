package com.hacybeyker.cartera.legacy.billers

import com.hacybeyker.cartera.legacy.billers.data.RemoteBillerRepository
import com.hacybeyker.cartera.legacy.billers.domain.Biller
import com.hacybeyker.cartera.legacy.core.data.LegacyApiCaller
import com.hacybeyker.cartera.legacy.core.data.LegacyJson
import com.hacybeyker.cartera.legacy.core.di.LegacyNetworkModule
import com.hacybeyker.cartera.legacy.core.domain.LegacyError
import com.hacybeyker.cartera.legacy.core.domain.LegacyException
import com.hacybeyker.cartera.legacy.integration.LegacyApiConfig
import com.hacybeyker.cartera.legacy.integration.LegacyHttpClient
import com.hacybeyker.cartera.legacy.testing.RecordingSessionExpiry
import com.hacybeyker.cartera.legacy.testing.fixture
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class RemoteBillerRepositoryTest {
    private val server = MockWebServer()
    private val expiry = RecordingSessionExpiry()
    private lateinit var repository: RemoteBillerRepository

    @Before
    fun setUp() {
        server.start()
        val retrofit = LegacyNetworkModule.provideRetrofit(
            LegacyHttpClient { OkHttpClient() },
            object : LegacyApiConfig {
                override val baseUrl: String = server.url("/api/v1/").toString()
            }
        )
        repository = RemoteBillerRepository(
            LegacyNetworkModule.provideBillersApi(retrofit),
            LegacyApiCaller(LegacyJson.instance, expiry)
        )
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun `the whole catalog of the fixtures maps to billers in their order`() {
        server.enqueue(json("""{"items":${fixture("billers.json")}}"""))

        val billers = repository.search("").blockingGet()

        assertEquals(20, billers.size)
        assertEquals(Biller("BIL-LUZ", "Luz del Norte", "Electricidad", "^\\d{6}$"), billers.first())
        assertEquals("/api/v1/billers?query=", server.takeRequest().target)
    }

    @Test
    fun `the query travels encoded as a query parameter`() {
        server.enqueue(json("""{"items":[]}"""))

        val billers = repository.search("agua tru").blockingGet()

        assertEquals(emptyList<Biller>(), billers)
        assertEquals("/api/v1/billers?query=agua%20tru", server.takeRequest().target)
    }

    @Test
    fun `a rejected token reports the session expiry and fails with it`() {
        server.enqueue(MockResponse.Builder().code(401).build())

        repository.search("").test().assertError { (it as LegacyException).error == LegacyError.SessionExpired }

        assertEquals(1, expiry.calls)
    }

    private fun json(body: String) =
        MockResponse.Builder().code(200).addHeader("Content-Type", "application/json").body(body).build()
}
