package com.hacybeyker.cartera.feature.market.ui.detail

import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp

/**
 * Gráfico de línea de [chart], dibujado con el Canvas de Compose: el trazado de los puntos fijos sale de
 * [ChartTrace], y el precio vivo mueve el último tramo y el punto final, que se leen al dibujar. [livePath] elige el
 * camino de dibujo (`CHART_LIVE_PATH`); los dos se ven igual.
 */
@Composable
fun PriceChartLine(
    chart: PriceChart,
    color: Color,
    strokeWidth: Dp,
    description: String,
    modifier: Modifier = Modifier,
    livePath: Boolean = false
) {
    val latest = rememberUpdatedState(chart)
    val stroke = with(LocalDensity.current) { strokeWidth.toPx() }
    val trace = remember(livePath) { ChartTrace(livePath, ::fixedPath) }
    val frameTime = if (livePath) rememberFrameTime() else null
    Spacer(
        modifier = modifier
            .drawBehind {
                frameTime?.value
                drawChart(chart = latest.value, trace = trace, color = color, stroke = stroke)
            }
            .semantics { contentDescription = description }
    )
}

@Composable
private fun rememberFrameTime(): State<Long> {
    val frameTime = remember { mutableLongStateOf(0L) }
    LaunchedEffect(frameTime) {
        while (true) withInfiniteAnimationFrameNanos { frameTime.longValue = it }
    }
    return frameTime
}

private fun fixedPath(geometry: ChartGeometry): Path = Path().apply {
    geometry.forEachFixedPoint { index, x, y -> if (index == 0) moveTo(x, y) else lineTo(x, y) }
}

private fun DrawScope.drawChart(chart: PriceChart, trace: ChartTrace<Path>, color: Color, stroke: Float) {
    val inset = stroke * 2
    val fixed = trace.trace(chart, size.width, size.height, inset)
    val geometry = ChartGeometry(chart, size.width, size.height, inset)
    val style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
    drawPath(path = fixed, color = color, style = style)
    val values = chart.series.values
    val end = Offset(geometry.x(chart.lastIndex), geometry.y(chart.last.toFloat()))
    values.lastOrNull()?.let { value ->
        val start = Offset(geometry.x(values.lastIndex), geometry.y(value))
        drawLine(color = color, start = start, end = end, strokeWidth = stroke, cap = StrokeCap.Round)
    }
    drawCircle(color = color, radius = stroke * 2, center = end)
}
