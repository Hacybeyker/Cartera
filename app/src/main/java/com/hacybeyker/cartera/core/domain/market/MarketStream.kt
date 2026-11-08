package com.hacybeyker.cartera.core.domain.market

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Canal en vivo de precios: una sola conexión para toda la app. Cada flujo de precios cuenta como suscriptor
 * mientras se recolecta; la conexión se abre con el primero y se cierra cuando no queda ninguno.
 */
interface MarketStream {
    /** El último precio de cada símbolo y después cada cambio. Al reconectar sigue con el último valor. */
    fun quotes(symbols: Set<String>): Flow<Quote>

    /** Las cotizaciones del par (`USD/PEN`). */
    fun fxQuotes(pair: String): Flow<FxQuote>

    /** Estado de la conexión; observarlo no la abre. */
    val status: StateFlow<MarketStreamStatus>
}

enum class MarketStreamStatus {
    /** Sin suscriptores o sin sesión: no hay conexión. */
    IDLE,
    CONNECTING,
    CONNECTED,

    /** La conexión se cayó o no se pudo abrir; se vuelve a intentar con espera creciente. */
    RECONNECTING
}
