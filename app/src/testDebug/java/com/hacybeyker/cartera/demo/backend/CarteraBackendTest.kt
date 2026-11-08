package com.hacybeyker.cartera.demo.backend

import java.io.IOException
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CarteraBackendTest {
    private val reads = CopyOnWriteArrayList<String>()
    private val logs = CopyOnWriteArrayList<String>()
    private val backend = CarteraBackend(
        fixtures = { path ->
            reads += "${Thread.currentThread().name}:$path"
            fileFixtures.read(path)
        },
        clock = MutableClock(),
        log = { logs += it }
    )
    private val client = OkHttpClient.Builder()
        .retryOnConnectionFailure(false)
        .readTimeout(1, TimeUnit.SECONDS)
        .build()

    @Before
    fun setUp() {
        backend.start(InetAddress.getLoopbackAddress(), port = 0)
    }

    @After
    fun tearDown() {
        backend.close()
    }

    @Test
    fun `fixtures load lazily on the backend worker thread`() {
        assertTrue(reads.isEmpty())

        val response = execute(request("app/config"))

        assertEquals(200, response.first)
        assertEquals(listOf("${CarteraBackend.WORKER_THREAD}:app-config.json"), reads)
    }

    @Test
    fun `every response carries a request id and is logged`() {
        val generated = client.newCall(request("app/config")).execute().use { it.header("X-Request-Id") }
        val echoed = client.newCall(request("app/config").newBuilder().header("X-Request-Id", "abc").build())
            .execute().use { it.header("X-Request-Id") }

        assertEquals("req-000001", generated)
        assertEquals("abc", echoed)
        assertTrue(logs.first().startsWith("GET /api/v1/app/config 200 "))
        assertTrue(logs.first().endsWith("X-Request-Id=req-000001"))
    }

    @Test
    fun `problem responses keep their content type`() {
        client.newCall(request("me")).execute().use { response ->
            assertEquals(401, response.code)
            assertEquals("application/problem+json; charset=utf-8", response.header("Content-Type"))
        }
    }

    @Test
    fun `latency header delays the response`() {
        val startedAt = System.nanoTime()

        execute(request("app/config").newBuilder().header("X-Demo-Latency-Ms", "300").build())

        assertTrue(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt) >= 300)
        assertTrue(logs.single().contains("latency=300ms"))
    }

    @Test
    fun `backend down answers 503 with retry after`() {
        val request = request("app/config").newBuilder().header("X-Demo-Backend-Down", "true").build()

        client.newCall(request).execute().use { response ->
            assertEquals(503, response.code)
            assertEquals("30", response.header("Retry-After"))
            assertTrue(response.body.string().contains("SERVICE_UNAVAILABLE"))
        }
    }

    @Test
    fun `failure mode http 500 answers an internal error`() {
        assertEquals(500, execute(failing("http_500")).first)
    }

    @Test
    fun `failure mode timeout never answers`() {
        assertThrows(SocketTimeoutException::class.java) { execute(failing("timeout")) }
        assertTrue(logs.single().contains(" timeout "))
    }

    @Test
    fun `failure mode disconnect closes the socket`() {
        assertThrows(IOException::class.java) { execute(failing("disconnect")) }
        assertTrue(logs.single().contains(" disconnect "))
    }

    @Test
    fun `demo reset and unknown routes ignore the scenario`() {
        val down = mapOf("X-Demo-Backend-Down" to "true")

        val reset = backend.handle(BackendRequest("POST", "/api/v1/_demo/reset", headers = down))
        val unknown = backend.handle(BackendRequest("GET", "/api/v1/cards", headers = down))

        assertEquals(Outcome.Respond(BackendResponse(status = 204)), reset.outcome)
        assertEquals(404, (unknown.outcome as Outcome.Respond).response.status)
    }

    @Test
    fun `new endpoints follow the same latency and failure rules`() {
        val headers = mapOf("X-Demo-Failure-Rate" to "100", "X-Demo-Latency-Ms" to "250")

        val orders = backend.handle(BackendRequest("POST", "/api/v1/orders", headers = headers))
        val down = backend.handle(
            BackendRequest("GET", "/api/v1/market/stocks", headers = mapOf("X-Demo-Backend-Down" to "true"))
        )

        assertEquals(500, (orders.outcome as Outcome.Respond).response.status)
        assertEquals(250L, orders.latencyMs)
        assertEquals(503, (down.outcome as Outcome.Respond).response.status)
    }

    @Test
    fun `reset makes injected failures repeat from the start`() {
        val headers = mapOf("X-Demo-Failure-Rate" to "50")
        val config = BackendRequest("GET", "/api/v1/app/config", headers = headers)
        val first = List(20) { isOk(backend.handle(config)) }

        backend.handle(BackendRequest("POST", "/api/v1/_demo/reset"))

        val second = List(20) { isOk(backend.handle(config)) }
        assertEquals(first, second)
    }

    @Test
    fun `query parameters and body reach the router`() {
        val login = Request.Builder()
            .url(backend.baseUrl.resolve("auth/login")!!)
            .post(
                """{"username":"ana.torres","password":"Cartera2026"}""".toRequestBody("application/json".toMediaType())
            )
            .build()
        val token = client.newCall(login).execute().use { response ->
            Regex(""""accessToken":"([^"]+)"""").find(response.body.string())!!.groupValues[1]
        }
        val movements = Request.Builder()
            .url(backend.baseUrl.newBuilder().addPathSegment("movements").addQueryParameter("limit", "3").build())
            .header("Authorization", "Bearer $token")
            .build()

        client.newCall(movements).execute().use { response ->
            assertEquals(200, response.code)
            assertEquals(3, Regex(""""id":"MOV-""").findAll(response.body.string()).count())
        }
    }

    @Test
    fun `bill payment endpoints follow the same latency and failure rules`() {
        val headers = mapOf("X-Demo-Failure-Rate" to "100", "X-Demo-Latency-Ms" to "250")

        val payment = backend.handle(BackendRequest("POST", "/api/v1/bill-payments", headers = headers))
        val billers = backend.handle(BackendRequest("GET", "/api/v1/billers", headers = headers))
        val voucher = backend.handle(
            BackendRequest("GET", "/api/v1/bill-payments/PAY-000001/voucher", headers = headers)
        )
        val down = backend.handle(
            BackendRequest(
                "GET",
                "/api/v1/billers/BIL-LUZ/debts",
                headers = mapOf("X-Demo-Backend-Down" to "true")
            )
        )

        listOf(payment, billers, voucher).forEach {
            assertEquals(500, (it.outcome as Outcome.Respond).response.status)
            assertEquals(250L, it.latencyMs)
        }
        assertEquals(503, (down.outcome as Outcome.Respond).response.status)
    }

    @Test
    fun `a payment and its voucher travel over http with the right content types`() {
        val token = login()
        val payBody = billBody()
        val pay = authorized("bill-payments", token).newBuilder()
            .header("Idempotency-Key", "http-1")
            .post(payBody.toRequestBody("application/json".toMediaType()))
            .build()

        val paymentId = client.newCall(pay).execute().use { response ->
            assertEquals(201, response.code)
            assertEquals("application/json; charset=utf-8", response.header("Content-Type"))
            Regex(""""paymentId":"([^"]+)"""").find(response.body.string())!!.groupValues[1]
        }
        client.newCall(authorized("bill-payments/$paymentId/voucher", token)).execute().use { response ->
            val html = response.body.string()
            assertEquals(200, response.code)
            assertEquals("text/html; charset=utf-8", response.header("Content-Type"))
            assertTrue(">S/ 2,122.50<" in html)
            assertTrue(">80000001<" in html)
        }
        client.newCall(authorized("bill-payments/PAY-999999/voucher", token)).execute().use { response ->
            assertEquals(404, response.code)
            assertEquals("application/problem+json; charset=utf-8", response.header("Content-Type"))
        }
        assertTrue(logs.any { it.startsWith("POST /api/v1/bill-payments 201 ") })
        assertTrue(logs.any { it.startsWith("GET /api/v1/bill-payments/$paymentId/voucher 200 ") })
    }

    @Test
    fun `the history is sent only when asked and measures at least 150 kb`() {
        val token = login()
        val debts = authorized("billers/BIL-LUZ/debts", token).let { request ->
            request.newBuilder()
                .url(request.url.newBuilder().addQueryParameter("customerCode", "100200").build())
                .build()
        }
        val withHistory = debts.newBuilder()
            .url(debts.url.newBuilder().addQueryParameter("includeHistory", "true").build())
            .build()

        val plainSize = client.newCall(debts).execute().use { it.body.bytes().size }
        val historySize = client.newCall(withHistory).execute().use { it.body.bytes().size }

        assertTrue(plainSize < 1_000)
        assertTrue(historySize > 150_000)
    }

    private fun login(): String {
        val request = Request.Builder()
            .url(backend.baseUrl.resolve("auth/login")!!)
            .post(
                """{"username":"ana.torres","password":"Cartera2026"}""".toRequestBody("application/json".toMediaType())
            )
            .build()
        return client.newCall(request).execute().use { response ->
            Regex(""""accessToken":"([^"]+)"""").find(response.body.string())!!.groupValues[1]
        }
    }

    private fun authorized(path: String, token: String) =
        request(path).newBuilder().header("Authorization", "Bearer $token").build()

    private fun isOk(handled: Handled) = (handled.outcome as? Outcome.Respond)?.response?.status == 200

    private fun request(path: String) = Request.Builder().url(backend.baseUrl.resolve(path)!!).build()

    private fun failing(mode: String) = request("app/config").newBuilder()
        .header("X-Demo-Failure-Rate", "100")
        .header("X-Demo-Failure-Mode", mode)
        .build()

    private fun execute(request: Request): Pair<Int, String> =
        client.newCall(request).execute().use { it.code to it.body.string() }
}
