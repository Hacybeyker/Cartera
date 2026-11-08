package com.hacybeyker.cartera.feature.market.ui.portfolio

object PortfolioTestTags {
    /** Prefijo de los estados (`portafolio_cargando`, `portafolio_error`, `portafolio_vacio`) y marca de la pantalla. */
    const val PORTFOLIO = "portafolio"
    const val LIST = "portafolio_lista"
    const val RECONNECTING = "portafolio_reconectando"

    fun holding(symbol: String) = "portafolio_item_$symbol"
}
