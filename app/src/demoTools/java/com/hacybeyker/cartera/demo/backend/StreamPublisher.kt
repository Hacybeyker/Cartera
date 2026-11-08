package com.hacybeyker.cartera.demo.backend

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Una conexión abierta del canal: su handshake, su socket, lo que tiene suscrito y sus temporizadores. */
class StreamConnection(val handshake: StreamHandshake, val socket: StreamSocket) {
    val symbols = linkedSetOf<String>()
    val pairs = linkedSetOf<String>()
    val timers = mutableListOf<Cancellable>()
    val tag: String get() = "WS#${handshake.id}"
}

/** Suscripciones y mensajes `quote` y `fx` del canal, con el precio vigente del servidor. */
class StreamPublisher(
    private val state: BackendState,
    private val desks: BackendDesks,
    private val log: (String) -> Unit
) {
    fun subscribe(connection: StreamConnection, request: StreamRequest.Subscription) {
        val symbols = request.symbols.filter { state.market.stock(it) != null }
        val pairs = request.pairs.filter { it == FxDesk.PAIR }
        val unknown = (request.symbols - symbols.toSet()) + (request.pairs - pairs.toSet())
        if (unknown.isNotEmpty()) log("${connection.tag} subscribe ignorados: ${unknown.joinToString(" ")}")
        if (symbols.isEmpty() && pairs.isEmpty()) return
        connection.symbols += symbols
        connection.pairs += pairs
        send(connection, "subscribe", symbols, pairs)
    }

    fun unsubscribe(connection: StreamConnection, request: StreamRequest.Subscription) {
        connection.symbols -= request.symbols.toSet()
        connection.pairs -= request.pairs.toSet()
        log("${connection.tag} unsubscribe → ${(request.symbols + request.pairs).joinToString(" ")}")
    }

    /** Envía el valor actual de todo lo suscrito, si hay algo; [event] encabeza la línea del log. */
    fun publish(connection: StreamConnection, event: String) {
        if (connection.symbols.isNotEmpty() || connection.pairs.isNotEmpty()) {
            send(connection, event, connection.symbols.toList(), connection.pairs.toList())
        }
    }

    private fun send(connection: StreamConnection, event: String, symbols: List<String>, pairs: List<String>) {
        val sent = symbols.map { symbol ->
            val quote = desks.market.quoteJson(symbol)
            connection.socket.send(message(QUOTE, quote))
            "$symbol ${quote.text("last")}"
        } + pairs.map { pair ->
            val quote = desks.fx.issue(connection.handshake.userId, connection.handshake.scenario.quoteTtlSeconds)
            connection.socket.send(message(FX, desks.fx.quoteJson(quote)))
            "$pair ${quote.rate.buy.toPlainString()}/${quote.rate.sell.toPlainString()}"
        }
        log("${connection.tag} $event → ${sent.joinToString(" ")}")
    }

    private fun message(type: String, body: JsonObject): String =
        JsonObject(mapOf("type" to JsonPrimitive(type)) + body).toString()

    companion object {
        const val QUOTE = "quote"
        const val FX = "fx"
    }
}
