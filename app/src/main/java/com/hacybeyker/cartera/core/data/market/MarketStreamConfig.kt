package com.hacybeyker.cartera.core.data.market

import javax.inject.Qualifier

/** URL del canal en vivo, derivada de `BASE_URL` (OkHttp acepta `http(s)` para abrir un WebSocket). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MarketStreamUrl

/** Scope del canal en vivo: no ejecuta en paralelo, así el estado del canal no necesita bloqueos. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MarketStreamScope

/** Espera antes de cada reconexión; los tests la reemplazan para no esperar de verdad. */
fun interface ReconnectWait {
    suspend fun await(millis: Long)
}
