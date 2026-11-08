package com.hacybeyker.cartera.core.domain.market

import com.hacybeyker.cartera.core.domain.Money
import com.hacybeyker.cartera.core.domain.portfolio.Portfolio
import com.hacybeyker.cartera.core.testing.TestPortfolio
import com.hacybeyker.cartera.core.testing.TestQuotes
import com.hacybeyker.cartera.core.testing.testQuote
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class PortfolioTest {

    private fun usd(value: String) = Money(value.toBigDecimal(), "USD")

    @Test
    fun `totals follow the server rules at the initial prices`() {
        val totals = TestPortfolio.totals

        assertEquals(usd("1094.40"), totals.marketValue)
        assertEquals(usd("1040.00"), totals.cost)
        assertEquals(usd("54.40"), totals.gain)
        assertEquals("5.23", totals.gainPct.toPlainString())
        assertEquals(usd("-9.17"), totals.dayChange)
        assertEquals("-0.83", totals.dayChangePct.toPlainString())
    }

    @Test
    fun `each holding is valued at its price`() {
        val qori = TestPortfolio.holdings.single { it.symbol == "QORI" }

        assertEquals(usd("230.40"), qori.marketValue)
        assertEquals(usd("240.00"), qori.cost)
        assertEquals(usd("-9.60"), qori.gain)
        assertEquals("-4.00", qori.gainPct.toPlainString())
        assertEquals(usd("-0.72"), qori.dayChange)
    }

    @Test
    fun `a live price revalues only its holding`() {
        val revalued = TestPortfolio.withQuote(testQuote("ANDM", "43.10", change = "1.34"))

        assertEquals(usd("431.00"), revalued.holdings.first { it.symbol == "ANDM" }.marketValue)
        assertEquals(usd("1104.40"), revalued.totals.marketValue)
        assertEquals(usd("0.83"), revalued.totals.dayChange)
        assertSame(TestPortfolio.holdings[1], revalued.holdings[1])
    }

    @Test
    fun `a price of a stock not held leaves the portfolio as it was`() {
        assertFalse(TestPortfolio.holds("LIMA"))
        assertTrue(TestPortfolio.holds("QORI"))
        assertSame(TestPortfolio, TestPortfolio.withQuote(testQuote("LIMA", "9.90")))
        assertSame(TestPortfolio.holdings[0], TestPortfolio.holdings[0].withQuote(testQuote("QORI", "1.00")))
    }

    @Test
    fun `revaluing with the list prices matches the server`() {
        assertEquals(TestPortfolio, TestPortfolio.withQuotes(TestQuotes))
    }

    @Test
    fun `an empty portfolio has zero totals and percentages`() {
        val totals = Portfolio("ACC-003", "USD", emptyList()).totals

        assertEquals(usd("0.00"), totals.marketValue)
        assertEquals("0.00", totals.gainPct.toPlainString())
        assertEquals("0.00", totals.dayChangePct.toPlainString())
    }
}
