package com.hacybeyker.cartera.demo.backend

import java.time.Instant
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Lo que el cliente envía por el canal en vivo (`api/asyncapi.yaml`). */
sealed interface StreamRequest {
    data class Subscription(val subscribe: Boolean, val symbols: List<String>, val pairs: List<String>) :
        StreamRequest

    data class Invalid(val reason: String) : StreamRequest

    companion object {
        const val SUBSCRIBE = "subscribe"
        const val UNSUBSCRIBE = "unsubscribe"
        private val ACTIONS = mapOf(SUBSCRIBE to true, UNSUBSCRIBE to false)

        fun parse(text: String): StreamRequest {
            val message = parseObject(text)
            val subscribe = message?.text("type")?.let(ACTIONS::get)
            val symbols = message?.names("symbols")
            val pairs = message?.names("pairs")
            return when {
                message == null -> Invalid("no es un objeto JSON")
                subscribe == null -> Invalid("type debe ser $SUBSCRIBE o $UNSUBSCRIBE")
                symbols == null || pairs == null -> Invalid("symbols y pairs deben ser listas de textos")
                else -> Subscription(subscribe, symbols, pairs)
            }
        }

        private fun parseObject(text: String): JsonObject? = try {
            Json.parseToJsonElement(text) as? JsonObject
        } catch (_: SerializationException) {
            null
        }

        // Una lista ausente equivale a vacía; cualquier otra forma invalida el mensaje entero.
        private fun JsonObject.names(key: String): List<String>? = when (val element = get(key)) {
            null -> emptyList()
            is JsonArray -> element.map { (it as? JsonPrimitive)?.takeIf(JsonPrimitive::isString)?.content }
                .takeIf { null !in it }
                ?.filterNotNull()
            else -> null
        }
    }
}

/** Los datos del handshake que valen durante toda la conexión. */
data class StreamHandshake(val id: Int, val userId: String, val sessionExpiresAt: Instant, val scenario: DemoScenario)

/** Planificador del canal: cada tarea corre en el hilo del backend, como el resto del estado. */
fun interface StreamScheduler {
    fun schedule(delayMs: Long, task: () -> Unit): Cancellable
}

fun interface Cancellable {
    fun cancel()
}

/** Lo que el canal necesita de una conexión WebSocket abierta. */
interface StreamSocket {
    fun send(text: String)

    fun close(code: Int, reason: String)

    /** Corta el socket sin cierre de WebSocket: el cliente lo ve como una caída de red. */
    fun drop()
}
