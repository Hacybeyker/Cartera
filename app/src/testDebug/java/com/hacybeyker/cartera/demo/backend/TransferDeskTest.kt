package com.hacybeyker.cartera.demo.backend

import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Test

class TransferDeskTest {
    private val harness = RouterHarness()
    private val token = harness.login()

    @Test
    fun `reference transfer debits the source and prepends the movement`() {
        val response = harness.transfer(token, transferBody())
        val body = response.json()

        assertEquals(201, response.status)
        assertEquals("TRF-000001", body.string("transferId"))
        assertEquals("90000001", body.string("operationNumber"))
        assertEquals("2026-11-07T10:00:00-05:00", body.string("createdAt"))
        assertEquals("2150.00", body.obj("sourceAvailableBalance").string("value"))
        assertEquals("2150.00", accountBalance("ACC-002"))
        val newest = harness.get("movements", token, mapOf("accountId" to "ACC-002")).json().items().first().jsonObject
        assertEquals(body.string("movementId"), newest.string("id"))
        assertEquals("-150.00", newest.obj("amount").string("value"))
        assertEquals("DEBIT", newest.string("type"))
        assertEquals("Transferencia a Luis Ramírez", newest.string("description"))
    }

    @Test
    fun `created movement has detail and a minimal breakdown`() {
        val movementId = harness.transfer(token, transferBody()).json().string("movementId")

        val detail = harness.get("movements/$movementId", token).json()
        val breakdown = harness.get("movements/$movementId/breakdown", token).json()

        assertEquals("999-0000001-0-01", detail.obj("counterparty").string("reference"))
        assertEquals(1, breakdown.getValue("lines").jsonArray.size)
        assertEquals("0.00", breakdown.obj("fees").string("value"))
    }

    @Test
    fun `custom description is kept`() {
        val body = transferBody().removeSuffix("}") + ""","description":"Cena"}"""

        val movementId = harness.transfer(token, body).json().string("movementId")

        assertEquals("Cena", harness.get("movements/$movementId", token).json().string("description"))
    }

    @Test
    fun `repeated idempotency key returns the same response without charging twice`() {
        val first = harness.transfer(token, transferBody(), key = "same")
        val second = harness.transfer(token, transferBody(), key = "same")

        assertEquals(first, second)
        assertEquals("2150.00", accountBalance("ACC-002"))
    }

    @Test
    fun `different keys charge twice`() {
        harness.transfer(token, transferBody(), key = "a")
        harness.transfer(token, transferBody(), key = "b")

        assertEquals("2000.00", accountBalance("ACC-002"))
    }

    @Test
    fun `amount over the available balance is insufficient funds`() {
        assertProblem(harness.transfer(token, transferBody(value = "2300.01")), "INSUFFICIENT_FUNDS")
        assertEquals("2300.00", accountBalance("ACC-002"))
    }

    @Test
    fun `whole balance can be transferred`() {
        assertEquals(201, harness.transfer(token, transferBody(value = "2300")).status)
        assertEquals("0.00", accountBalance("ACC-002"))
    }

    @Test
    fun `pen to a dollar destination is a currency mismatch`() {
        assertProblem(harness.transfer(token, transferBody(destination = "999-0000003-0-10")), "CURRENCY_MISMATCH")
    }

    @Test
    fun `amount currency must match the source`() {
        assertProblem(harness.transfer(token, transferBody(currency = "USD")), "CURRENCY_MISMATCH")
    }

    @Test
    fun `unknown destination is not found`() {
        assertProblem(harness.transfer(token, transferBody(destination = "123")), "DESTINATION_NOT_FOUND")
    }

    @Test
    fun `source must be an own account`() {
        val response = harness.transfer(token, transferBody(source = "ACC-404"))

        assertProblem(response, "VALIDATION_ERROR")
        assertEquals(listOf("sourceAccountId"), fields(response))
    }

    @Test
    fun `missing idempotency key is a validation error`() {
        val response = harness.transfer(token, transferBody(), key = null)

        assertEquals(listOf("Idempotency-Key"), fields(response))
    }

    @Test
    fun `invalid amounts and fields are reported per field`() {
        assertEquals(listOf("amount.value"), fields(harness.transfer(token, transferBody(value = "0"))))
        assertEquals(listOf("amount.value"), fields(harness.transfer(token, transferBody(value = "-5"))))
        assertEquals(listOf("amount.value"), fields(harness.transfer(token, transferBody(value = "1.234"))))
        assertEquals(listOf("amount.currency"), fields(harness.transfer(token, transferBody(currency = "soles"))))
        assertEquals(
            listOf("sourceAccountId", "destinationAccountNumber", "amount.value", "amount.currency"),
            fields(harness.transfer(token, "{}"))
        )
        val longDescription = transferBody().removeSuffix("}") + ""","description":"${"x".repeat(141)}"}"""
        assertEquals(listOf("description"), fields(harness.transfer(token, longDescription)))
    }

    @Test
    fun `reset restores balances, movements and idempotency`() {
        harness.transfer(token, transferBody(), key = "same")

        harness.call("POST", "_demo/reset")

        assertEquals("2300.00", accountBalance("ACC-002"))
        assertEquals(
            "MOV-002-0001",
            harness.get("movements", token, mapOf("accountId" to "ACC-002")).json().items()
                .first().jsonObject.string("id")
        )
        assertEquals("TRF-000001", harness.transfer(token, transferBody(), key = "same").json().string("transferId"))
    }

    @Test
    fun `a transfer between pages does not shift the next page`() {
        val query = mapOf("accountId" to "ACC-002", "limit" to "10")
        val first = harness.get("movements", token, query).json()
        val expected = harness.get("movements", token, query + ("cursor" to first.string("nextCursor"))).json().items()

        harness.transfer(token, transferBody())

        val second = harness.get("movements", token, query + ("cursor" to first.string("nextCursor"))).json().items()
        assertEquals(expected, second)
    }

    @Test
    fun `all accounts list includes the new movement first`() {
        val movementId = harness.transfer(token, transferBody()).json().string("movementId")

        val newest = harness.get("movements", token).json().items().first().jsonObject

        assertEquals(movementId, newest.string("id"))
    }

    private fun accountBalance(accountId: String): String =
        harness.get("accounts/$accountId", token).json().obj("availableBalance").string("value")

    private fun fields(response: BackendResponse): List<String> =
        response.json().getValue("errors").jsonArray.map { it.jsonObject.string("field") }

    private fun assertProblem(response: BackendResponse, code: String) {
        assertEquals(422, response.status)
        assertEquals(code, response.json().string("code"))
    }
}
