package com.hacybeyker.cartera.feature.home.ui.exchange

import com.hacybeyker.cartera.core.ui.exchange.ExchangeFieldTestTags

object ExchangeTestTags {
    /** Prefijo de los estados (`cambio_cargando`, `cambio_error`, `cambio_reintentar`) y marca de la pantalla. */
    const val EXCHANGE = "cambio"
    const val SCREEN = "cambio_pantalla"
    const val BUY = "cambio_compra"
    const val SELL = "cambio_venta"
    const val AMOUNT = ExchangeFieldTestTags.AMOUNT
    const val DIRECTION = "cambio_direccion"
    const val SOURCE = ExchangeFieldTestTags.SOURCE
    const val DESTINATION = ExchangeFieldTestTags.DESTINATION
    const val ESTIMATE = "cambio_estimado"
    const val HOW_WE_CALCULATE = "cambio_como_calculamos"
    const val CONTINUE = "cambio_continuar"
    const val RECONNECTING = "cambio_reconectando"

    const val HOW_WE_CALCULATE_SHEET = "sheet_como_calculamos"
    const val HOW_WE_CALCULATE_CLOSE = "sheet_como_calculamos_entendido"
}
