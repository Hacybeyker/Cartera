package com.hacybeyker.cartera.demo.backend

import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OrderDeskTest {
    private val harness = RouterHarness()
    private val token = harness.login()

    @Test
    fun `reference purchase of two ANDM`() {
        val response = harness.order(token, orderBody("ANDM", "BUY", "2"))
        val body = response.json()

        assertEquals(201, response.status)
        assertEquals("ORD-000001", body.string("orderId"))
        assertEquals("70000001", body.string("operationNumber"))
        assertEquals("2026-11-07T10:00:00-05:00", body.string("createdAt"))
        assertEquals("BUY", body.string("side"))
        assertEquals("ANDM", body.string("symbol"))
        assertEquals("2", body.getValue("quantity").jsonPrimitive.content)
        assertEquals(money("42.10"), body.obj("executedPrice"))
        assertEquals(money("84.20"), body.obj("total"))
        assertEquals("ACC-003", body.string("accountId"))
        assertEquals(money("966.00"), body.obj("accountAvailableBalance"))
        assertEquals("MOV-003-O0001", body.string("movementId"))
        assertEquals("12", body.obj("holding").getValue("quantity").jsonPrimitive.content)
        assertEquals(money("40.35"), body.obj("holding").obj("averagePrice"))
        assertEquals("966.00", harness.balance(token, "ACC-003"))
        assertEquals("12" to "40.35", holding("ANDM"))
    }

    @Test
    fun `reference sale of one QORI after the purchase`() {
        harness.order(token, orderBody("ANDM", "BUY", "2"), key = "buy")

        val response = harness.order(token, orderBody("QORI", "SELL", "1"), key = "sell")
        val body = response.json()

        assertEquals(201, response.status)
        assertEquals(money("115.20"), body.obj("total"))
        assertEquals(money("1081.20"), body.obj("accountAvailableBalance"))
        assertEquals("1081.20", harness.balance(token, "ACC-003"))
        assertEquals("1" to "120.00", holding("QORI"))
        assertEquals("ORD-000002", body.string("orderId"))
    }

    @Test
    fun `orders appear as movements of the dollar account`() {
        val buyId = harness.order(token, orderBody("ANDM", "BUY", "2"), key = "buy").json().string("movementId")
        val sellId = harness.order(token, orderBody("QORI", "SELL", "1"), key = "sell").json().string("movementId")

        val newest = harness.get("movements", token, mapOf("accountId" to "ACC-003")).json().items()
            .take(2).map { it.jsonObject }
        val buy = harness.get("movements/$buyId", token).json()
        val breakdown = harness.get("movements/$sellId/breakdown", token).json()

        assertEquals(setOf(buyId, sellId), newest.map { it.string("id") }.toSet())
        assertEquals("Compra de 2 ANDM", buy.string("description"))
        assertEquals("INVESTMENT", buy.string("category"))
        assertEquals("DEBIT", buy.string("type"))
        assertEquals(money("-84.20"), buy.obj("amount"))
        assertEquals("Andina Minerals", buy.obj("counterparty").string("name"))
        assertEquals("1 QORI × 115.20", breakdown.getValue("lines").jsonArray.single().jsonObject.string("concept"))
        assertEquals(money("115.20"), breakdown.getValue("lines").jsonArray.single().jsonObject.obj("amount"))
        val sell = harness.get("movements/$sellId", token).json()
        assertEquals("Venta de 1 QORI", sell.string("description"))
        assertEquals("CREDIT", sell.string("type"))
    }

    @Test
    fun `orders run at the current server price`() {
        harness.state.market.advance("ANDM")
        val price = harness.state.market.price("ANDM")

        val body = harness.order(token, orderBody("ANDM", "BUY", "1")).json()

        assertEquals(money(price.toPlainString()), body.obj("executedPrice"))
        assertEquals(money(price.toPlainString()), body.obj("total"))
    }

    @Test
    fun `buying a stock not held opens a holding at the execution price`() {
        harness.order(token, orderBody("LIMA", "BUY", "3"))

        assertEquals("3" to "9.85", holding("LIMA"))
    }

    @Test
    fun `selling every share removes the holding`() {
        val body = harness.order(token, orderBody("QORI", "SELL", "2")).json()

        assertEquals("0", body.obj("holding").getValue("quantity").jsonPrimitive.content)
        assertEquals(JsonNull, body.obj("holding").getValue("averagePrice"))
        assertNull(holdingOrNull("QORI"))
    }

    @Test
    fun `repeated idempotency key returns the same order without charging twice`() {
        val first = harness.order(token, orderBody(), key = "same")
        val second = harness.order(token, orderBody(), key = "same")
        val otherBody = harness.order(token, orderBody(quantity = "5"), key = "same")

        assertEquals(first, second)
        assertEquals(first, otherBody)
        assertEquals("966.00", harness.balance(token, "ACC-003"))
    }

    @Test
    fun `idempotency keys of orders and transfers do not collide`() {
        harness.transfer(token, transferBody(), key = "shared")

        val order = harness.order(token, orderBody(), key = "shared")

        assertEquals("ORD-000001", order.json().string("orderId"))
    }

    @Test
    fun `different keys buy twice`() {
        harness.order(token, orderBody(), key = "a")
        harness.order(token, orderBody(), key = "b")

        assertEquals("881.80", harness.balance(token, "ACC-003"))
        assertEquals("14" to "40.60", holding("ANDM"))
    }

    @Test
    fun `purchase over the available balance is insufficient funds`() {
        val response = harness.order(token, orderBody("QORI", "BUY", "10"))

        assertProblem(response, "INSUFFICIENT_FUNDS")
        assertEquals("1050.20", harness.balance(token, "ACC-003"))
    }

    @Test
    fun `selling the whole position credits it and closes the holding`() {
        harness.order(token, orderBody("ANDM", "BUY", "2"), key = "a")
        harness.order(token, orderBody("ANDM", "SELL", "12"), key = "b")

        assertEquals("1471.20", harness.balance(token, "ACC-003"))
        assertNull(holdingOrNull("ANDM"))
    }

    @Test
    fun `selling more than held is insufficient holdings`() {
        assertProblem(harness.order(token, orderBody("QORI", "SELL", "3")), "INSUFFICIENT_HOLDINGS")
        assertProblem(harness.order(token, orderBody("LIMA", "SELL", "1")), "INSUFFICIENT_HOLDINGS")
        assertEquals("2" to "120.00", holding("QORI"))
    }

    @Test
    fun `failed order keeps its key free for a retry`() {
        harness.order(token, orderBody("QORI", "BUY", "10"), key = "retry")

        assertEquals(201, harness.order(token, orderBody("QORI", "BUY", "1"), key = "retry").status)
    }

    @Test
    fun `invalid fields are reported per field`() {
        assertEquals(listOf("symbol"), harness.order(token, orderBody(symbol = "XXXX")).errorFields())
        assertEquals(listOf("side"), harness.order(token, orderBody(side = "HOLD")).errorFields())
        assertEquals(listOf("quantity"), harness.order(token, orderBody(quantity = "0")).errorFields())
        assertEquals(listOf("quantity"), harness.order(token, orderBody(quantity = "-1")).errorFields())
        assertEquals(listOf("quantity"), harness.order(token, orderBody(quantity = "1.5")).errorFields())
        assertEquals(listOf("quantity"), harness.order(token, orderBody(quantity = "\"2\"")).errorFields())
        assertEquals(
            listOf("symbol", "side", "quantity", "accountId"),
            harness.order(token, "{}").errorFields()
        )
    }

    @Test
    fun `account must be own and in dollars`() {
        val soles = harness.order(token, orderBody(account = "ACC-002"))
        val foreign = harness.order(token, orderBody(account = "ACC-404"))

        assertProblem(soles, "VALIDATION_ERROR")
        assertEquals(
            "UNSUPPORTED_CURRENCY",
            soles.json().getValue("errors").jsonArray.single().jsonObject.string("code")
        )
        assertEquals(listOf("accountId"), foreign.errorFields())
    }

    @Test
    fun `missing idempotency key and session are rejected`() {
        assertEquals(listOf("Idempotency-Key"), harness.order(token, orderBody(), key = null).errorFields())
        assertEquals(401, harness.call("POST", "orders", body = orderBody()).status)
    }

    @Test
    fun `reset restores balance, holdings and numbering`() {
        harness.order(token, orderBody(), key = "same")

        harness.call("POST", "_demo/reset")

        assertEquals("1050.20", harness.balance(token, "ACC-003"))
        assertEquals("10" to "40.00", holding("ANDM"))
        assertEquals("ORD-000001", harness.order(token, orderBody(), key = "same").json().string("orderId"))
    }

    private fun holdingOrNull(symbol: String): JsonObject? = harness.get("portfolio", token).json()
        .getValue("holdings").jsonArray.map { it.jsonObject }.firstOrNull { it.string("symbol") == symbol }

    private fun holding(symbol: String): Pair<String, String> {
        val holding = requireNotNull(holdingOrNull(symbol))
        return holding.getValue("quantity").jsonPrimitive.content to holding.obj("averagePrice").string("value")
    }

    private fun money(value: String) = money(value.toBigDecimal(), "USD")

    private fun assertProblem(response: BackendResponse, code: String) {
        assertEquals(422, response.status)
        assertEquals(code, response.problemCode())
    }
}
