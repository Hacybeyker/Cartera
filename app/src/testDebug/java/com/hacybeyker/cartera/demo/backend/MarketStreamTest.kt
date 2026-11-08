package com.hacybeyker.cartera.demo.backend

import java.time.Duration
import java.time.OffsetDateTime
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MarketStreamTest {
    private val harness = StreamHarness()
    private val stream = harness.stream
    private val scheduler = harness.scheduler

    @Test
    fun `subscribe answers right away with the current value of what was subscribed`() {
        val (id, socket) = harness.connect()

        harness.subscribe(id, symbols = listOf("ANDM", "QORI"), pairs = listOf("USD/PEN"))

        val sent = socket.take()
        assertEquals(listOf("quote", "quote", "fx"), sent.map { it.string("type") })
        assertEquals(listOf("ANDM" to "42.10", "QORI" to "115.20"), sent.take(2).map { it.symbolAndLast() })
        assertEquals(listOf("3.742", "3.768"), listOf(sent[2].string("buy"), sent[2].string("sell")))
        assertEquals("USD/PEN", sent[2].string("pair"))
        assertTrue(sent[2].string("quoteId").startsWith("QTE-"))
        assertTrue(harness.logs.contains("WS#1 subscribe → ANDM 42.10 QORI 115.20 USD/PEN 3.742/3.768"))
    }

    @Test
    fun `quote messages carry the contract fields`() {
        val (id, socket) = harness.connect()

        harness.subscribe(id, symbols = listOf("ANDM"))

        assertEquals(
            setOf("type", "symbol", "name", "currency", "last", "change", "changePct", "ts"),
            socket.take().single().keys
        )
    }

    @Test
    fun `each tick sends only what the connection subscribed`() {
        val (id, socket) = harness.connect()
        harness.subscribe(id, symbols = listOf("ANDM"))
        socket.take()

        scheduler.advanceBy(2_000)

        val sent = socket.take()
        assertEquals(listOf("ANDM" to "42.08"), sent.map { it.symbolAndLast() })
        assertTrue(harness.logs.contains("WS#1 tick 1 → ANDM 42.08"))
    }

    @Test
    fun `unsubscribe stops the messages of what was removed`() {
        val (id, socket) = harness.connect()
        harness.subscribe(id, symbols = listOf("ANDM", "QORI"), pairs = listOf("USD/PEN"))
        harness.unsubscribe(id, symbols = listOf("ANDM"), pairs = listOf("USD/PEN"))
        socket.take()

        scheduler.advanceBy(2_000)

        assertEquals(listOf("QORI"), socket.take().map { it.string("symbol") })
        assertTrue(harness.logs.contains("WS#1 unsubscribe → ANDM USD/PEN"))
    }

    @Test
    fun `nothing subscribed sends nothing but prices still move`() {
        val (_, socket) = harness.connect()

        scheduler.advanceBy(2_000)

        assertTrue(socket.sent.isEmpty())
        assertEquals("42.08", harness.router.state.market.price("ANDM").toPlainString())
    }

    @Test
    fun `tick_ms 0 freezes prices even with open connections`() {
        val (id, socket) = harness.connect(tickMs = 0)
        harness.subscribe(id, symbols = listOf("ANDM"))
        socket.take()

        scheduler.advanceBy(60_000)

        assertTrue(socket.sent.isEmpty())
        assertEquals("42.10", harness.router.state.market.price("ANDM").toPlainString())
        assertFalse(harness.logs.any { it.startsWith("WS reloj cada") })
    }

    @Test
    fun `two connections share one market clock`() {
        val (first, firstSocket) = harness.connect()
        val (second, secondSocket) = harness.connect()
        harness.subscribe(first, symbols = listOf("ANDM"))
        harness.subscribe(second, symbols = listOf("ANDM"))
        firstSocket.take()
        secondSocket.take()

        scheduler.advanceBy(2_000)

        assertEquals(listOf("ANDM" to "42.08"), firstSocket.take().map { it.symbolAndLast() })
        assertEquals(listOf("ANDM" to "42.08"), secondSocket.take().map { it.symbolAndLast() })
        assertEquals("42.08", harness.router.state.market.price("ANDM").toPlainString())
        assertEquals(1, harness.logs.count { it.startsWith("WS reloj cada") })
    }

    @Test
    fun `the last handshake sets the tick interval`() {
        val (first, socket) = harness.connect(tickMs = 2_000)
        harness.subscribe(first, symbols = listOf("ANDM"))
        harness.connect(tickMs = 500)
        socket.take()

        scheduler.advanceBy(1_000)

        assertEquals(2, socket.take().size)
        assertTrue(harness.logs.contains("WS reloj cada 500 ms"))
    }

    @Test
    fun `a handshake with tick_ms 0 stops the running clock`() {
        val (first, socket) = harness.connect()
        harness.subscribe(first, symbols = listOf("ANDM"))
        harness.connect(tickMs = 0)
        socket.take()

        scheduler.advanceBy(10_000)

        assertTrue(socket.sent.isEmpty())
        assertTrue(harness.logs.contains("WS reloj detenido"))
    }

    @Test
    fun `the clock stops when the last connection closes`() {
        val (id, socket) = harness.connect()

        stream.closedByClient(id, 1000, "bye")
        scheduler.advanceBy(10_000)

        assertEquals(1000 to "", socket.closedWith)
        assertEquals("42.10", harness.router.state.market.price("ANDM").toPlainString())
        assertEquals(0, stream.openConnections)
        assertEquals(0, scheduler.pending)
        assertTrue(harness.logs.contains("WS#1 cerrado code=1000 por=cliente abiertas=0 motivo=bye"))
        assertTrue(harness.logs.contains("WS reloj detenido"))
    }

    @Test
    fun `fx messages use the quote ttl of the handshake`() {
        val (id, socket) = harness.connect(quoteTtl = 5)

        harness.subscribe(id, pairs = listOf("USD/PEN"))
        scheduler.advanceBy(2_000)

        val sent = socket.take()
        assertEquals(2, sent.size)
        sent.forEach { fx ->
            val ttl = Duration.between(
                OffsetDateTime.parse(fx.string("ts")),
                OffsetDateTime.parse(fx.string("expiresAt"))
            )
            assertEquals(Duration.ofSeconds(5), ttl)
        }
        assertEquals("3.742", sent[1].string("buy"))
        assertEquals(sent[1].string("quoteId"), harness.router.state.fxQuotes.keys.last())
    }

    @Test
    fun `an order executes at the price of the last tick`() {
        val (id, socket) = harness.connect()
        harness.subscribe(id, symbols = listOf("ANDM"))

        scheduler.advanceBy(6_000)

        val lastTick = socket.take().last().string("last")
        val order = harness.router.order(harness.login(), orderBody(symbol = "ANDM", quantity = "1")).json()
        assertEquals("42.01", lastTick)
        assertEquals(lastTick, order.obj("executedPrice").string("value"))
    }

    @Test
    fun `stream_drop_s cuts the connection without a close frame`() {
        val (id, socket) = harness.connect(dropSeconds = 15)
        harness.subscribe(id, symbols = listOf("ANDM"))

        scheduler.advanceBy(14_999)
        assertFalse(socket.dropped)
        scheduler.advanceBy(1)

        assertTrue(socket.dropped)
        assertNull(socket.closedWith)
        assertEquals(0, stream.openConnections)
        assertTrue(harness.logs.contains("WS#1 corte a los 15 s"))
        assertTrue(
            harness.logs.contains("WS#1 cerrado code=1006 por=servidor abiertas=0 motivo=corte de demo")
        )
    }

    @Test
    fun `every connection counts its own drop from zero`() {
        val (_, first) = harness.connect(dropSeconds = 10)
        scheduler.advanceBy(6_000)
        val (_, second) = harness.connect(dropSeconds = 10)

        scheduler.advanceBy(4_000)
        assertTrue(first.dropped)
        assertFalse(second.dropped)
        scheduler.advanceBy(6_000)

        assertTrue(second.dropped)
    }

    @Test
    fun `the session of the handshake closes the connection with 4401 when it expires`() {
        val (id, socket) = harness.connect(token = harness.login(sessionTtl = 30))
        harness.subscribe(id, symbols = listOf("ANDM"))

        scheduler.advanceBy(29_999)
        assertNull(socket.closedWith)
        scheduler.advanceBy(1)

        assertEquals(4401 to "La sesión expiró.", socket.closedWith)
        assertEquals(0, stream.openConnections)
        assertTrue(
            harness.logs.contains("WS#1 cerrado code=4401 por=servidor abiertas=0 motivo=La sesión expiró.")
        )
    }

    @Test
    fun `a tick past the session expiry closes with 4401 before sending`() {
        val (id, socket) = harness.connect(token = harness.login(sessionTtl = 60))
        harness.subscribe(id, symbols = listOf("ANDM"))
        socket.take()

        harness.clock.now = harness.clock.now.plusSeconds(60)
        scheduler.advanceBy(2_000)

        assertTrue(socket.sent.isEmpty())
        assertEquals(4401, socket.closedWith?.first)
    }

    @Test
    fun `a 4401 the client never answers ends in a cut`() {
        val (id, socket) = harness.connect(token = harness.login(sessionTtl = 1))

        scheduler.advanceBy(1_000 + MarketStream.CLOSE_GRACE_MS)

        assertEquals(4401, socket.closedWith?.first)
        assertTrue(socket.dropped)
        stream.closed(id)
    }

    @Test
    fun `a 4401 the client answers does not cut the socket`() {
        val (id, socket) = harness.connect(token = harness.login(sessionTtl = 1))
        scheduler.advanceBy(1_000)

        stream.closedByClient(id, 4401, "")
        stream.closed(id)
        scheduler.advanceBy(MarketStream.CLOSE_GRACE_MS)

        assertFalse(socket.dropped)
        assertEquals(1, harness.logs.count { it.startsWith("WS#1 cerrado") })
    }

    @Test
    fun `a network failure closes the connection once`() {
        val (id, _) = harness.connect()

        stream.failed(id, "Connection reset")
        stream.failed(id, "Connection reset")

        assertEquals(listOf("WS#1 cerrado code=1006 por=red abiertas=0 motivo=Connection reset"), closedLogs())
    }

    @Test
    fun `open and closed lines can be counted per connection`() {
        val (first, _) = harness.connect()
        val (second, _) = harness.connect(dropSeconds = 1)
        stream.closedByClient(first, 1001, "")
        scheduler.advanceBy(1_000)

        assertEquals(2, harness.logs.count { Regex("""^WS#\d+ abierto """).containsMatchIn(it) })
        assertEquals(2, closedLogs().size)
        assertTrue(
            harness.logs.contains(
                "WS#$second abierto user=USR-001 tick_ms=2000 stream_drop_s=1 quote_ttl_s=30 abiertas=2"
            )
        )
        assertTrue(closedLogs().first().endsWith("motivo=-"))
    }

    @Test
    fun `invalid messages are ignored with a log`() {
        val (id, socket) = harness.connect()

        listOf(
            "hola",
            "[]",
            """{"type":"ping"}""",
            """{"type":"subscribe","symbols":"ANDM"}""",
            """{"type":"subscribe","symbols":[1]}"""
        ).forEach { stream.receive(id, it) }
        harness.subscribe(id, symbols = listOf("XXXX"), pairs = listOf("EUR/PEN"))
        stream.receiveBinary(id)

        assertTrue(socket.sent.isEmpty())
        assertEquals(6, harness.logs.count { it.startsWith("WS#1 mensaje ignorado") })
        assertTrue(harness.logs.contains("WS#1 subscribe ignorados: XXXX EUR/PEN"))
    }

    @Test
    fun `missing lists count as empty`() {
        val (id, socket) = harness.connect()

        stream.receive(id, """{"type":"subscribe","symbols":["QORI"]}""")

        assertEquals(listOf("QORI"), socket.take().map { it.string("symbol") })
    }

    @Test
    fun `events of unknown connections are ignored`() {
        stream.receive(99, """{"type":"subscribe","symbols":["QORI"]}""")
        stream.receiveBinary(99)
        stream.closedByClient(99, 1000, "")
        stream.failed(99, "x")

        assertTrue(harness.logs.isEmpty())
    }

    @Test
    fun `reset keeps connections open and sends the initial values`() {
        val (id, socket) = harness.connect()
        harness.subscribe(id, symbols = listOf("ANDM"), pairs = listOf("USD/PEN"))
        scheduler.advanceBy(4_000)
        socket.take()

        harness.router.call("POST", "_demo/reset")
        stream.reset()

        val sent = socket.take()
        assertEquals("42.10", sent[0].string("last"))
        assertEquals("3.742", sent[1].string("buy"))
        assertEquals(1, stream.openConnections)
        assertTrue(harness.logs.contains("WS#1 reset → ANDM 42.10 USD/PEN 3.742/3.768"))
    }

    private fun closedLogs(): List<String> = harness.logs.filter { Regex("""^WS#\d+ cerrado """).containsMatchIn(it) }

    private fun JsonObject.symbolAndLast(): Pair<String, String> = string("symbol") to string("last")
}
