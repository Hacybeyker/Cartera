package com.hacybeyker.cartera.demo.backend

import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class FxDeskTest {
    private val harness = RouterHarness()
    private val token = harness.login()

    @Test
    fun `rates issue a quote with the default ttl`() {
        val quote = harness.quote(token)

        assertEquals("USD/PEN", quote.string("pair"))
        assertEquals("3.742", quote.string("buy"))
        assertEquals("3.768", quote.string("sell"))
        assertEquals("QTE-000001", quote.string("quoteId"))
        assertEquals("2026-11-07T10:00:30-05:00", quote.string("expiresAt"))
        assertEquals("2026-11-07T10:00:00-05:00", quote.string("ts"))
        assertEquals("QTE-000002", harness.quote(token).string("quoteId"))
    }

    @Test
    fun `quote ttl header sets the expiry and invalid values keep the default`() {
        assertEquals("2026-11-07T10:00:05-05:00", harness.quote(token, ttlSeconds = 5).string("expiresAt"))
        assertEquals("2026-11-07T10:00:30-05:00", harness.quote(token, ttlSeconds = 0).string("expiresAt"))
    }

    @Test
    fun `reference exchange of one hundred dollars at the buy rate`() {
        val quoteId = harness.quote(token).string("quoteId")

        val response = harness.exchange(token, exchangeBody(quoteId))
        val body = response.json()

        assertEquals(201, response.status)
        assertEquals("FX-000001", body.string("exchangeId"))
        assertEquals("80000001", body.string("operationNumber"))
        assertEquals(quoteId, body.string("quoteId"))
        assertEquals("3.742", body.string("rate"))
        assertEquals(money("100.00", "USD"), body.obj("debited"))
        assertEquals(money("374.20", "PEN"), body.obj("credited"))
        assertEquals("ACC-003", body.string("sourceAccountId"))
        assertEquals(money("950.20", "USD"), body.obj("sourceAvailableBalance"))
        assertEquals("ACC-002", body.string("destinationAccountId"))
        assertEquals(money("2674.20", "PEN"), body.obj("destinationAvailableBalance"))
        assertEquals(
            listOf("MOV-003-X0001", "MOV-002-X0001"),
            body.getValue("movementIds").jsonArray.map { it.jsonPrimitive.content }
        )
        assertEquals("950.20", harness.balance(token, "ACC-003"))
        assertEquals("2674.20", harness.balance(token, "ACC-002"))
    }

    @Test
    fun `soles to dollars use the sell rate`() {
        val quoteId = harness.quote(token).string("quoteId")

        val body = harness.exchange(token, exchangeBody(quoteId, "ACC-002", "ACC-003", "376.80", "PEN")).json()

        assertEquals("3.768", body.string("rate"))
        assertEquals(money("100.00", "USD"), body.obj("credited"))
        assertEquals("1923.20", harness.balance(token, "ACC-002"))
        assertEquals("1150.20", harness.balance(token, "ACC-003"))
    }

    @Test
    fun `soles to dollars round the credit to the cent`() {
        val quoteId = harness.quote(token).string("quoteId")

        val body = harness.exchange(token, exchangeBody(quoteId, "ACC-001", "ACC-003", "10.00", "PEN")).json()

        assertEquals(money("2.65", "USD"), body.obj("credited"))
    }

    @Test
    fun `each account gets its own movement`() {
        val quoteId = harness.quote(token).string("quoteId")
        harness.exchange(token, exchangeBody(quoteId))

        val debit = harness.get("movements/MOV-003-X0001", token).json()
        val credit = harness.get("movements/MOV-002-X0001", token).json()
        val breakdown = harness.get("movements/MOV-002-X0001/breakdown", token).json()

        assertEquals("Cambio de dólares a soles", debit.string("description"))
        assertEquals("DEBIT", debit.string("type"))
        assertEquals(money("-100.00", "USD"), debit.obj("amount"))
        assertEquals("ACC-002", debit.obj("counterparty").string("reference"))
        assertEquals("CREDIT", credit.string("type"))
        assertEquals(money("374.20", "PEN"), credit.obj("amount"))
        assertEquals("Ana Torres", credit.obj("counterparty").string("name"))
        assertEquals("3.7420", breakdown.string("exchangeRate"))
        assertEquals(
            "MOV-002-X0001",
            harness.get("movements", token, mapOf("accountId" to "ACC-002")).json().items()
                .first().jsonObject.string("id")
        )
    }

    @Test
    fun `the quote rate holds even after the market moves`() {
        val quoteId = harness.quote(token).string("quoteId")
        repeat(5) { harness.state.market.advanceFx() }

        assertEquals("3.742", harness.exchange(token, exchangeBody(quoteId)).json().string("rate"))
    }

    @Test
    fun `expired quote is rejected with a fresh one`() {
        val quoteId = harness.quote(token, ttlSeconds = 5).string("quoteId")
        harness.state.market.advanceFx()
        harness.clock.now = harness.clock.now.plusSeconds(5)

        val response = harness.exchange(token, exchangeBody(quoteId))
        val fresh = response.json().obj("quote")

        assertEquals(422, response.status)
        assertEquals("QUOTE_EXPIRED", response.problemCode())
        assertNotEquals(quoteId, fresh.string("quoteId"))
        assertEquals(harness.state.market.fx().tick.buy.toPlainString(), fresh.string("buy"))
        assertEquals("2026-11-07T10:00:10-05:00", fresh.string("expiresAt"))
        assertEquals("1050.20", harness.balance(token, "ACC-003"))
        assertEquals(201, harness.exchange(token, exchangeBody(fresh.string("quoteId"))).status)
    }

    @Test
    fun `quote is valid until just before it expires`() {
        val quoteId = harness.quote(token, ttlSeconds = 5).string("quoteId")
        harness.clock.now = harness.clock.now.plusMillis(4_999)

        assertEquals(201, harness.exchange(token, exchangeBody(quoteId)).status)
    }

    @Test
    fun `accounts in the same currency are a currency mismatch`() {
        val quoteId = harness.quote(token).string("quoteId")

        assertProblem(
            harness.exchange(token, exchangeBody(quoteId, "ACC-001", "ACC-002", "10", "PEN")),
            "CURRENCY_MISMATCH"
        )
        assertProblem(harness.exchange(token, exchangeBody(quoteId, "ACC-003", "ACC-003")), "CURRENCY_MISMATCH")
    }

    @Test
    fun `amount must be in the source currency`() {
        val quoteId = harness.quote(token).string("quoteId")

        assertProblem(harness.exchange(token, exchangeBody(quoteId, currency = "PEN")), "CURRENCY_MISMATCH")
    }

    @Test
    fun `amount over the source balance is insufficient funds`() {
        val quoteId = harness.quote(token).string("quoteId")

        assertProblem(harness.exchange(token, exchangeBody(quoteId, value = "1050.21")), "INSUFFICIENT_FUNDS")
        assertEquals(201, harness.exchange(token, exchangeBody(quoteId, value = "1050.20"), key = "all").status)
        assertEquals("0.00", harness.balance(token, "ACC-003"))
    }

    @Test
    fun `an amount worth less than a cent is rejected`() {
        val quoteId = harness.quote(token).string("quoteId")

        val response = harness.exchange(token, exchangeBody(quoteId, "ACC-002", "ACC-003", "0.01", "PEN"))

        assertEquals(listOf("amount.value"), response.errorFields())
    }

    @Test
    fun `unknown quote and foreign accounts are validation errors`() {
        val quoteId = harness.quote(token).string("quoteId")

        assertEquals(listOf("quoteId"), harness.exchange(token, exchangeBody("QTE-999999")).errorFields())
        assertEquals(
            listOf("sourceAccountId"),
            harness.exchange(token, exchangeBody(quoteId, source = "ACC-404")).errorFields()
        )
        assertEquals(
            listOf("destinationAccountId"),
            harness.exchange(token, exchangeBody(quoteId, destination = "ACC-404")).errorFields()
        )
    }

    @Test
    fun `invalid body is reported per field`() {
        assertEquals(
            listOf("quoteId", "sourceAccountId", "destinationAccountId", "amount.value", "amount.currency"),
            harness.exchange(token, "{}").errorFields()
        )
        assertEquals(listOf("amount.value"), harness.exchange(token, exchangeBody("Q", value = "0")).errorFields())
        assertEquals(listOf("amount.value"), harness.exchange(token, exchangeBody("Q", value = "1.234")).errorFields())
        assertEquals(
            listOf("amount.currency"),
            harness.exchange(token, exchangeBody("Q", currency = "usd")).errorFields()
        )
        assertEquals(listOf("Idempotency-Key"), harness.exchange(token, exchangeBody("Q"), key = null).errorFields())
    }

    @Test
    fun `repeated idempotency key returns the same exchange without moving money twice`() {
        val quoteId = harness.quote(token).string("quoteId")

        val first = harness.exchange(token, exchangeBody(quoteId), key = "same")
        harness.clock.now = harness.clock.now.plusSeconds(60)
        val second = harness.exchange(token, exchangeBody(quoteId), key = "same")

        assertEquals(first, second)
        assertEquals("950.20", harness.balance(token, "ACC-003"))
    }

    @Test
    fun `fx endpoints need a session`() {
        assertEquals(401, harness.get("fx/rates").status)
        assertEquals(401, harness.call("POST", "fx/exchanges", body = exchangeBody("Q")).status)
    }

    @Test
    fun `reset forgets quotes and restores both balances`() {
        val quoteId = harness.quote(token).string("quoteId")
        harness.exchange(token, exchangeBody(quoteId))

        harness.call("POST", "_demo/reset")

        assertEquals("1050.20", harness.balance(token, "ACC-003"))
        assertEquals("2300.00", harness.balance(token, "ACC-002"))
        assertEquals(listOf("quoteId"), harness.exchange(token, exchangeBody(quoteId), key = "new").errorFields())
        assertEquals("QTE-000001", harness.quote(token).string("quoteId"))
    }

    private fun money(value: String, currency: String) = money(value.toBigDecimal(), currency)

    private fun assertProblem(response: BackendResponse, code: String) {
        assertEquals(422, response.status)
        assertEquals(code, response.problemCode())
    }
}
