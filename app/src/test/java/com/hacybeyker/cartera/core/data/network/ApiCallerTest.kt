package com.hacybeyker.cartera.core.data.network

import com.hacybeyker.cartera.core.data.network.dto.FieldErrorDto
import com.hacybeyker.cartera.core.data.network.dto.LoginRequestDto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.SocketEffect
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

class ApiCallerTest {
    private val server = MockWebServer()
    private var sessionExpiries = 0
    private val caller = ApiCaller(CarteraHttpClient.json) { sessionExpiries++ }
    private lateinit var api: CarteraApi

    @Before
    fun setUp() {
        server.start()
        val client = CarteraHttpClient.okHttpClient(timeoutSeconds = 1, interceptors = emptyList())
        api = CarteraHttpClient.retrofit(server.url("/api/v1/").toString(), client).create(CarteraApi::class.java)
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun `success returns the decoded body`() = runTest {
        enqueue(200, """{"minSupportedVersion":"0.1.0","termsVersion":"2026-11-01","extra":true}""")

        val result = caller { api.appConfig() }

        assertEquals("2026-11-01", (result as ApiResult.Success).value.termsVersion)
    }

    @Test
    fun `rfc 9457 problem maps to its code, detail and trace`() = runTest {
        enqueue(401, problem("INVALID_CREDENTIALS", 401), "application/problem+json")

        val result = caller { api.me() }

        assertEquals(
            ApiResult.Failure(ApiError.Problem(401, ProblemCode.INVALID_CREDENTIALS, "detalle", "req-1", emptyList())),
            result
        )
    }

    @Test
    fun `validation problem keeps the field errors`() = runTest {
        val body = problem("VALIDATION_ERROR", 422)
            .replace("}", ""","errors":[{"field":"amount.value","code":"MUST_BE_POSITIVE","message":"m"}]}""")
        enqueue(422, body, "application/problem+json")

        val error = (caller { api.accounts() } as ApiResult.Failure).error as ApiError.Problem

        assertEquals(listOf(FieldErrorDto("amount.value", "MUST_BE_POSITIVE", "m")), error.fieldErrors)
    }

    @Test
    fun `session expired and unauthorized notify the session expiry`() = runTest {
        enqueue(401, problem("SESSION_EXPIRED", 401), "application/problem+json")
        enqueue(401, problem("UNAUTHORIZED", 401), "application/problem+json")

        caller { api.me() }
        caller { api.accounts() }

        assertEquals(2, sessionExpiries)
    }

    @Test
    fun `401 without a problem body notifies the session expiry`() = runTest {
        enqueue(401, "")

        val result = caller { api.me() }

        assertEquals(ApiResult.Failure(ApiError.Http(401)), result)
        assertEquals(1, sessionExpiries)
    }

    @Test
    fun `invalid credentials and other failures do not notify the session expiry`() = runTest {
        enqueue(401, problem("INVALID_CREDENTIALS", 401), "application/problem+json")
        enqueue(500, problem("INTERNAL_ERROR", 500), "application/problem+json")

        caller { api.login(LoginRequestDto("ana.torres", "x")) }
        caller { api.accounts() }

        assertEquals(0, sessionExpiries)
    }

    @Test
    fun `unknown problem code is kept as unknown`() = runTest {
        enqueue(409, problem("CONFLICT", 409), "application/problem+json")

        val error = (caller { api.accounts() } as ApiResult.Failure).error as ApiError.Problem

        assertEquals(ProblemCode.UNKNOWN, error.code)
    }

    @Test
    fun `http error without a problem body maps to http status`() = runTest {
        enqueue(502, "<html>Bad gateway</html>", "text/html")
        enqueue(404, "")

        assertEquals(ApiResult.Failure(ApiError.Http(502)), caller { api.accounts() })
        assertEquals(ApiResult.Failure(ApiError.Http(404)), caller { api.accounts() })
    }

    @Test
    fun `malformed success body is an invalid response`() = runTest {
        enqueue(200, """{"items":"nope"}""")

        assertEquals(ApiResult.Failure(ApiError.InvalidResponse), caller { api.accounts() })
    }

    @Test
    fun `stalled response is a timeout`() = runTest {
        server.enqueue(MockResponse.Builder().onResponseStart(SocketEffect.Stall).build())

        assertEquals(ApiResult.Failure(ApiError.Timeout), caller { api.accounts() })
    }

    @Test
    fun `closed socket is a network error`() = runTest {
        server.enqueue(MockResponse.Builder().onResponseStart(SocketEffect.CloseSocket()).build())

        assertEquals(ApiResult.Failure(ApiError.Network), caller { api.accounts() })
    }

    @Test
    fun `cancellation is not swallowed`() {
        assertThrows(CancellationException::class.java) {
            runTest { caller<Unit> { throw CancellationException("cancelada") } }
        }
    }

    private fun enqueue(code: Int, body: String, contentType: String = "application/json") {
        server.enqueue(MockResponse.Builder().code(code).addHeader("Content-Type", contentType).body(body).build())
    }

    private fun problem(code: String, status: Int) =
        """{"type":"https://cartera.invalid/problems/x","title":"t","status":$status,""" +
            """"detail":"detalle","code":"$code","traceId":"req-1"}"""
}
