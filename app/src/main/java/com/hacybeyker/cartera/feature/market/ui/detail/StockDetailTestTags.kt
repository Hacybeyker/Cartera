package com.hacybeyker.cartera.feature.market.ui.detail

import com.hacybeyker.cartera.core.domain.market.HistoryRange

object StockDetailTestTags {
    const val SCREEN = "accion_detalle"
    const val MARK = "accion_detalle"

    /** Prefijo de los estados de la pantalla (`accion_cargando`, `accion_error`, `accion_reintentar`). */
    const val STATES = "accion"
    const val PRICE = "accion_precio"
    const val CHART = "accion_grafico"

    /** Prefijo de los estados del gráfico al cambiar de rango (`accion_grafico_cargando`, `…_error`, `…_reintentar`). */
    const val CHART_STATES = "accion_grafico"
    const val HOLDING = "accion_tenencia"
    const val RECONNECTING = "accion_reconectando"

    /** `accion_rango_1D`, `…_1S`, `…_1M` y `…_1A`: la etiqueta que se ve, no el código del contrato (1W, 1Y). */
    fun range(range: HistoryRange) = "accion_rango_${range.tagSuffix}"
}

private val HistoryRange.tagSuffix: String
    get() = when (this) {
        HistoryRange.DAY -> "1D"
        HistoryRange.WEEK -> "1S"
        HistoryRange.MONTH -> "1M"
        HistoryRange.YEAR -> "1A"
    }
