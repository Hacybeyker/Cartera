package com.hacybeyker.cartera.demo.backend

import java.time.Duration
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class MarketDeskTest {
    private val harness = RouterHarness()
    private val token = harness.login()

    @Test
    fun `stock list has the eight reference stocks at their initial price`() {
        val response = harness.get("market/stocks", token)
        val items = response.json().items().map { it.jsonObject }

        assertEquals(200, response.status)
        assertEquals(REFERENCE_PRICES.keys.toList(), items.map { it.string("symbol") })
        assertEquals(REFERENCE_PRICES.values.toList(), items.map { it.string("last") })
        assertEquals(setOf("USD"), items.map { it.string("currency") }.toSet())
        val andm = items.first()
        assertEquals("Andina Minerals", andm.string("name"))
        assertEquals(setOf("symbol", "name", "currency", "last", "change", "changePct", "ts"), andm.keys)
        assertEquals("2026-11-07T10:00:00-05:00", andm.string("ts"))
    }

    @Test
    fun `change and percentage are measured against the previous close`() {
        val stock = harness.state.market.stock("ANDM")!!
        val quote = harness.get("market/stocks/ANDM", token).json()

        val change = "42.10".toBigDecimal() - stock.previousClose
        assertEquals(change.toPlainString(), quote.string("change"))
        assertEquals(MarketDesk.percent(change, stock.previousClose), quote.string("changePct"))
    }

    @Test
    fun `stock detail adds the day data and the description`() {
        val stock = harness.state.market.stock("QORI")!!
        val detail = harness.get("market/stocks/QORI", token).json()

        assertEquals("115.20", detail.string("last"))
        assertEquals(stock.open.toPlainString(), detail.string("open"))
        assertEquals(stock.high.toPlainString(), detail.string("high"))
        assertEquals(stock.low.toPlainString(), detail.string("low"))
        assertEquals(stock.previousClose.toPlainString(), detail.string("previousClose"))
        assertEquals(stock.volume, detail.getValue("volume").jsonPrimitive.long)
        assertEquals(stock.description, detail.string("description"))
    }

    @Test
    fun `unknown symbol is not found`() {
        assertProblem(harness.get("market/stocks/XXXX", token), 404, "NOT_FOUND")
        assertProblem(harness.get("market/stocks/XXXX/history", token, mapOf("range" to "1D")), 404, "NOT_FOUND")
    }

    @Test
    fun `every range ends at the current price and now`() {
        val sizes = mapOf("1D" to 390, "1W" to 168, "1M" to 120, "1Y" to 365)

        sizes.forEach { (range, size) ->
            val body = harness.get("market/stocks/QORI/history", token, mapOf("range" to range)).json()
            val points = body.getValue("points").jsonArray.map { it.jsonObject }

            assertEquals(range, body.string("range"))
            assertEquals("USD", body.string("currency"))
            assertEquals(size, points.size)
            assertEquals("115.20", points.last().string("price"))
            assertEquals("2026-11-07T10:00:00-05:00", points.last().string("ts"))
        }
    }

    @Test
    fun `one day history is one point per minute`() {
        val points = harness.get("market/stocks/ANDM/history", token, mapOf("range" to "1D")).json()
            .getValue("points").jsonArray.map { it.jsonObject.string("ts") }

        assertEquals("2026-11-07T03:31:00-05:00", points.first())
        assertEquals("2026-11-07T09:59:00-05:00", points[points.size - 2])
    }

    @Test
    fun `invalid or missing range is a validation error`() {
        val invalid = harness.get("market/stocks/ANDM/history", token, mapOf("range" to "5Y"))
        val missing = harness.get("market/stocks/ANDM/history", token)

        assertProblem(invalid, 422, "VALIDATION_ERROR")
        assertEquals(listOf("range"), invalid.errorFields())
        assertEquals(listOf("range"), missing.errorFields())
    }

    @Test
    fun `market endpoints need a session`() {
        listOf("market/stocks", "market/stocks/ANDM", "market/stocks/ANDM/history", "portfolio", "market/stream")
            .forEach { path -> assertProblem(harness.get(path), 401, "UNAUTHORIZED") }
    }

    @Test
    fun `stream handshake is not served yet`() {
        assertProblem(harness.get("market/stream", token), 404, "NOT_FOUND")
    }

    @Test
    fun `portfolio of the test user values each holding at the current price`() {
        val body = harness.get("portfolio", token).json()
        val holdings = body.getValue("holdings").jsonArray.map { it.jsonObject }
        val andm = holdings.first()

        assertEquals("ACC-003", body.string("accountId"))
        assertEquals("USD", body.string("currency"))
        assertEquals(listOf("ANDM", "MOCH", "QORI"), holdings.map { it.string("symbol") })
        assertEquals(listOf("10", "5", "2"), holdings.map { it.getValue("quantity").jsonPrimitive.content })
        assertEquals("Andina Minerals", andm.string("name"))
        assertEquals("40.00", andm.obj("averagePrice").string("value"))
        assertEquals("42.10", andm.obj("lastPrice").string("value"))
        assertEquals("421.00", andm.obj("marketValue").string("value"))
        assertEquals("400.00", andm.obj("cost").string("value"))
        assertEquals("21.00", andm.obj("gain").string("value"))
        assertEquals("5.25", andm.string("gainPct"))
        assertEquals("-9.60", holdings.last().obj("gain").string("value"))
        val totals = body.obj("totals")
        assertEquals("1094.40", totals.obj("marketValue").string("value"))
        assertEquals("1040.00", totals.obj("cost").string("value"))
        assertEquals("54.40", totals.obj("gain").string("value"))
        assertEquals("5.23", totals.string("gainPct"))
    }

    @Test
    fun `day change adds each holding against its previous close`() {
        val body = harness.get("portfolio", token).json()
        val expected = listOf("ANDM" to 10, "MOCH" to 5, "QORI" to 2).sumOf { (symbol, quantity) ->
            (harness.state.market.price(symbol) - harness.state.market.stock(symbol)!!.previousClose) *
                quantity.toBigDecimal()
        }

        assertEquals(expected.setScale(2).toPlainString(), body.obj("totals").obj("dayChange").string("value"))
    }

    @Test
    fun `advancing the market moves the quotes, the day range and the portfolio`() {
        harness.clock.now = harness.clock.now.plus(Duration.ofSeconds(2))
        val quote = harness.state.market.advance("QORI")
        val ticks = FixtureCatalog(fileFixtures).ticks("QORI")

        assertEquals(ticks[1], quote.last)
        assertEquals(ticks[1].toPlainString(), harness.get("market/stocks/QORI", token).json().string("last"))
        assertEquals("2026-11-07T10:00:02-05:00", harness.get("market/stocks/QORI", token).json().string("ts"))
        val qori = harness.get("portfolio", token).json().getValue("holdings").jsonArray.last().jsonObject
        assertEquals(ticks[1].toPlainString(), qori.obj("lastPrice").string("value"))
        assertEquals("42.10", harness.get("market/stocks/ANDM", token).json().string("last"))
    }

    @Test
    fun `day high and low include every price played`() {
        val market = harness.state.market
        val stock = market.stock("LIMA")!!
        val played = mutableListOf(market.price("LIMA"))
        repeat(TICKS - 1) { played += market.advance("LIMA").last }

        val quote = market.quote("LIMA")!!
        assertEquals(maxOf(stock.high, played.max()), quote.high)
        assertEquals(minOf(stock.low, played.min()), quote.low)
    }

    @Test
    fun `ticks start over after the last one`() {
        val market = harness.state.market
        repeat(TICKS) { market.advance("ANDM") }

        assertEquals("42.10".toBigDecimal(), market.price("ANDM"))
    }

    @Test
    fun `fx rate advances, starts over and keeps its tick time`() {
        val market = harness.state.market
        val first = market.fx()

        harness.clock.now = harness.clock.now.plusSeconds(5)
        val second = market.advanceFx()
        repeat(TICKS - 1) { market.advanceFx() }

        assertEquals(FxTick("3.742".toBigDecimal(), "3.768".toBigDecimal()), first.tick)
        assertNotEquals(first.tickedAt, second.tickedAt)
        assertEquals(first.tick, market.fx().tick)
    }

    @Test
    fun `reset puts every price back on its first tick`() {
        harness.state.market.advance("ANDM")
        harness.state.market.advanceFx()
        harness.clock.now = harness.clock.now.plusSeconds(60)

        harness.call("POST", "_demo/reset")

        assertEquals("42.10", harness.get("market/stocks/ANDM", token).json().string("last"))
        assertEquals("2026-11-07T10:01:00-05:00", harness.get("market/stocks/ANDM", token).json().string("ts"))
        assertEquals("3.742", harness.quote(token).string("buy"))
    }

    @Test
    fun `holding sold out leaves the portfolio`() {
        harness.state.holdings.put("USR-001", Holding("MOCH", 0, "80.00".toBigDecimal()))

        val symbols = harness.get("portfolio", token).json().getValue("holdings").jsonArray
            .map { it.jsonObject.string("symbol") }

        assertEquals(listOf("ANDM", "QORI"), symbols)
    }

    @Test
    fun `percent of a zero base is zero`() {
        assertEquals("0.00", MarketDesk.percent("5".toBigDecimal(), "0".toBigDecimal()))
    }

    private fun assertProblem(response: BackendResponse, status: Int, code: String) {
        assertEquals(status, response.status)
        assertEquals(code, response.problemCode())
    }

    companion object {
        const val TICKS = 600
        val REFERENCE_PRICES = linkedMapOf(
            "ANDM" to "42.10",
            "CUSC" to "27.30",
            "INKA" to "63.40",
            "LIMA" to "9.85",
            "MOCH" to "88.60",
            "NAZC" to "12.45",
            "PCFC" to "18.75",
            "QORI" to "115.20"
        )
    }
}
