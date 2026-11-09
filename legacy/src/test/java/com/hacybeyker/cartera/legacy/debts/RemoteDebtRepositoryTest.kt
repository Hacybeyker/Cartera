package com.hacybeyker.cartera.legacy.debts

import com.hacybeyker.cartera.legacy.core.data.LegacyApiCaller
import com.hacybeyker.cartera.legacy.core.data.LegacyJson
import com.hacybeyker.cartera.legacy.core.di.LegacyNetworkModule
import com.hacybeyker.cartera.legacy.core.domain.LegacyError
import com.hacybeyker.cartera.legacy.core.domain.LegacyException
import com.hacybeyker.cartera.legacy.core.domain.ProblemCode
import com.hacybeyker.cartera.legacy.debts.data.JsonDebtsBodyDecoder
import com.hacybeyker.cartera.legacy.debts.data.RemoteDebtRepository
import com.hacybeyker.cartera.legacy.debts.data.dto.CustomerDebtsDto
import com.hacybeyker.cartera.legacy.integration.LegacyApiConfig
import com.hacybeyker.cartera.legacy.integration.LegacyHttpClient
import com.hacybeyker.cartera.legacy.testing.FakeLegacyFeatureFlags
import com.hacybeyker.cartera.legacy.testing.ImmediateSchedulerProvider
import com.hacybeyker.cartera.legacy.testing.RecordingSessionExpiry
import com.hacybeyker.cartera.legacy.testing.Samples
import com.hacybeyker.cartera.legacy.testing.luzBodyWithHistory
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RemoteDebtRepositoryTest {
    private val server = MockWebServer()
    private val flags = FakeLegacyFeatureFlags()
    private val expiry = RecordingSessionExpiry()
    private lateinit var repository: RemoteDebtRepository

    @Before
    fun setUp() {
        server.start()
        val retrofit = LegacyNetworkModule.provideRetrofit(
            LegacyHttpClient { OkHttpClient() },
            object : LegacyApiConfig {
                override val baseUrl: String = server.url("/api/v1/").toString()
            }
        )
        repository = RemoteDebtRepository(
            LegacyNetworkModule.provideDebtsApi(retrofit),
            LegacyApiCaller(LegacyJson.instance, expiry),
            flags,
            ImmediateSchedulerProvider(),
            JsonDebtsBodyDecoder(LegacyJson.instance)
        )
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun `the pending debts of a customer map to exact amounts, periods and due dates`() {
        server.enqueue(json(DEBTS_BODY))

        val debts = repository.pendingDebts("BIL-LUZ", "100200").blockingGet()

        assertEquals(Samples.luzDebts, debts)
        assertEquals("/api/v1/billers/BIL-LUZ/debts?customerCode=100200", server.takeRequest().target)
    }

    @Test
    fun `a customer without pending debts maps to an empty list`() {
        server.enqueue(json("""{"customerName":"Ana Torres","debts":[]}"""))

        assertEquals(emptyList<Any>(), repository.pendingDebts("BIL-NET", "700800").blockingGet().debts)
    }

    @Test
    fun `an unknown customer fails with its problem code`() {
        server.enqueue(
            MockResponse.Builder().code(404).addHeader("Content-Type", "application/problem+json")
                .body("""{"detail":"No existe","code":"CUSTOMER_NOT_FOUND"}""").build()
        )

        repository.pendingDebts("BIL-LUZ", "999999").test().assertError {
            ((it as LegacyException).error as LegacyError.Rejected).code == ProblemCode.CUSTOMER_NOT_FOUND
        }
    }

    @Test
    fun `an amount or a date the module cannot read is a server error`() {
        server.enqueue(json(DEBTS_BODY.replace("85.40", "ochenta")))
        server.enqueue(json(DEBTS_BODY.replace("2026-11-12", "pronto")))
        server.enqueue(json(DEBTS_BODY.replace("2026-10", "octubre")))

        repeat(3) {
            repository.pendingDebts("BIL-LUZ", "100200").test()
                .assertError { (it as LegacyException).error == LegacyError.Server }
        }
    }

    @Test
    fun `with the flag on the request asks for the history and the same debts come out`() {
        flags.debtsMainDecode = true
        server.enqueue(json(luzBodyWithHistory()))

        val debts = repository.pendingDebts("BIL-LUZ", "100200").blockingGet()

        assertEquals(Samples.luzDebts, debts)
        assertEquals(
            "/api/v1/billers/BIL-LUZ/debts?customerCode=100200&includeHistory=true",
            server.takeRequest().target
        )
    }

    @Test
    fun `with the flag off the request does not ask for the history`() {
        server.enqueue(json(DEBTS_BODY))

        repository.pendingDebts("BIL-LUZ", "100200").test().assertValueCount(1)

        assertEquals(null, server.takeRequest().url.queryParameter("includeHistory"))
    }

    @Test
    fun `the flag is read at every call`() {
        server.enqueue(json(DEBTS_BODY))
        server.enqueue(json(luzBodyWithHistory()))
        server.enqueue(json(DEBTS_BODY))

        repository.pendingDebts("BIL-LUZ", "100200").test().assertValueCount(1)
        flags.debtsMainDecode = true
        repository.pendingDebts("BIL-LUZ", "100200").test().assertValueCount(1)
        flags.debtsMainDecode = false
        repository.pendingDebts("BIL-LUZ", "100200").test().assertValueCount(1)

        assertEquals(
            listOf(null, "true", null),
            List(3) { server.takeRequest().url.queryParameter("includeHistory") }
        )
    }

    @Test
    fun `a body that carries no history gives the same debts with the flag on`() {
        flags.debtsMainDecode = true
        server.enqueue(json(DEBTS_BODY))

        assertEquals(Samples.luzDebts, repository.pendingDebts("BIL-LUZ", "100200").blockingGet())
    }

    @Test
    fun `the history of the body is decoded into the dto`() {
        val dto = LegacyJson.instance.decodeFromString(CustomerDebtsDto.serializer(), luzBodyWithHistory())

        assertEquals(24, dto.history.size)
        assertTrue(dto.history.all { it.lines.isNotEmpty() })
        val plain = LegacyJson.instance.decodeFromString(CustomerDebtsDto.serializer(), DEBTS_BODY)
        assertEquals(emptyList<Any>(), plain.history)
    }

    @Test
    fun `with the flag on an unknown customer, a bad body and a bad amount fail like with it off`() {
        flags.debtsMainDecode = true
        server.enqueue(
            MockResponse.Builder().code(404).addHeader("Content-Type", "application/problem+json")
                .body("""{"detail":"No existe","code":"CUSTOMER_NOT_FOUND"}""").build()
        )
        server.enqueue(json("no es json"))
        server.enqueue(json(DEBTS_BODY.replace("85.40", "ochenta")))

        repository.pendingDebts("BIL-LUZ", "999999").test().assertError {
            ((it as LegacyException).error as LegacyError.Rejected).code == ProblemCode.CUSTOMER_NOT_FOUND
        }
        repeat(2) {
            repository.pendingDebts("BIL-LUZ", "100200").test()
                .assertError { (it as LegacyException).error == LegacyError.Server }
        }
    }

    @Test
    fun `with the flag on a 401 reports the expired session`() {
        flags.debtsMainDecode = true
        server.enqueue(MockResponse.Builder().code(401).build())

        repository.pendingDebts("BIL-LUZ", "100200").test()
            .assertError { (it as LegacyException).error == LegacyError.SessionExpired }
        assertEquals(1, expiry.calls)
    }

    private fun json(body: String) =
        MockResponse.Builder().code(200).addHeader("Content-Type", "application/json").body(body).build()

    private companion object {
        const val DEBTS_BODY =
            """{"customerName":"Ana Torres","debts":[""" +
                """{"id":"DEB-LUZ-202610","period":"2026-10","dueDate":"2026-11-12",""" +
                """"amount":{"value":"85.40","currency":"PEN"}},""" +
                """{"id":"DEB-LUZ-202611","period":"2026-11","dueDate":"2026-12-12",""" +
                """"amount":{"value":"92.10","currency":"PEN"}}]}"""
    }
}
