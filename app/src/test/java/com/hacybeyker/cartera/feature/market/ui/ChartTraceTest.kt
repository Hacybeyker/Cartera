package com.hacybeyker.cartera.feature.market.ui

import com.hacybeyker.cartera.feature.market.ui.detail.ChartGeometry
import com.hacybeyker.cartera.feature.market.ui.detail.ChartTrace
import com.hacybeyker.cartera.feature.market.ui.detail.PriceChart
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Test

class ChartTraceTest {
    private val history = listOf("10", "12", "11", "14", "13").map { it.toBigDecimal() }
    private val chart = PriceChart.from(history, live = "13".toBigDecimal())

    private var builds = 0

    private fun points(geometry: ChartGeometry): List<Pair<Float, Float>> {
        builds++
        val points = mutableListOf<Pair<Float, Float>>()
        geometry.forEachFixedPoint { _, x, y -> points += x to y }
        return points
    }

    private fun ChartTrace<List<Pair<Float, Float>>>.at(chart: PriceChart, width: Float = 400f) =
        trace(chart, width = width, height = 100f, inset = 4f)

    @Test
    fun `both drawing paths trace the same points`() {
        val cachedPath = ChartTrace(livePath = false, ::points)
        val livePath = ChartTrace(livePath = true, ::points)

        listOf(chart, chart.withLast("13.5".toBigDecimal()), chart.withLast("20".toBigDecimal())).forEach {
            assertEquals(cachedPath.at(it), livePath.at(it))
        }
    }

    @Test
    fun `cached path reuses the trace while only the last point moves`() {
        val trace = ChartTrace(livePath = false, ::points)

        val first = trace.at(chart)
        val moved = trace.at(chart.withLast("13.5".toBigDecimal()))

        assertSame(first, moved)
        assertEquals(1, builds)
    }

    @Test
    fun `cached path traces again with new bounds, a new series or another size`() {
        val trace = ChartTrace(livePath = false, ::points)
        val first = trace.at(chart)

        val newHigh = trace.at(chart.withLast("20".toBigDecimal()))
        val newSeries = trace.at(PriceChart.from(history, live = chart.last))
        val wider = trace.at(chart, width = 800f)

        assertNotSame(first, newHigh)
        assertEquals(first, newSeries)
        assertNotSame(first, newSeries)
        assertNotSame(newSeries, wider)
        assertEquals(4, builds)
    }

    @Test
    fun `live path traces again every time, with the same points`() {
        val trace = ChartTrace(livePath = true, ::points)

        val first = trace.at(chart)
        val again = trace.at(chart)
        val moved = trace.at(chart.withLast("13.5".toBigDecimal()))

        assertNotSame(first, again)
        assertEquals(first, again)
        assertEquals(first, moved)
        assertEquals(3, builds)
    }
}
