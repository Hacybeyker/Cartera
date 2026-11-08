package com.hacybeyker.cartera.feature.market.ui.detail

import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp

/**
 * Gráfico de línea de [chart], dibujado con el Canvas de Compose. El trazado de los puntos fijos se arma una vez por
 * serie, límites y tamaño; el precio vivo solo mueve el último tramo y el punto final, que se leen al dibujar.
 */
@Composable
fun PriceChartLine(
    chart: PriceChart,
    color: Color,
    strokeWidth: Dp,
    description: String,
    modifier: Modifier = Modifier
) {
    val latest = rememberUpdatedState(chart)
    val stroke = with(LocalDensity.current) { strokeWidth.toPx() }
    val drawing = remember(chart.series, chart.bounds, color, stroke) {
        Modifier.drawWithCache {
            val geometry = ChartGeometry(chart, size.width, size.height, inset = stroke * 2)
            val fixed = Path()
            geometry.forEachFixedPoint { index, x, y -> if (index == 0) fixed.moveTo(x, y) else fixed.lineTo(x, y) }
            val values = chart.series.values
            val lastFixed = values.lastOrNull()?.let { Offset(geometry.x(values.lastIndex), geometry.y(it)) }
            val style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
            onDrawBehind {
                drawPath(path = fixed, color = color, style = style)
                val end = Offset(geometry.x(chart.lastIndex), geometry.y(latest.value.last.toFloat()))
                lastFixed?.let { start ->
                    drawLine(color = color, start = start, end = end, strokeWidth = stroke, cap = StrokeCap.Round)
                }
                drawCircle(color = color, radius = stroke * 2, center = end)
            }
        }
    }
    Spacer(modifier = modifier.then(drawing).semantics { contentDescription = description })
}
