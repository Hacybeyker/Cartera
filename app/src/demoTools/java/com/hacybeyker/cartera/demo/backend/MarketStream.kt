package com.hacybeyker.cartera.demo.backend

import java.time.Clock
import java.time.Duration

/**
 * Canal en vivo `/market/stream`: conexiones, sus suscripciones y cómo terminan. En cada tick del [MarketClock]
 * avanzan todas las acciones y el tipo de cambio en [MarketState], así REST, órdenes y portafolio ven el mismo
 * precio que el canal, y cada conexión recibe solo lo que tiene suscrito. Todo se llama desde el hilo del backend.
 */
class MarketStream(
    private val state: BackendState,
    private val desks: BackendDesks,
    private val clock: Clock,
    private val scheduler: StreamScheduler,
    private val log: (String) -> Unit = {}
) {
    private enum class Ending { SESSION_EXPIRED, DROPPED }

    private val connections = linkedMapOf<Int, StreamConnection>()
    private val closing = mutableMapOf<Int, Cancellable>()
    private val publisher = StreamPublisher(state, desks, log)
    private val marketClock = MarketClock(scheduler, log, ::tick)
    private var nextId = 0

    val openConnections: Int get() = connections.size

    /** Lee la sesión del handshake (el router ya la autenticó) y numera la conexión. */
    fun handshake(request: BackendRequest): StreamHandshake {
        val session = desks.sessions.session(request)
        return StreamHandshake(++nextId, session.userId, session.expiresAt, DemoScenario.from(request))
    }

    fun open(handshake: StreamHandshake, socket: StreamSocket) {
        val connection = StreamConnection(handshake, socket)
        val scenario = handshake.scenario
        connections[handshake.id] = connection
        val sessionLeft = Duration.between(clock.instant(), handshake.sessionExpiresAt).toMillis().coerceAtLeast(0)
        connection.timers += scheduler.schedule(sessionLeft) { end(connection, Ending.SESSION_EXPIRED) }
        if (scenario.streamDropSeconds > 0) {
            connection.timers += scheduler.schedule(scenario.streamDropSeconds * MILLIS_PER_SECOND) {
                end(connection, Ending.DROPPED)
            }
        }
        log(
            "${connection.tag} abierto user=${handshake.userId} tick_ms=${scenario.tickMs} " +
                "stream_drop_s=${scenario.streamDropSeconds} quote_ttl_s=${scenario.quoteTtlSeconds} " +
                "abiertas=${connections.size}"
        )
        marketClock.retime(scenario.tickMs)
    }

    fun receive(id: Int, text: String) {
        val connection = connections[id] ?: return
        when (val request = StreamRequest.parse(text)) {
            is StreamRequest.Invalid -> log("${connection.tag} mensaje ignorado: ${request.reason}")
            is StreamRequest.Subscription -> if (request.subscribe) {
                publisher.subscribe(connection, request)
            } else {
                publisher.unsubscribe(connection, request)
            }
        }
    }

    fun receiveBinary(id: Int) {
        connections[id]?.let { log("${it.tag} mensaje ignorado: binario") }
    }

    /** El cliente pidió cerrar: se contesta con 1000 para que el cierre termine limpio. */
    fun closedByClient(id: Int, code: Int, reason: String) {
        val connection = connections.remove(id) ?: return
        connection.socket.close(NORMAL_CLOSURE, "")
        finish(connection, "cerrado code=$code por=cliente", reason)
    }

    /** El cierre terminó en los dos sentidos. */
    fun closed(id: Int) {
        closing.remove(id)?.cancel()
    }

    fun failed(id: Int, error: String) {
        closing.remove(id)?.cancel()
        val connection = connections.remove(id) ?: return
        finish(connection, "cerrado code=$ABNORMAL_CLOSURE por=red", error)
    }

    /** Tras `POST /_demo/reset`: las conexiones siguen abiertas y reciben el valor inicial de lo suscrito. */
    fun reset() {
        connections.values.forEach { publisher.publish(it, "reset") }
    }

    private fun tick(tick: Int) {
        state.market.symbols.forEach(state.market::advance)
        state.market.advanceFx()
        val now = clock.instant()
        connections.values.toList().forEach { connection ->
            if (now.isBefore(connection.handshake.sessionExpiresAt)) {
                publisher.publish(connection, "tick $tick")
            } else {
                end(connection, Ending.SESSION_EXPIRED)
            }
        }
    }

    private fun end(connection: StreamConnection, ending: Ending) {
        val id = connection.handshake.id
        if (connections.remove(id) == null) return
        when (ending) {
            Ending.SESSION_EXPIRED -> {
                connection.socket.close(SESSION_EXPIRED, SESSION_EXPIRED_REASON)
                // Si el cliente no contesta el cierre, el socket se corta: no queda abierto a medias.
                closing[id] = scheduler.schedule(CLOSE_GRACE_MS) {
                    closing.remove(id)
                    connection.socket.drop()
                }
                finish(connection, "cerrado code=$SESSION_EXPIRED por=servidor", SESSION_EXPIRED_REASON)
            }
            Ending.DROPPED -> {
                log("${connection.tag} corte a los ${connection.handshake.scenario.streamDropSeconds} s")
                connection.socket.drop()
                finish(connection, "cerrado code=$ABNORMAL_CLOSURE por=servidor", "corte de demo")
            }
        }
    }

    private fun finish(connection: StreamConnection, closed: String, reason: String) {
        connection.timers.forEach(Cancellable::cancel)
        log("${connection.tag} $closed abiertas=${connections.size} motivo=${reason.ifEmpty { "-" }}")
        if (connections.isEmpty()) marketClock.stop()
    }

    companion object {
        const val NORMAL_CLOSURE = 1000

        /** Código de RFC 6455 para un cierre sin trama de cierre; solo aparece en el log. */
        const val ABNORMAL_CLOSURE = 1006
        const val SESSION_EXPIRED = 4401
        const val SESSION_EXPIRED_REASON = "La sesión expiró."
        const val CLOSE_GRACE_MS = 5_000L
        private const val MILLIS_PER_SECOND = 1_000L
    }
}
