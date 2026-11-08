package com.hacybeyker.cartera.feature.home.ui.market

object MarketTestTags {
    /** Prefijo de los estados (`mercado_cargando`, `mercado_error`, `mercado_reintentar`) y marca de la pantalla. */
    const val MARKET = "mercado"
    const val LIST = "mercado_lista"
    const val PORTFOLIO = "mercado_portafolio"
    const val RECONNECTING = "mercado_reconectando"

    fun stock(symbol: String) = "mercado_item_$symbol"
}
