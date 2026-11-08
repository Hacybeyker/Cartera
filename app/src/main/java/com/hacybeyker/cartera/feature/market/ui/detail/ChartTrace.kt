package com.hacybeyker.cartera.feature.market.ui.detail

/** Trazado de un [PriceChart] en un lienzo, armado con [build]. [livePath] elige el camino (`CHART_LIVE_PATH`). */
class ChartTrace<T : Any>(private val livePath: Boolean, private val build: (ChartGeometry) -> T) {
    private var tracedFor: TraceKey? = null
    private var traced: T? = null

    fun trace(chart: PriceChart, width: Float, height: Float, inset: Float): T {
        val geometry = ChartGeometry(chart, width, height, inset)
        if (livePath) return build(geometry)
        return cachedPath(TraceKey(chart.series, chart.bounds, width, height, inset), geometry)
    }

    private fun cachedPath(key: TraceKey, geometry: ChartGeometry): T =
        traced?.takeIf { key == tracedFor } ?: build(geometry).also {
            traced = it
            tracedFor = key
        }

    private data class TraceKey(
        val series: ChartSeries,
        val bounds: ChartBounds,
        val width: Float,
        val height: Float,
        val inset: Float
    )
}
