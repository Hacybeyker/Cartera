package com.hacybeyker.cartera.demo.scenario

import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class DemoScenarioInterceptorTest {
    private val server = MockWebServer()
    private val storage = FakeScenarioStorage()
    private val client = OkHttpClient.Builder().addInterceptor(DemoScenarioInterceptor(ScenarioStore(storage))).build()

    @Before
    fun setUp() {
        server.start()
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun `each endpoint gets its own latency and failure rate`() {
        ScenarioStore(storage).update(
            mapOf(
                "latency" to "default=400,movements=2000",
                "failure_rate" to "transfers=100",
                "failure_mode" to "disconnect"
            )
        )

        val movements = send("GET", "movements")
        val transfers = send("POST", "transfers")

        assertEquals("2000", movements.headers["X-Demo-Latency-Ms"])
        assertEquals("0", movements.headers["X-Demo-Failure-Rate"])
        assertEquals("disconnect", movements.headers["X-Demo-Failure-Mode"])
        assertEquals("400", transfers.headers["X-Demo-Latency-Ms"])
        assertEquals("100", transfers.headers["X-Demo-Failure-Rate"])
    }

    @Test
    fun `session ttl travels only with the login`() {
        ScenarioStore(storage).update(mapOf("session_ttl" to 60))

        assertEquals("60", send("POST", "auth/login").headers["X-Demo-Session-Ttl"])
        assertNull(send("GET", "me").headers["X-Demo-Session-Ttl"])
    }

    @Test
    fun `biometric login also carries the session ttl`() {
        ScenarioStore(storage).update(mapOf("session_ttl" to 60))

        assertEquals("60", send("POST", "auth/biometric/login").headers["X-Demo-Session-Ttl"])
        assertNull(send("POST", "auth/biometric/enroll").headers["X-Demo-Session-Ttl"])
    }

    @Test
    fun `quote ttl travels with the rates and the stream handshake`() {
        ScenarioStore(storage).update(mapOf("quote_ttl_s" to 5, "tick_ms" to 0, "stream_drop_s" to 15))

        val rates = send("GET", "fx/rates")
        val stream = send("GET", "market/stream")
        val exchange = send("POST", "fx/exchanges")

        assertEquals("5", rates.headers["X-Demo-Quote-Ttl-S"])
        assertNull(rates.headers["X-Demo-Tick-Ms"])
        assertEquals("0", stream.headers["X-Demo-Tick-Ms"])
        assertEquals("15", stream.headers["X-Demo-Stream-Drop-S"])
        assertEquals("5", stream.headers["X-Demo-Quote-Ttl-S"])
        assertNull(exchange.headers["X-Demo-Quote-Ttl-S"])
        assertEquals("400", stream.headers["X-Demo-Latency-Ms"])
    }

    @Test
    fun `stream handshake gets the default market values`() {
        val stream = send("GET", "market/stream")

        assertEquals("2000", stream.headers["X-Demo-Tick-Ms"])
        assertEquals("0", stream.headers["X-Demo-Stream-Drop-S"])
        assertEquals("30", stream.headers["X-Demo-Quote-Ttl-S"])
    }

    @Test
    fun `backend down is sent only when on`() {
        assertNull(send("GET", "accounts").headers["X-Demo-Backend-Down"])

        ScenarioStore(storage).update(mapOf("backend_down" to true))

        assertEquals("true", send("GET", "accounts").headers["X-Demo-Backend-Down"])
    }

    @Test
    fun `demo reset and paths outside the api are left untouched`() {
        assertNull(send("POST", "_demo/reset").headers["X-Demo-Latency-Ms"])
        assertNull(send("GET", "../../health").headers["X-Demo-Latency-Ms"])
    }

    private fun send(method: String, path: String): RecordedRequest {
        server.enqueue(MockResponse.Builder().code(204).build())
        val body = if (method == "POST") ByteArray(0).toRequestBody() else null
        val request = Request.Builder().url(server.url("/api/v1/").resolve(path)!!).method(method, body).build()
        client.newCall(request).execute().close()
        return server.takeRequest()
    }
}
