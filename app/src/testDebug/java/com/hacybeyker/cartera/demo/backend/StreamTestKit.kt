package com.hacybeyker.cartera.demo.backend

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

/** Planificador de tiempo virtual: [advanceBy] corre las tareas vencidas en orden y mueve el reloj con ellas. */
class FakeScheduler(private val clock: MutableClock) : StreamScheduler {
    private class Task(val dueMs: Long, val order: Int, val action: () -> Unit) {
        var cancelled = false
    }

    private val tasks = mutableListOf<Task>()
    private var nowMs = 0L
    private var scheduled = 0

    val pending: Int get() = tasks.count { !it.cancelled }

    override fun schedule(delayMs: Long, task: () -> Unit): Cancellable {
        val entry = Task(nowMs + delayMs, scheduled++, task)
        tasks += entry
        return Cancellable { entry.cancelled = true }
    }

    fun advanceBy(millis: Long) {
        val target = nowMs + millis
        while (true) {
            val next = tasks.filter { !it.cancelled && it.dueMs <= target }
                .minWithOrNull(compareBy<Task>({ it.dueMs }, { it.order })) ?: break
            tasks.remove(next)
            moveTo(next.dueMs)
            next.action()
        }
        moveTo(target)
        tasks.removeAll { it.cancelled }
    }

    private fun moveTo(millis: Long) {
        clock.now = clock.now.plusMillis(millis - nowMs)
        nowMs = millis
    }
}

class FakeStreamSocket : StreamSocket {
    val sent = mutableListOf<JsonObject>()
    var closedWith: Pair<Int, String>? = null
    var dropped = false

    override fun send(text: String) {
        sent += Json.parseToJsonElement(text).jsonObject
    }

    override fun close(code: Int, reason: String) {
        closedWith = code to reason
    }

    override fun drop() {
        dropped = true
    }

    fun take(): List<JsonObject> = sent.toList().also { sent.clear() }
}

class StreamHarness {
    val router = RouterHarness()
    val clock: MutableClock get() = router.clock
    val scheduler = FakeScheduler(router.clock)
    val logs = mutableListOf<String>()
    val stream = MarketStream(router.state, router.desks, router.clock, scheduler, log = { logs += it })

    fun login(sessionTtl: Int? = null): String {
        val headers = sessionTtl?.let { mapOf(DemoScenario.SESSION_TTL_HEADER to it.toString()) }.orEmpty()
        val response = router.call(
            method = "POST",
            path = "auth/login",
            body = """{"username":"ana.torres","password":"Cartera2026"}""",
            headers = headers
        )
        return response.json().string("accessToken")
    }

    fun connect(
        token: String = login(),
        tickMs: Int? = null,
        dropSeconds: Int? = null,
        quoteTtl: Int? = null
    ): Pair<Int, FakeStreamSocket> {
        val headers = listOfNotNull(
            "Authorization" to "Bearer $token",
            tickMs?.let { DemoScenario.TICK_MS_HEADER to it.toString() },
            dropSeconds?.let { DemoScenario.STREAM_DROP_HEADER to it.toString() },
            quoteTtl?.let { DemoScenario.QUOTE_TTL_HEADER to it.toString() }
        ).toMap()
        val handshake = stream.handshake(BackendRequest("GET", "${Endpoint.BASE_PATH}market/stream", headers = headers))
        val socket = FakeStreamSocket()
        stream.open(handshake, socket)
        return handshake.id to socket
    }

    fun subscribe(id: Int, symbols: List<String> = emptyList(), pairs: List<String> = emptyList()) =
        stream.receive(id, subscription("subscribe", symbols, pairs))

    fun unsubscribe(id: Int, symbols: List<String> = emptyList(), pairs: List<String> = emptyList()) =
        stream.receive(id, subscription("unsubscribe", symbols, pairs))

    private fun subscription(type: String, symbols: List<String>, pairs: List<String>): String =
        """{"type":"$type","symbols":${symbols.toJsonList()},"pairs":${pairs.toJsonList()}}"""

    private fun List<String>.toJsonList(): String = joinToString(",", "[", "]") { "\"$it\"" }
}
