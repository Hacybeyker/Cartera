package com.hacybeyker.cartera.core.data.market

import com.hacybeyker.cartera.core.data.network.AccessTokenUpdates
import com.hacybeyker.cartera.core.data.network.CarteraHttpClient
import com.hacybeyker.cartera.core.domain.market.MarketStreamStatus
import java.io.EOFException
import java.net.ProtocolException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WebSocketMarketStreamTest {
    private val factory = FakeWebSocketFactory()
    private val tokens = FakeAccessTokens("token-1")
    private val waits = mutableListOf<Long>()
    private var expiries = 0

    private fun TestScope.stream() = WebSocketMarketStream(
        sockets = factory,
        url = STREAM_URL,
        json = CarteraHttpClient.json,
        tokens = tokens,
        sessionExpiry = { expiries++ },
        scope = backgroundScope,
        reconnectWait = { waits += it }
    ).also { runCurrent() }

    @Test
    fun `one connection serves every subscription`() = runTest {
        val stream = stream()
        collect(stream.quotes(setOf("ANDM")))
        collect(stream.quotes(setOf("ANDM", "QORI")))
        assertEquals(MarketStreamStatus.CONNECTING, stream.status.value)

        factory.last.open()
        runCurrent()
        collect(stream.fxQuotes("USD/PEN"))

        assertEquals(1, factory.sockets.size)
        assertEquals(STREAM_URL, factory.last.request.url)
        assertEquals(MarketStreamStatus.CONNECTED, stream.status.value)
        assertEquals(
            listOf(sent("subscribe", listOf("ANDM", "QORI")), sent("subscribe", pairs = listOf("USD/PEN"))),
            factory.last.sentMessages()
        )
    }

    @Test
    fun `a symbol is unsubscribed only when its last subscriber leaves`() = runTest {
        val stream = stream()
        val first = collect(stream.quotes(setOf("ANDM")))
        val second = collect(stream.quotes(setOf("ANDM")))
        factory.last.open()
        runCurrent()

        first.job.cancel()
        runCurrent()
        assertEquals(listOf(sent("subscribe", listOf("ANDM"))), factory.last.sentMessages())

        factory.last.message(quoteJson("ANDM", "42.20"))
        runCurrent()
        assertEquals("42.20", second.values.last().last.toPlainString())

        second.job.cancel()
        runCurrent()
        assertEquals(sent("unsubscribe", listOf("ANDM")), factory.last.sentMessages().last())
    }

    @Test
    fun `each subscriber gets the prices of its symbols and the last one when it arrives`() = runTest {
        val stream = stream()
        val andm = collect(stream.quotes(setOf("ANDM")))
        factory.last.open()
        factory.last.message(quoteJson("ANDM", "42.10"))
        factory.last.message(quoteJson("QORI", "115.20"))
        runCurrent()

        val late = collect(stream.quotes(setOf("ANDM")))

        assertEquals(listOf("ANDM"), andm.values.map { it.symbol })
        assertEquals("42.10", late.values.single().last.toPlainString())
    }

    @Test
    fun `fx quotes reach the subscribers of the pair`() = runTest {
        val stream = stream()
        val fx = collect(stream.fxQuotes("USD/PEN"))
        factory.last.open()
        factory.last.message(FX_JSON)
        runCurrent()

        assertEquals("3.742", fx.values.single().buy.toPlainString())
        assertEquals("QTE-000012", fx.values.single().quoteId)
    }

    @Test
    fun `after a drop it reconnects, subscribes everything again and keeps the last price`() = runTest {
        val stream = stream()
        val quotes = collect(stream.quotes(setOf("ANDM", "QORI")))
        val first = factory.last
        first.open()
        first.message(quoteJson("ANDM", "42.10"))
        runCurrent()

        first.fail(EOFException())
        runCurrent()

        assertEquals(listOf(1_000L), waits)
        assertEquals(2, factory.sockets.size)
        assertEquals(MarketStreamStatus.RECONNECTING, stream.status.value)
        assertEquals(1, quotes.values.size)

        factory.last.open()
        runCurrent()

        assertEquals(MarketStreamStatus.CONNECTED, stream.status.value)
        assertEquals(listOf(sent("subscribe", listOf("ANDM", "QORI"))), factory.last.sentMessages())
    }

    @Test
    fun `the wait grows 1, 2, 4 and 8 seconds and stays at 8`() = runTest {
        val stream = stream()
        collect(stream.quotes(setOf("ANDM")))

        repeat(5) {
            factory.last.fail(ProtocolException("503"), code = 503)
            runCurrent()
        }

        assertEquals(listOf(1_000L, 2_000L, 4_000L, 8_000L, 8_000L), waits)
        assertEquals(6, factory.sockets.size)
        assertEquals(MarketStreamStatus.RECONNECTING, stream.status.value)
    }

    @Test
    fun `a successful connection restarts the wait`() = runTest {
        val stream = stream()
        collect(stream.quotes(setOf("ANDM")))
        factory.last.fail(EOFException())
        runCurrent()
        factory.last.fail(EOFException())
        runCurrent()

        factory.last.open()
        runCurrent()
        factory.last.serverClose(1001)
        runCurrent()

        assertEquals(listOf(1_000L, 2_000L, 1_000L), waits)
    }

    @Test
    fun `a 401 handshake ends the session without reconnecting`() = runTest {
        val stream = stream()
        collect(stream.quotes(setOf("ANDM")))

        factory.last.fail(ProtocolException("401"), code = 401)
        runCurrent()
        advanceTimeBy(60_000)

        assertEquals(1, expiries)
        assertEquals(1, factory.sockets.size)
        assertTrue(waits.isEmpty())
        assertEquals(MarketStreamStatus.IDLE, stream.status.value)
    }

    @Test
    fun `a 4401 close is answered, ends the session and does not reconnect`() = runTest {
        val stream = stream()
        collect(stream.quotes(setOf("ANDM")))
        factory.last.open()
        runCurrent()

        factory.last.serverClose(4401)
        runCurrent()
        advanceTimeBy(60_000)

        assertEquals(1000, factory.last.closedWith)
        assertEquals(1, expiries)
        assertEquals(1, factory.sockets.size)
        assertEquals(MarketStreamStatus.IDLE, stream.status.value)
    }

    @Test
    fun `a new login opens the channel again for the screens still subscribed`() = runTest {
        val stream = stream()
        collect(stream.quotes(setOf("ANDM")))
        factory.last.fail(ProtocolException("401"), code = 401)
        runCurrent()

        tokens.accessTokens.value = "token-2"
        runCurrent()

        assertEquals(2, factory.sockets.size)
        assertEquals(MarketStreamStatus.CONNECTING, stream.status.value)
    }

    @Test
    fun `logging out closes the channel`() = runTest {
        val stream = stream()
        collect(stream.quotes(setOf("ANDM")))
        factory.last.open()
        runCurrent()

        tokens.accessTokens.value = null
        runCurrent()
        advanceTimeBy(60_000)

        assertEquals(1000, factory.last.closedWith)
        assertEquals(1, factory.sockets.size)
        assertEquals(MarketStreamStatus.IDLE, stream.status.value)
    }

    @Test
    fun `without a session it does not connect`() = runTest {
        tokens.accessTokens.value = null
        val stream = stream()

        collect(stream.quotes(setOf("ANDM")))

        assertTrue(factory.sockets.isEmpty())
        assertEquals(MarketStreamStatus.IDLE, stream.status.value)
    }

    @Test
    fun `it closes a grace period after the last subscriber leaves`() = runTest {
        val stream = stream()
        val first = collect(stream.quotes(setOf("ANDM")))
        factory.last.open()
        runCurrent()

        first.job.cancel()
        advanceTimeBy(WebSocketMarketStream.GRACE_MILLIS - 1)
        runCurrent()
        assertNull(factory.last.closedWith)
        collect(stream.quotes(setOf("QORI")))
        advanceTimeBy(WebSocketMarketStream.GRACE_MILLIS * 2)
        assertEquals(1, factory.sockets.size)
        assertNull(factory.last.closedWith)
    }

    @Test
    fun `without subscribers the connection is closed and the old prices forgotten`() = runTest {
        val stream = stream()
        val first = collect(stream.quotes(setOf("ANDM")))
        factory.last.open()
        factory.last.message(quoteJson("ANDM", "42.10"))
        runCurrent()

        first.job.cancel()
        advanceTimeBy(WebSocketMarketStream.GRACE_MILLIS + 1)

        assertEquals(1000, factory.last.closedWith)
        assertEquals(MarketStreamStatus.IDLE, stream.status.value)
        val again = collect(stream.quotes(setOf("ANDM")))
        assertTrue(again.values.isEmpty())
        assertEquals(2, factory.sockets.size)
    }

    @Test
    fun `a drop with nobody subscribed does not reconnect`() = runTest {
        val stream = stream()
        val first = collect(stream.quotes(setOf("ANDM")))
        factory.last.open()
        runCurrent()
        first.job.cancel()
        runCurrent()

        factory.last.fail(EOFException())
        runCurrent()

        assertTrue(waits.isEmpty())
        assertEquals(MarketStreamStatus.IDLE, stream.status.value)
    }

    @Test
    fun `invalid messages are ignored without closing`() = runTest {
        val stream = stream()
        val quotes = collect(stream.quotes(setOf("ANDM")))
        val socket = factory.last
        socket.open()
        listOf(
            "no es json",
            "[1, 2]",
            """{"type":"otro"}""",
            """{"type":"quote","symbol":"ANDM"}""",
            quoteJson("ANDM", "cuarenta"),
            quoteJson("ANDM", "42.10").replace("2026-11-07T10:00:02-05:00", "ayer"),
            FX_JSON
        ).forEach(socket::message)
        socket.message(quoteJson("ANDM", "42.30"))
        runCurrent()

        assertEquals(listOf("42.30"), quotes.values.map { it.last.toPlainString() })
        assertNull(socket.closedWith)
        assertEquals(MarketStreamStatus.CONNECTED, stream.status.value)
    }

    @Test
    fun `callbacks of a replaced connection are ignored`() = runTest {
        val stream = stream()
        val quotes = collect(stream.quotes(setOf("ANDM")))
        val old = factory.last
        old.open()
        runCurrent()
        old.fail(EOFException())
        runCurrent()

        old.message(quoteJson("ANDM", "1.00"))
        old.open()
        runCurrent()

        assertTrue(quotes.values.isEmpty())
        assertEquals(MarketStreamStatus.RECONNECTING, stream.status.value)
    }

    @Test
    fun `an empty set of symbols needs no connection`() = runTest {
        val stream = stream()

        collect(stream.quotes(emptySet()))

        assertTrue(factory.sockets.isEmpty())
    }

    private class Collected<T>(val job: Job, val values: List<T>)

    private fun <T> TestScope.collect(flow: Flow<T>): Collected<T> {
        val values = mutableListOf<T>()
        val job = backgroundScope.launch { flow.collect { values += it } }
        runCurrent()
        return Collected(job, values)
    }

    private fun sent(type: String, symbols: List<String> = emptyList(), pairs: List<String> = emptyList()) =
        Sent(type, symbols, pairs)

    private data class Sent(val type: String, val symbols: List<String>, val pairs: List<String>)

    private fun FakeWebSocket.sentMessages(): List<Sent> = sent.map { text ->
        val json = Json.parseToJsonElement(text).jsonObject
        Sent(json.string("type"), json.strings("symbols"), json.strings("pairs"))
    }

    private fun JsonObject.string(key: String) = getValue(key).jsonPrimitive.content

    private fun JsonObject.strings(key: String) = getValue(key).jsonArray.map { it.jsonPrimitive.content }

    private companion object {
        val STREAM_URL = "http://127.0.0.1:8765/api/v1/market/stream".toHttpUrl()

        const val FX_JSON = """{"type":"fx","pair":"USD/PEN","buy":"3.742","sell":"3.768","quoteId":"QTE-000012",""" +
            """"expiresAt":"2026-11-07T10:00:32-05:00","ts":"2026-11-07T10:00:02-05:00"}"""

        fun quoteJson(symbol: String, last: String) =
            """{"type":"quote","symbol":"$symbol","name":"Nombre","currency":"USD","last":"$last",""" +
                """"change":"0.34","changePct":"0.81","ts":"2026-11-07T10:00:02-05:00"}"""
    }
}

private class FakeAccessTokens(token: String?) : AccessTokenUpdates {
    override val accessTokens: MutableStateFlow<String?> = MutableStateFlow(token)
}

class FakeWebSocketFactory : WebSocket.Factory {
    val sockets = mutableListOf<FakeWebSocket>()
    val last: FakeWebSocket get() = sockets.last()

    override fun newWebSocket(request: Request, listener: WebSocketListener): WebSocket =
        FakeWebSocket(request, listener).also { sockets += it }
}

/** Un WebSocket sin red: el test dispara los callbacks que daría OkHttp y lee lo que el cliente envió. */
class FakeWebSocket(val request: Request, private val listener: WebSocketListener) : WebSocket {
    val sent = mutableListOf<String>()
    var closedWith: Int? = null

    override fun request(): Request = request

    override fun queueSize(): Long = 0

    override fun send(text: String): Boolean {
        sent += text
        return closedWith == null
    }

    override fun send(bytes: ByteString): Boolean = false

    override fun close(code: Int, reason: String?): Boolean {
        if (closedWith != null) return false
        closedWith = code
        return true
    }

    override fun cancel() = Unit

    fun open() = listener.onOpen(this, response(SWITCHING_PROTOCOLS))

    fun message(text: String) = listener.onMessage(this, text)

    fun fail(error: Throwable, code: Int? = null) = listener.onFailure(this, error, code?.let(::response))

    fun serverClose(code: Int) = listener.onClosing(this, code, "")

    private fun response(code: Int): Response = Response.Builder()
        .request(request)
        .protocol(Protocol.HTTP_1_1)
        .code(code)
        .message("")
        .build()

    private companion object {
        const val SWITCHING_PROTOCOLS = 101
    }
}
