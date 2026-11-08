package com.hacybeyker.cartera.demo.backend

import java.math.BigDecimal
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FixturesTest {
    private val catalog = FixtureCatalog(fileFixtures)
    private val accountIds = listOf("ACC-001", "ACC-002", "ACC-003")

    @Test
    fun `movement counts match the reference data`() {
        assertEquals(listOf(1200, 30, 12), accountIds.map { catalog.movements(it).size })
    }

    @Test
    fun `movement ids are unique and belong to their file`() {
        val all = accountIds.flatMap { id -> catalog.movements(id).onEach { assertEquals(id, it.accountId) } }

        assertEquals(all.size, all.map { it.id }.toSet().size)
    }

    @Test
    fun `every breakdown adds up to the movement amount`() {
        accountIds.flatMap(catalog::movements).forEach { record ->
            val breakdown = record.json.obj("breakdown")
            val lines = breakdown.getValue("lines").jsonArray.sumOf { value(it.jsonObject.obj("amount")) }
            val total = lines + value(breakdown.obj("fees")) + value(breakdown.obj("taxes"))

            assertEquals(record.id, value(record.json.obj("amount")).abs(), total)
        }
    }

    @Test
    fun `amount sign matches the movement type`() {
        accountIds.flatMap(catalog::movements).forEach { record ->
            val negative = value(record.json.obj("amount")).signum() < 0
            assertEquals(record.id, if (negative) "DEBIT" else "CREDIT", record.json.string("type"))
        }
    }

    @Test
    fun `third party accounts cover both currencies`() {
        assertEquals(
            listOf("PEN", "PEN", "USD"),
            catalog.thirdPartyAccounts.map { it.string("currency") }
        )
        assertTrue(catalog.users.single().string("username") == "ana.torres")
    }

    @Test
    fun `stocks are the eight reference companies in dollars`() {
        val names = listOf(
            "Andina Minerals",
            "Cusco Telecom",
            "Inka Energy",
            "Lima Logistics",
            "Moche Tech",
            "Nazca Retail",
            "Pacífico Foods",
            "Qori Gold"
        )

        assertEquals(MarketDeskTest.REFERENCE_PRICES.keys.toList(), catalog.stocks.map { it.symbol })
        assertEquals(names, catalog.stocks.map { it.name })
        assertTrue(catalog.stocks.all { it.currency == "USD" && it.description.isNotBlank() && it.volume > 0 })
    }

    @Test
    fun `day data is consistent with the one day history`() {
        catalog.stocks.forEach { stock ->
            val day = catalog.history(stock.symbol).getValue(HistoryRange.ONE_DAY).prices

            assertEquals(stock.symbol, day.first(), stock.open)
            assertEquals(stock.symbol, day.max(), stock.high)
            assertEquals(stock.symbol, day.min(), stock.low)
            assertTrue(stock.symbol, stock.previousClose.signum() > 0)
        }
    }

    @Test
    fun `first tick of every stock is its initial price`() {
        MarketDeskTest.REFERENCE_PRICES.forEach { (symbol, price) ->
            assertEquals(symbol, BigDecimal(price), catalog.ticks(symbol).first())
        }
    }

    @Test
    fun `ticks are a bounded walk with two decimals`() {
        MarketDeskTest.REFERENCE_PRICES.forEach { (symbol, price) ->
            val ticks = catalog.ticks(symbol)
            val initial = BigDecimal(price)

            assertEquals(symbol, MarketDeskTest.TICKS, ticks.size)
            assertTrue(symbol, ticks.all { it.scale() == 2 && it.signum() > 0 })
            assertTrue(symbol, ticks.all { (it - initial).abs() <= initial * BigDecimal("0.031") })
            assertTrue(symbol, ticks.zipWithNext().all { (a, b) -> (b - a).abs() <= a * BigDecimal("0.01") })
            assertTrue(symbol, ticks.toSet().size > 10)
        }
    }

    @Test
    fun `every history range ends at the initial price`() {
        val sizes = mapOf(
            HistoryRange.ONE_DAY to (390 to 60L),
            HistoryRange.ONE_WEEK to (168 to 3600L),
            HistoryRange.ONE_MONTH to (120 to 21600L),
            HistoryRange.ONE_YEAR to (365 to 86400L)
        )
        MarketDeskTest.REFERENCE_PRICES.forEach { (symbol, price) ->
            sizes.forEach { (range, expected) ->
                val series = catalog.history(symbol).getValue(range)

                assertEquals("$symbol $range", expected, series.prices.size to series.stepSeconds)
                assertEquals("$symbol $range", BigDecimal(price), series.prices.last())
                assertTrue("$symbol $range", series.prices.all { it.scale() == 2 && it.signum() > 0 })
            }
        }
    }

    @Test
    fun `fx starts at the reference rates with a constant spread`() {
        val ticks = catalog.fxTicks

        assertEquals(FxTick(BigDecimal("3.742"), BigDecimal("3.768")), ticks.first())
        assertEquals(MarketDeskTest.TICKS, ticks.size)
        assertTrue(ticks.all { it.sell - it.buy == BigDecimal("0.026") && it.buy.scale() == 3 })
        assertTrue(ticks.all { (it.buy - BigDecimal("3.742")).abs() <= BigDecimal("0.04") })
        assertTrue(ticks.toSet().size > 10)
    }

    @Test
    fun `portfolio is the reference one of the test user`() {
        val portfolio = catalog.portfolio

        assertEquals("USR-001", portfolio.userId)
        assertEquals("ACC-003", portfolio.accountId)
        assertEquals(
            listOf(
                Holding("ANDM", 10, BigDecimal("40.00")),
                Holding("MOCH", 5, BigDecimal("80.00")),
                Holding("QORI", 2, BigDecimal("120.00"))
            ),
            portfolio.holdings
        )
    }

    private fun value(money: JsonObject) = BigDecimal(money.string("value"))
}
