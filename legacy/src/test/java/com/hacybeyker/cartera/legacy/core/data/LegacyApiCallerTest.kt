package com.hacybeyker.cartera.legacy.core.data

import com.hacybeyker.cartera.legacy.billers.data.BillersApi
import com.hacybeyker.cartera.legacy.core.di.LegacyNetworkModule
import com.hacybeyker.cartera.legacy.core.domain.FieldError
import com.hacybeyker.cartera.legacy.core.domain.LegacyError
import com.hacybeyker.cartera.legacy.core.domain.LegacyException
import com.hacybeyker.cartera.legacy.core.domain.ProblemCode
import com.hacybeyker.cartera.legacy.integration.LegacyApiConfig
import com.hacybeyker.cartera.legacy.integration.LegacyHttpClient
import com.hacybeyker.cartera.legacy.testing.RecordingSessionExpiry
import io.reactivex.rxjava3.core.Single
import java.util.concurrent.TimeUnit
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.SocketEffect
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class LegacyApiCallerTest {
    private val server = MockWebServer()
    private val expiry = RecordingSessionExpiry()
    private val caller = LegacyApiCaller(LegacyJson.instance, expiry)
    private lateinit var api: BillersApi

    @Before
    fun setUp() {
        server.start()
        val client = OkHttpClient.Builder().readTimeout(300, TimeUnit.MILLISECONDS).build()
        val retrofit = LegacyNetworkModule.provideRetrofit(
            LegacyHttpClient { client },
            object : LegacyApiConfig {
                override val baseUrl: String = server.url("/api/v1/").toString()
            }
        )
        api = LegacyNetworkModule.provideBillersApi(retrofit)
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun `a success passes through untouched`() {
        enqueue(200, """{"items":[]}""")

        call().assertValue { it.items.isEmpty() }
        assertEquals(0, expiry.calls)
    }

    @Test
    fun `an rfc 9457 problem keeps its status, code, detail and field errors`() {
        val body = problem("VALIDATION_ERROR", 422)
            .replace("}", ""","errors":[{"field":"customerCode","code":"INVALID_FORMAT","message":"m"}]}""")
        enqueue(422, body)

        call().assertError { failure ->
            (failure as LegacyException).error ==
                LegacyError.Rejected(
                    status = 422,
                    code = ProblemCode.VALIDATION_ERROR,
                    detail = "detalle",
                    fieldErrors = listOf(FieldError("customerCode", "INVALID_FORMAT", "m"))
                )
        }
    }

    @Test
    fun `a not found problem keeps its code`() {
        enqueue(404, problem("CUSTOMER_NOT_FOUND", 404))

        call().assertError {
            ((it as LegacyException).error as LegacyError.Rejected).code == ProblemCode.CUSTOMER_NOT_FOUND
        }
    }

    @Test
    fun `an unknown problem code is kept as unknown`() {
        enqueue(409, problem("CONFLICT", 409))

        call().assertError { ((it as LegacyException).error as LegacyError.Rejected).code == ProblemCode.UNKNOWN }
    }

    @Test
    fun `a 401 with or without a problem body reports the session expiry once each`() {
        enqueue(401, problem("SESSION_EXPIRED", 401))
        enqueue(401, "")

        call().assertError { (it as LegacyException).error == LegacyError.SessionExpired }
        assertEquals(1, expiry.calls)
        call().assertError { (it as LegacyException).error == LegacyError.SessionExpired }
        assertEquals(2, expiry.calls)
    }

    @Test
    fun `other failures do not report the session expiry`() {
        enqueue(500, problem("INTERNAL_ERROR", 500))
        enqueue(404, problem("CUSTOMER_NOT_FOUND", 404))

        call()
        call()

        assertEquals(0, expiry.calls)
    }

    @Test
    fun `a 5xx is a server error even with a problem body`() {
        enqueue(503, problem("SERVICE_UNAVAILABLE", 503))
        enqueue(502, "<html>Bad gateway</html>")

        call().assertError { (it as LegacyException).error == LegacyError.Server }
        call().assertError { (it as LegacyException).error == LegacyError.Server }
    }

    @Test
    fun `a 4xx without a problem body is a server error`() {
        enqueue(404, "")
        enqueue(400, "<html>nope</html>")
        enqueue(400, "{}")

        repeat(3) { call().assertError { (it as LegacyException).error == LegacyError.Server } }
    }

    @Test
    fun `a closed socket or a stalled response is a network error`() {
        server.enqueue(MockResponse.Builder().onResponseStart(SocketEffect.CloseSocket()).build())
        server.enqueue(MockResponse.Builder().onResponseStart(SocketEffect.Stall).build())

        call().assertError { (it as LegacyException).error == LegacyError.Network }
        call().assertError { (it as LegacyException).error == LegacyError.Network }
    }

    @Test
    fun `a malformed success body is a server error`() {
        enqueue(200, """{"items":"nope"}""")

        call().assertError { (it as LegacyException).error == LegacyError.Server }
    }

    @Test
    fun `malformed numbers and dates while mapping are a server error`() {
        enqueue(200, """{"items":[]}""")

        api.billers("").map { "dos".toBigDecimal() }.compose(caller.translated())
            .test().assertError { (it as LegacyException).error == LegacyError.Server }
        Single.fromCallable { java.time.LocalDate.parse("ayer") }.compose(caller.translated())
            .test().assertError { (it as LegacyException).error == LegacyError.Server }
    }

    @Test
    fun `a programming error is not hidden as a service error`() {
        val programmingError = IllegalStateException("programming error")

        Single.error<String>(programmingError).compose(caller.translated()).test().assertError(programmingError)
        assertEquals(0, expiry.calls)
    }

    @Test
    fun `an error that is already translated stays as it is`() {
        val translated = LegacyException(LegacyError.Network)

        Single.error<String>(translated).compose(caller.translated())
            .test().assertError { (it as LegacyException).error == LegacyError.Network }
    }

    private fun call() = api.billers("").compose(caller.translated()).test()

    private fun enqueue(code: Int, body: String) {
        server.enqueue(
            MockResponse.Builder().code(code).addHeader("Content-Type", "application/json").body(body).build()
        )
    }

    private fun problem(code: String, status: Int) =
        """{"type":"https://cartera.invalid/problems/x","title":"t","status":$status,""" +
            """"detail":"detalle","code":"$code","traceId":"req-1"}"""
}
