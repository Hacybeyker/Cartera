package com.hacybeyker.cartera.feature.market.ui

import com.hacybeyker.cartera.feature.market.ui.detail.ChartBounds
import com.hacybeyker.cartera.feature.market.ui.detail.ChartGeometry
import com.hacybeyker.cartera.feature.market.ui.detail.PriceChart
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Test

class PriceChartTest {
    private fun prices(vararg values: String) = values.map { it.toBigDecimal() }

    private val chart = PriceChart.from(prices("10", "12", "11", "14", "13"), live = "13".toBigDecimal())

    @Test
    fun `the live price replaces the last point of the history`() {
        val joined = PriceChart.from(prices("10", "12", "99"), live = "11".toBigDecimal())

        assertEquals(2, joined.series.size)
        assertEquals(2, joined.lastIndex)
        assertEquals("11", joined.last.toPlainString())
        assertEquals("10", joined.low.toPlainString())
        assertEquals("12", joined.high.toPlainString())
    }

    @Test
    fun `the bounds include the live price`() {
        val higher = chart.withLast("15.5".toBigDecimal())

        assertEquals(ChartBounds(low = 10f, high = 14f), chart.bounds)
        assertEquals(ChartBounds(low = 10f, high = 15.5f), higher.bounds)
        assertEquals("9", chart.withLast("9".toBigDecimal()).low.toPlainString())
    }

    @Test
    fun `moving the last point keeps the fixed series`() {
        val moved = chart.withLast("13.5".toBigDecimal())

        assertSame(chart.series, moved.series)
        assertNotSame(chart, moved)
        assertSame(chart, chart.withLast("13.00".toBigDecimal()))
    }

    @Test
    fun `points spread across the width and between the bounds`() {
        val geometry = ChartGeometry(chart, width = 400f, height = 200f, inset = 10f)

        assertEquals(0f, geometry.x(0), 0f)
        assertEquals(100f, geometry.x(1), 0f)
        assertEquals(400f, geometry.x(chart.lastIndex), 0f)
        assertEquals(10f, geometry.y(14f), 0.001f)
        assertEquals(190f, geometry.y(10f), 0.001f)
        assertEquals(100f, geometry.y(12f), 0.001f)
    }

    @Test
    fun `the fixed points are visited in order, scaled`() {
        val visited = mutableListOf<Triple<Int, Float, Float>>()
        ChartGeometry(chart, width = 400f, height = 100f).forEachFixedPoint { index, x, y ->
            visited += Triple(index, x, y)
        }

        assertEquals(listOf(0, 1, 2, 3), visited.map { it.first })
        assertEquals(listOf(0f, 100f, 200f, 300f), visited.map { it.second })
        assertEquals(listOf(100f, 50f, 75f, 0f), visited.map { it.third })
    }

    @Test
    fun `a flat or single point series is drawn in the middle`() {
        val flat = ChartGeometry(PriceChart.from(prices("5", "5", "5"), "5".toBigDecimal()), 300f, 100f)
        val single = PriceChart.from(prices("7"), "8".toBigDecimal())
        val alone = ChartGeometry(single, 300f, 100f)

        assertEquals(50f, flat.y(5f), 0f)
        assertEquals(0, single.series.size)
        assertEquals(150f, alone.x(single.lastIndex), 0f)
        assertEquals(50f, alone.y(8f), 0f)
    }
}
