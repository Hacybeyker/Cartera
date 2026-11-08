package com.hacybeyker.cartera.core.ui.format

import com.hacybeyker.cartera.core.domain.Money
import com.hacybeyker.cartera.core.testing.pen
import java.time.OffsetDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class FormatsTest {

    @Test
    fun `balances use the reference format`() {
        assertEquals("S/ 8,450.75", pen("8450.75").formatted())
        assertEquals("US$ 1,050.20", Money("1050.2".toBigDecimal(), "USD").formatted())
        assertEquals("EUR 5.00", Money("5".toBigDecimal(), "EUR").formatted())
    }

    @Test
    fun `movement amounts keep their sign`() {
        assertEquals("-S/ 1,379.23", pen("-1379.23").formattedSigned())
        assertEquals("+S/ 5,200.00", pen("5200").formattedSigned())
        assertEquals("S/ 0.00", pen("0.00").formattedSigned())
    }

    @Test
    fun `counts use thousands separators`() {
        assertEquals("2,795,000", 2_795_000L.formattedCount())
        assertEquals("0", 0L.formattedCount())
    }

    @Test
    fun `dates use a fixed numeric format`() {
        assertEquals("06/11/2026 14:32", OffsetDateTime.parse("2026-11-06T14:32:00-05:00").formatted())
    }

    @Test
    fun `price changes and percentages keep their sign`() {
        assertEquals("+0.34", "0.34".toBigDecimal().formattedChange())
        assertEquals("-1,250.00", "-1250".toBigDecimal().formattedChange())
        assertEquals("0.00", "0.00".toBigDecimal().formattedChange())
        assertEquals("+0.81 %", "0.81".toBigDecimal().formattedPercent())
        assertEquals("-2.98 %", "-2.98".toBigDecimal().formattedPercent())
    }
}
