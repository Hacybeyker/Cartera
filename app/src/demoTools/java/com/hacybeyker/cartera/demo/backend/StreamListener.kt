package com.hacybeyker.cartera.demo.backend

import java.net.SocketException
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.internal.ws.RealWebSocket
import okio.ByteString

/** Une el WebSocket de mockwebserver3 con [MarketStream]: cada evento pasa al hilo del backend con [post]. */
class StreamListener(
    private val handshake: StreamHandshake,
    private val stream: MarketStream,
    private val post: (() -> Unit) -> Unit
) : WebSocketListener() {

    override fun onOpen(webSocket: WebSocket, response: Response) =
        post { stream.open(handshake, OkHttpStreamSocket(webSocket)) }

    override fun onMessage(webSocket: WebSocket, text: String) = post { stream.receive(handshake.id, text) }

    override fun onMessage(webSocket: WebSocket, bytes: ByteString) = post { stream.receiveBinary(handshake.id) }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) =
        post { stream.closedByClient(handshake.id, code, reason) }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) = post { stream.closed(handshake.id) }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) =
        post { stream.failed(handshake.id, t.message ?: t.javaClass.simpleName) }
}

private class OkHttpStreamSocket(private val webSocket: WebSocket) : StreamSocket {
    override fun send(text: String) {
        webSocket.send(text)
    }

    override fun close(code: Int, reason: String) {
        webSocket.close(code, reason)
    }

    // WebSocket.cancel() del lado del servidor no tiene una Call que cancelar; failWebSocket cierra el socket TCP
    // sin trama de cierre, que es lo que ve el cliente cuando se cae la red.
    override fun drop() {
        (webSocket as RealWebSocket).failWebSocket(SocketException("Corte del canal en vivo"))
    }
}
