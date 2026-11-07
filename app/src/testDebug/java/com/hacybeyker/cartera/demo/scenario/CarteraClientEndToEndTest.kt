package com.hacybeyker.cartera.demo.scenario

import com.hacybeyker.cartera.core.data.network.ApiCaller
import com.hacybeyker.cartera.core.data.network.ApiError
import com.hacybeyker.cartera.core.data.network.ApiResult
import com.hacybeyker.cartera.core.data.network.BearerInterceptor
import com.hacybeyker.cartera.core.data.network.CarteraApi
import com.hacybeyker.cartera.core.data.network.CarteraHttpClient
import com.hacybeyker.cartera.core.data.network.ProblemCode
import com.hacybeyker.cartera.core.data.network.dto.LoginRequestDto
import com.hacybeyker.cartera.core.data.network.dto.MoneyDto
import com.hacybeyker.cartera.core.data.network.dto.TransferRequestDto
import com.hacybeyker.cartera.core.data.session.InMemoryAccessTokenStore
import com.hacybeyker.cartera.demo.backend.CarteraBackend
import com.hacybeyker.cartera.demo.backend.MutableClock
import com.hacybeyker.cartera.demo.backend.fileFixtures
import java.net.InetAddress
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class CarteraClientEndToEndTest {
    private val clock = MutableClock()
    private val backend = CarteraBackend(fileFixtures, clock)
    private val storage = FakeScenarioStorage()
    private val scenario = ScenarioStore(storage)
    private val tokens = InMemoryAccessTokenStore()
    private val caller = ApiCaller(CarteraHttpClient.json)
    private lateinit var client: OkHttpClient
    private lateinit var api: CarteraApi

    @Before
    fun setUp() {
        backend.start(InetAddress.getLoopbackAddress(), port = 0)
        scenario.update(mapOf("latency" to "0"))
        client = CarteraHttpClient.okHttpClient(
            timeoutSeconds = 1,
            interceptors = listOf(BearerInterceptor(tokens), DemoScenarioInterceptor(scenario))
        )
        api = CarteraHttpClient.retrofit(backend.baseUrl.toString(), client).create(CarteraApi::class.java)
    }

    @After
    fun tearDown() {
        backend.close()
    }

    @Test
    fun `the whole read flow works against the embedded backend`() = runTest {
        assertEquals("2026-11-01", success { api.appConfig() }.termsVersion)
        login()

        assertEquals("Ana Torres", success { api.me() }.fullName)
        assertEquals(3, success { api.accounts() }.items.size)
        assertEquals("Cuenta Sueldo", success { api.account("ACC-001") }.alias)
        val page = success { api.movements("ACC-001", cursor = null, limit = 20) }
        val next = success { api.movements("ACC-001", cursor = page.nextCursor, limit = 20) }
        assertEquals(20, next.items.size)
        assertEquals("MOV-001-0021", next.items.first().id)
        assertEquals("POS", success { api.movement("MOV-002-0001") }.channel)
        assertEquals(40, success { api.breakdown("MOV-002-0001") }.lines.size)
    }

    @Test
    fun `transfer succeeds once per idempotency key`() = runTest {
        login()
        val body = TransferRequestDto("ACC-002", "999-0000001-0-01", MoneyDto("150.00", "PEN"))

        val first = success { api.transfer("key-1", body) }
        val again = success { api.transfer("key-1", body) }

        assertEquals(first, again)
        assertEquals("2150.00", success { api.account("ACC-002") }.availableBalance.value)
    }

    @Test
    fun `business errors arrive as typed problems`() = runTest {
        assertProblem(ProblemCode.INVALID_CREDENTIALS) { api.login(LoginRequestDto("ana.torres", "mal")) }
        assertProblem(ProblemCode.UNAUTHORIZED) { api.me() }
        login()
        assertProblem(ProblemCode.CURRENCY_MISMATCH) {
            api.transfer("k", TransferRequestDto("ACC-002", "999-0000003-0-10", MoneyDto("1.00", "PEN")))
        }
        assertProblem(ProblemCode.NOT_FOUND) { api.movement("MOV-404") }
    }

    @Test
    fun `session ttl from the scenario makes the session expire`() = runTest {
        scenario.update(mapOf("session_ttl" to 60))
        login()

        clock.now = clock.now.plusSeconds(60)

        assertProblem(ProblemCode.SESSION_EXPIRED) { api.accounts() }
    }

    @Test
    fun `backend down and injected failures map to their errors`() = runTest {
        scenario.update(mapOf("backend_down" to true))
        assertProblem(ProblemCode.SERVICE_UNAVAILABLE) { api.appConfig() }

        scenario.update(mapOf("backend_down" to false, "failure_rate" to "app_config=100"))
        assertProblem(ProblemCode.INTERNAL_ERROR) { api.appConfig() }

        scenario.update(mapOf("failure_mode" to "timeout"))
        assertEquals(ApiResult.Failure(ApiError.Timeout), caller { api.appConfig() })

        scenario.update(mapOf("failure_mode" to "disconnect"))
        assertEquals(ApiResult.Failure(ApiError.Network), caller { api.appConfig() })
    }

    @Test
    fun `reset client restores the fixtures`() = runTest {
        login()
        success { api.transfer("key-1", TransferRequestDto("ACC-002", "999-0000001-0-01", MoneyDto("150.00", "PEN"))) }

        val reset = DemoResetClient(client, backend.baseUrl).reset()

        assertEquals(true, reset)
        assertEquals("2300.00", success { api.account("ACC-002") }.availableBalance.value)
    }

    @Test
    fun `reset client reports an unreachable backend`() {
        val unreachable = backend.baseUrl
        backend.close()

        assertEquals(false, DemoResetClient(client, unreachable).reset())
    }

    @Test
    fun `no bearer is sent after the token is cleared`() = runTest {
        login()
        tokens.clear()

        assertNull(tokens.accessToken())
        assertProblem(ProblemCode.UNAUTHORIZED) { api.accounts() }
    }

    private suspend fun login() {
        tokens.update(success { api.login(LoginRequestDto("ana.torres", "Cartera2026")) }.accessToken)
    }

    private suspend fun <T> success(call: suspend () -> T): T = (caller(call) as ApiResult.Success).value

    private suspend fun assertProblem(code: ProblemCode, call: suspend () -> Any) {
        val error = (caller(call) as ApiResult.Failure).error
        assertEquals(code, (error as ApiError.Problem).code)
    }
}
