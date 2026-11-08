package com.hacybeyker.cartera.core.data.market

import com.hacybeyker.cartera.core.data.network.AccessTokenUpdates
import com.hacybeyker.cartera.core.data.network.SessionExpiryListener
import com.hacybeyker.cartera.core.domain.market.FxQuote
import com.hacybeyker.cartera.core.domain.market.MarketStream
import com.hacybeyker.cartera.core.domain.market.MarketStreamStatus
import com.hacybeyker.cartera.core.domain.market.Quote
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.WebSocket

/**
 * Cliente del canal en vivo (`/market/stream`): una sola conexión para toda la app, con suscripciones por símbolo y
 * por par contadas por referencias.
 *
 * - Se abre con el primer suscriptor y se cierra [GRACE_MILLIS] después de que se va el último: pasar de una pantalla
 *   con precios a otra, o rotar, no reabre el socket.
 * - Si la conexión se cae o no se puede abrir, reconecta con espera creciente y vuelve a suscribir todo, porque el
 *   servidor no recuerda nada. Los precios quedan en su último valor mientras tanto.
 * - Un 401 en el handshake o un cierre 4401 terminan la sesión: avisa a [SessionExpiryListener] y no reconecta. Un
 *   cambio de token (cierre de sesión, expiración, otro login) cierra la conexión del anterior.
 *
 * El estado se toca solo dentro de [scope], que no ejecuta en paralelo; los callbacks de OkHttp se pasan a ese scope.
 */
@Singleton
class WebSocketMarketStream @Inject constructor(
    sockets: WebSocket.Factory,
    @MarketStreamUrl url: HttpUrl,
    json: Json,
    tokens: AccessTokenUpdates,
    sessionExpiry: SessionExpiryListener,
    @MarketStreamScope private val scope: CoroutineScope,
    reconnectWait: ReconnectWait
) : MarketStream {

    private val messages = MarketStreamMessages(json)
    private val symbols = RefCounts()
    private val pairs = RefCounts()
    private val quoteValues = ConcurrentHashMap<String, MutableStateFlow<Quote?>>()
    private val fxValues = ConcurrentHashMap<String, MutableStateFlow<FxQuote?>>()
    private var graceJob: Job? = null

    private val connection: MarketStreamConnection = MarketStreamConnection(
        sockets = sockets,
        url = url,
        tokens = tokens,
        sessionExpiry = sessionExpiry,
        scope = scope,
        reconnectWait = reconnectWait,
        events = MarketStreamConnection.Events(
            isWanted = { !symbols.isEmpty || !pairs.isEmpty },
            onConnected = { connection.send(messages.subscription(true, symbols.keys, pairs.keys)) },
            onText = ::deliver
        )
    )

    override val status: StateFlow<MarketStreamStatus> = connection.status

    override fun quotes(symbols: Set<String>): Flow<Quote> {
        if (symbols.isEmpty()) return emptyFlow()
        val values = symbols.map { quoteValues.getOrPut(it) { MutableStateFlow(null) }.filterNotNull() }
        return subscription(symbols = symbols, pairs = emptySet(), values = values.merge())
    }

    override fun fxQuotes(pair: String): Flow<FxQuote> {
        val values = fxValues.getOrPut(pair) { MutableStateFlow(null) }.filterNotNull()
        return subscription(symbols = emptySet(), pairs = setOf(pair), values = values)
    }

    private fun <T> subscription(symbols: Set<String>, pairs: Set<String>, values: Flow<T>): Flow<T> = flow {
        scope.launch { acquire(symbols, pairs) }
        try {
            emitAll(values)
        } finally {
            scope.launch { release(symbols, pairs) }
        }
    }

    private fun acquire(symbols: Set<String>, pairs: Set<String>) {
        graceJob?.cancel()
        graceJob = null
        val newSymbols = this.symbols.add(symbols)
        val newPairs = this.pairs.add(pairs)
        // Abriéndose o esperando para reconectar no se envía nada: al abrir se suscribe todo.
        if (connection.isOpen) {
            connection.send(
                messages.subscription(true, newSymbols, newPairs)
            )
        } else {
            connection.start()
        }
    }

    private fun release(symbols: Set<String>, pairs: Set<String>) {
        val goneSymbols = this.symbols.remove(symbols)
        val gonePairs = this.pairs.remove(pairs)
        // Sin suscriptores, el último valor queda viejo: el próximo que llegue espera el actual del servidor.
        goneSymbols.forEach { quoteValues[it]?.value = null }
        gonePairs.forEach { fxValues[it]?.value = null }
        if (connection.isOpen) connection.send(messages.subscription(false, goneSymbols, gonePairs))
        if (this.symbols.isEmpty && this.pairs.isEmpty) {
            graceJob = scope.launch {
                delay(GRACE_MILLIS)
                graceJob = null
                connection.stop()
            }
        }
    }

    private fun deliver(text: String) {
        when (val message = messages.parse(text)) {
            is StreamMessage.Price -> message.quote.let { if (it.symbol in symbols) quoteValues[it.symbol]?.value = it }
            is StreamMessage.Fx -> message.quote.let { if (it.pair in pairs) fxValues[it.pair]?.value = it }
            null -> Unit
        }
    }

    companion object {
        const val GRACE_MILLIS = 1_000L
    }
}
