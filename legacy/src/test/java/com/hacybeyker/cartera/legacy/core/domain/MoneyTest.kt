package com.hacybeyker.cartera.legacy.core.domain

import com.hacybeyker.cartera.legacy.testing.pen
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class MoneyTest {

    @Test
    fun `soles are formatted with two decimals and thousands separators`() {
        assertEquals("S/ 85.40", pen("85.40").format())
        assertEquals("S/ 2,122.50", pen("2122.5").format())
        assertEquals("S/ 0.00", Money.zero(PEN).format())
    }

    @Test
    fun `other currencies use their own symbol or their code`() {
        assertEquals("US$ 1,050.20", Money(BigDecimal("1050.20"), "USD").format())
        assertEquals("EUR 3.00", Money(BigDecimal("3"), "EUR").format())
    }

    @Test
    fun `amounts add exactly`() {
        assertEquals(pen("177.50"), pen("85.40") + pen("92.10"))
        assertEquals("0.30", (pen("0.10") + pen("0.20")).amount.toPlainString())
    }

    @Test
    fun `amounts in different currencies do not add`() {
        assertThrows(IllegalArgumentException::class.java) { pen("1.00") + Money(BigDecimal("1.00"), "USD") }
    }
}
