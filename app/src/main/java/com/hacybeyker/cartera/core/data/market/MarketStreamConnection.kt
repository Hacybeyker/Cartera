package com.hacybeyker.cartera.core.data.market

import com.hacybeyker.cartera.core.data.network.AccessTokenUpdates
import com.hacybeyker.cartera.core.data.network.SessionExpiryListener
import com.hacybeyker.cartera.core.domain.market.MarketStreamStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.HttpUrl
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

/**
 * El socket del canal en vivo: uno a la vez, con reconexión de espera creciente y fin de sesión. No sabe de
 * suscripciones; se las pide a [events]. Solo se usa desde [scope], que no ejecuta en paralelo.
 */
internal class MarketStreamConnection(
    private val sockets: WebSocket.Factory,
    private val url: HttpUrl,
    private val tokens: AccessTokenUpdates,
    private val sessionExpiry: SessionExpiryListener,
    private val scope: CoroutineScope,
    private val reconnectWait: ReconnectWait,
    private val events: Events
) {
    /** [isWanted]: si alguien sigue suscrito. [onConnected]: hay que volver a suscribir todo. */
    class Events(val isWanted: () -> Boolean, val onConnected: () -> Unit, val onText: (String) -> Unit)

    private val statusFlow = MutableStateFlow(MarketStreamStatus.IDLE)
    val status: StateFlow<MarketStreamStatus> = statusFlow.asStateFlow()

    private var socket: WebSocket? = null
    private var socketToken: String? = null

    // Cada socket nuevo, o cerrado por la app, sube la generación: los callbacks de uno anterior se descartan.
    private var generation = 0
    private var failedAttempts = 0
    private var retryJob: Job? = null

    // El token que el servidor rechazó (401 o 4401): con él no se vuelve a intentar.
    private var rejectedToken: String? = null

    val isOpen: Boolean get() = socket != null && statusFlow.value == MarketStreamStatus.CONNECTED

    init {
        scope.launch { tokens.accessTokens.collect(::onTokenChanged) }
    }

    /** Abre el socket si no hay uno abierto, abriéndose o esperando para reconectar. */
    fun start() {
        if (socket == null && retryJob == null) connect()
    }

    fun stop() {
        cancelRetry()
        closeSocket()
        failedAttempts = 0
        statusFlow.value = MarketStreamStatus.IDLE
    }

    fun send(text: String?) {
        if (text != null) socket?.send(text)
    }

    private fun connect() {
        val token = tokens.accessTokens.value
        if (token == null || token == rejectedToken) {
            statusFlow.value = MarketStreamStatus.IDLE
            return
        }
        socketToken = token
        statusFlow.value = if (failedAttempts == 0) MarketStreamStatus.CONNECTING else MarketStreamStatus.RECONNECTING
        // El token lo pone BearerInterceptor, como en cualquier llamada; los interceptores extra también aplican.
        socket = sockets.newWebSocket(Request.Builder().url(url).build(), Listener(++generation))
    }

    private fun closeSocket() {
        generation++
        socket?.close(NORMAL_CLOSURE, null)
        socket = null
        socketToken = null
    }

    private fun cancelRetry() {
        retryJob?.cancel()
        retryJob = null
    }

    // Cerrar sesión, expirar o entrar con otro usuario cambia el token: el socket del anterior no sirve.
    private fun onTokenChanged(token: String?) {
        val active = socket != null || retryJob != null
        if (!active && !events.isWanted()) return
        if (active && token == socketToken) return
        stop()
        if (events.isWanted()) connect()
    }

    private fun opened() {
        failedAttempts = 0
        statusFlow.value = MarketStreamStatus.CONNECTED
        events.onConnected()
    }

    private fun dropped() {
        closeSocket()
        if (!events.isWanted()) return stop()
        statusFlow.value = MarketStreamStatus.RECONNECTING
        val wait = BACKOFF_MILLIS[failedAttempts.coerceAtMost(BACKOFF_MILLIS.lastIndex)]
        failedAttempts++
        retryJob = scope.launch {
            reconnectWait.await(wait)
            retryJob = null
            connect()
        }
    }

    private fun sessionEnded() {
        rejectedToken = socketToken
        stop()
        sessionExpiry.onSessionExpired()
    }

    private inner class Listener(private val generation: Int) : WebSocketListener() {

        override fun onOpen(webSocket: WebSocket, response: Response) = post { opened() }

        override fun onMessage(webSocket: WebSocket, text: String) = post { events.onText(text) }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            webSocket.close(NORMAL_CLOSURE, null)
            post { if (code == SESSION_EXPIRED_CLOSE) sessionEnded() else dropped() }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) = post {
            if (response?.code == HTTP_UNAUTHORIZED) sessionEnded() else dropped()
        }

        private fun post(action: () -> Unit) {
            scope.launch { if (generation == this@MarketStreamConnection.generation) action() }
        }
    }

    companion object {
        val BACKOFF_MILLIS = listOf(1_000L, 2_000L, 4_000L, 8_000L)
        const val NORMAL_CLOSURE = 1000
        const val SESSION_EXPIRED_CLOSE = 4401
        private const val HTTP_UNAUTHORIZED = 401
    }
}
