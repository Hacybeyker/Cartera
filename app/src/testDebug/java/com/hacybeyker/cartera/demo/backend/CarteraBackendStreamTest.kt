package com.hacybeyker.cartera.demo.backend

import java.net.InetAddress
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CarteraBackendStreamTest {
    private val logs = CopyOnWriteArrayList<Pair<String, String>>()
    private val backend = CarteraBackend(
        fixtures = fileFixtures,
        clock = MutableClock(),
        log = { logs += Thread.currentThread().name to it }
    )
    private val client = OkHttpClient.Builder()
        .retryOnConnectionFailure(false)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    @Before
    fun setUp() {
        backend.start(InetAddress.getLoopbackAddress(), port = 0)
    }

    @After
    fun tearDown() {
        client.dispatcher.executorService.shutdown()
        backend.close()
    }

    @Test
    fun `handshake without a token answers 401`() {
        val events = connect(token = null)

        val failure = events.next() as Event.Failure
        assertEquals(401, failure.code)
        assertTrue(failure.body.contains("UNAUTHORIZED"))
    }

    @Test
    fun `handshake with the backend down answers 503`() {
        val events = connect(headers = mapOf("X-Demo-Backend-Down" to "true"))

        assertEquals(503, (events.next() as Event.Failure).code)
    }

    @Test
    fun `subscribe gets the current value and then ticks of what was subscribed`() {
        val events = connect(headers = mapOf("X-Demo-Tick-Ms" to "50"))
        assertTrue(events.next() is Event.Open)

        events.socket.send("""{"type":"subscribe","symbols":["ANDM"],"pairs":["USD/PEN"]}""")

        val initial = List(2) { (events.next() as Event.Message).json }
        assertEquals("42.10", initial[0].string("last"))
        assertEquals("fx", initial[1].string("type"))
        val ticks = List(6) { (events.next() as Event.Message).json }
        assertEquals(setOf("quote", "fx"), ticks.map { it.string("type") }.toSet())
        assertTrue(ticks.filter { it.string("type") == "quote" }.all { it.string("symbol") == "ANDM" })
        assertTrue(logs.filter { it.second.startsWith("WS") }.all { it.first == CarteraBackend.WORKER_THREAD })
    }

    @Test
    fun `stream_drop_s cuts the connection as a failure, not a close`() {
        val events = connect(headers = mapOf("X-Demo-Stream-Drop-S" to "1", "X-Demo-Tick-Ms" to "0"))
        assertTrue(events.next() is Event.Open)

        val failure = events.next()

        assertTrue("$failure", failure is Event.Failure && failure.code == null)
        awaitLog("WS#1 cerrado code=1006 por=servidor")
        assertTrue(logs.any { it.second == "WS#1 corte a los 1 s" })
    }

    @Test
    fun `an expired session closes the channel with 4401`() {
        val events = connect(token = login(sessionTtl = 1), headers = mapOf("X-Demo-Tick-Ms" to "0"))
        assertTrue(events.next() is Event.Open)

        val closing = events.next() as Event.Closing

        assertEquals(4401, closing.code)
        assertEquals("La sesión expiró.", closing.reason)
        events.socket.close(1000, null)
        awaitLog("WS#1 cerrado code=4401 por=servidor")
    }

    @Test
    fun `a client close is answered and logged once`() {
        val events = connect()
        assertTrue(events.next() is Event.Open)

        events.socket.close(1000, "fin")

        assertEquals(1000, (events.next() as Event.Closing).code)
        awaitLog("WS#1 cerrado code=1000 por=cliente abiertas=0 motivo=fin")
        assertTrue(logs.any { it.second.startsWith("GET /api/v1/market/stream 101 WS#1 ") })
        assertEquals(1, logs.count { it.second.startsWith("WS#1 abierto ") })
    }

    private sealed interface Event {
        data object Open : Event
        data class Message(val json: JsonObject) : Event
        data class Closing(val code: Int, val reason: String) : Event
        data class Failure(val code: Int?, val body: String) : Event
    }

    private class Events(val socket: WebSocket, private val queue: LinkedBlockingQueue<Event>) {
        fun next(): Event = requireNotNull(queue.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS)) { "Sin eventos del canal" }
    }

    private fun connect(token: String? = login(), headers: Map<String, String> = emptyMap()): Events {
        val queue = LinkedBlockingQueue<Event>()
        val request = Request.Builder().url(backend.baseUrl.resolve("market/stream")!!).apply {
            token?.let { header("Authorization", "Bearer $it") }
            headers.forEach { (name, value) -> header(name, value) }
        }.build()
        val socket = client.newWebSocket(
            request,
            object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    queue += Event.Open
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    queue += Event.Message(Json.parseToJsonElement(text).jsonObject)
                }

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    queue += Event.Closing(code, reason)
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    queue += Event.Failure(response?.code, response?.body?.string().orEmpty())
                }
            }
        )
        return Events(socket, queue)
    }

    private fun login(sessionTtl: Int? = null): String {
        val request = Request.Builder()
            .url(backend.baseUrl.resolve("auth/login")!!)
            .apply { sessionTtl?.let { header("X-Demo-Session-Ttl", it.toString()) } }
            .post(
                """{"username":"ana.torres","password":"Cartera2026"}""".toRequestBody("application/json".toMediaType())
            )
            .build()
        return client.newCall(request).execute().use { response ->
            Json.parseToJsonElement(response.body.string()).jsonObject.string("accessToken")
        }
    }

    private fun awaitLog(prefix: String) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(TIMEOUT_SECONDS)
        while (logs.none { it.second.startsWith(prefix) }) {
            check(System.nanoTime() < deadline) { "No llegó el log $prefix" }
            Thread.sleep(POLL_MILLIS)
        }
    }

    private companion object {
        const val TIMEOUT_SECONDS = 5L
        const val POLL_MILLIS = 10L
    }
}
