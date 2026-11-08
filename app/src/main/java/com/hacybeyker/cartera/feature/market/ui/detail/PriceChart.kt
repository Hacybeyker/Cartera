package com.hacybeyker.cartera.feature.market.ui.detail

import androidx.compose.runtime.Immutable
import java.math.BigDecimal

/** Los puntos fijos de una serie: todos menos el último, que es el precio vivo. */
class ChartSeries(prices: List<BigDecimal>) {
    val values: FloatArray = FloatArray(prices.size) { prices[it].toFloat() }
    val low: BigDecimal? = prices.minOrNull()
    val high: BigDecimal? = prices.maxOrNull()

    val size: Int get() = values.size
}

/** Límites verticales del gráfico, en el mismo orden que los precios. */
@Immutable
data class ChartBounds(val low: Float, val high: Float)

/** Serie de un rango con su último punto en [last], el precio vigente. */
@Immutable
data class PriceChart(val series: ChartSeries, val last: BigDecimal) {
    val low: BigDecimal get() = series.low?.min(last) ?: last
    val high: BigDecimal get() = series.high?.max(last) ?: last
    val bounds: ChartBounds get() = ChartBounds(low = low.toFloat(), high = high.toFloat())

    /** Índice del último punto; los fijos van de 0 a `lastIndex - 1`. */
    val lastIndex: Int get() = series.size

    /** El mismo gráfico con el último punto en [price]. */
    fun withLast(price: BigDecimal): PriceChart = if (price.compareTo(last) == 0) this else copy(last = price)

    companion object {
        /** El último punto del historial empalma con el precio vigente: lo reemplaza [live]. */
        fun from(history: List<BigDecimal>, live: BigDecimal): PriceChart =
            PriceChart(series = ChartSeries(history.dropLast(1)), last = live)
    }
}

/**
 * Coordenadas de [chart] en un lienzo de [width] × [height], con [inset] libre arriba y abajo para que el trazo no
 * se corte en los extremos. La x reparte los puntos a lo ancho; la y va del máximo (arriba) al mínimo (abajo).
 */
class ChartGeometry(
    private val chart: PriceChart,
    private val width: Float,
    private val height: Float,
    private val inset: Float = 0f
) {
    private val bounds = chart.bounds

    fun x(index: Int): Float = if (chart.lastIndex == 0) width / 2 else width * index / chart.lastIndex

    fun y(price: Float): Float {
        val span = bounds.high - bounds.low
        val drawable = height - 2 * inset
        return if (span == 0f) height / 2 else inset + drawable * (bounds.high - price) / span
    }

    /** Recorre los puntos fijos en orden, ya escalados. */
    fun forEachFixedPoint(action: (index: Int, x: Float, y: Float) -> Unit) {
        chart.series.values.forEachIndexed { index, value -> action(index, x(index), y(value)) }
    }
}
