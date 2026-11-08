package com.hacybeyker.cartera.demo.backend

import java.io.File
import java.math.BigDecimal
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private val REFERENCE_BILLERS = setOf("BIL-LUZ", "BIL-AGUA", "BIL-TEL", "BIL-NET")

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

    @Test
    fun `billers are twenty, the reference ones first in their category`() {
        val billers = catalog.billers

        assertEquals(20, billers.size)
        assertEquals(billers.size, billers.map { it.id }.toSet().size)
        assertEquals(
            listOf(
                Biller("BIL-LUZ", "Luz del Norte", "Electricidad", "^\\d{6}$"),
                Biller("BIL-AGUA", "Agua Trujillo", "Agua", "^\\d{6}$"),
                Biller("BIL-TEL", "Telefonía Andina", "Telefonía", "^\\d{6}$"),
                Biller("BIL-NET", "Internet Moche", "Internet", "^\\d{6}$")
            ),
            billers.groupBy { it.category }.values.map { it.first() }.filter { it.id in REFERENCE_BILLERS }
        )
        assertEquals(
            listOf("Electricidad", "Agua", "Gas", "Telefonía", "Internet", "Cable", "Seguros", "Educación"),
            billers.map { it.category }.distinct()
        )
        assertTrue(billers.all { it.name.isNotBlank() && it.customerCodeFormat.startsWith("^") })
    }

    @Test
    fun `reference customers and receipts match the reference data`() {
        fun pending(billerId: String) = catalog.customers(billerId).single().let { customer ->
            Triple(customer.code, customer.name, customer.debts.map { it.period to it.amount.toPlainString() })
        }

        assertEquals(
            Triple("100200", "Ana Torres", listOf("2026-10" to "85.40", "2026-11" to "92.10")),
            pending("BIL-LUZ")
        )
        assertEquals(Triple("300400", "Familia Torres", listOf("2026-10" to "46.80")), pending("BIL-AGUA"))
        assertEquals(Triple("500600", "Ana Torres", listOf("2026-10" to "59.90")), pending("BIL-TEL"))
        assertEquals(Triple("700800", "Ana Torres", emptyList<Pair<String, String>>()), pending("BIL-NET"))
        catalog.billers.filter { it.id in REFERENCE_BILLERS }.forEach { biller ->
            assertTrue(biller.id, biller.accepts(catalog.customers(biller.id).single().code))
        }
    }

    @Test
    fun `filler companies have a receipts file without customers`() {
        val fillers = catalog.billers.filter { it.id !in REFERENCE_BILLERS }

        assertEquals(16, fillers.size)
        fillers.forEach { assertTrue(it.id, catalog.customers(it.id).isEmpty()) }
    }

    @Test
    fun `receipt ids are unique and every receipt is in soles with two decimals`() {
        val debts = catalog.billers.flatMap { biller -> catalog.customers(biller.id).flatMap { it.debts } }

        assertEquals(4, debts.size)
        assertEquals(debts.size, debts.map { it.id }.toSet().size)
        assertTrue(debts.all { it.currency == "PEN" && it.amount.scale() == 2 && it.amount.signum() > 0 })
        assertTrue(debts.all { Regex("""\d{4}-\d{2}""").matches(it.period) })
    }

    @Test
    fun `luz del norte history covers twenty four months and every month adds up with igv`() {
        val history = catalog.customers("BIL-LUZ").single().history.map { it.jsonObject }

        assertEquals(24, history.size)
        assertEquals("2024-10", history.first().string("period"))
        assertEquals("2026-09", history.last().string("period"))
        assertEquals(history.map { it.string("period") }.sorted(), history.map { it.string("period") })
        history.forEach { month ->
            val lines = month.getValue("lines").jsonArray.map { it.jsonObject }
            val total = lines.sumOf { value(it.obj("amount")) }

            assertEquals(month.string("period"), value(month.obj("amount")), total)
            assertTrue(month.string("period"), lines.size > 50)
            assertTrue(month.string("period"), lines.all { it.string("detail").isNotBlank() })
            assertEquals("IGV (18 %)", lines.last().string("concept"))
            assertTrue(month.string("paidAt").endsWith("-05:00"))
        }
        assertTrue(history.all { it.obj("amount").string("currency") == "PEN" })
    }

    @Test
    fun `only luz del norte keeps history and its file measures at least 150 kb`() {
        val withHistory = catalog.billers.filter { biller ->
            catalog.customers(biller.id).any { it.history.isNotEmpty() }
        }.map { it.id }

        assertEquals(listOf("BIL-LUZ"), withHistory)
        assertTrue(File(fixturesDir, "debts/BIL-LUZ.json").length() > 150_000)
    }

    @Test
    fun `the test user has the operations pin`() {
        assertEquals("1234", catalog.users.single().string("operationsPin"))
    }

    private fun value(money: JsonObject) = BigDecimal(money.string("value"))
}
