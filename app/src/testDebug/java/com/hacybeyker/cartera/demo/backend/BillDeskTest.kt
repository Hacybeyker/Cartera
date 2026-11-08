package com.hacybeyker.cartera.demo.backend

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class BillDeskTest {
    private val harness = RouterHarness()
    private val token = harness.login()

    @Test
    fun `billers list the twenty companies with the reference ones first in their category`() {
        val response = harness.get("billers", token)
        val billers = response.json().items().map { it.jsonObject }

        assertEquals(200, response.status)
        assertEquals(20, billers.size)
        assertEquals(setOf("id", "name", "category", "customerCodeFormat"), billers.first().keys)
        assertEquals(
            listOf("BIL-LUZ", "BIL-AGUA", "BIL-TEL", "BIL-NET"),
            billers.groupBy { it.string("category") }
                .filterKeys { it in listOf("Electricidad", "Agua", "Telefonía", "Internet") }
                .map { (_, inCategory) -> inCategory.first().string("id") }
        )
        assertEquals("^\\d{6}$", billers.first().string("customerCodeFormat"))
        assertEquals(
            setOf("Electricidad", "Agua", "Gas", "Telefonía", "Internet", "Cable", "Seguros", "Educación"),
            billers.map { it.string("category") }.toSet()
        )
    }

    @Test
    fun `billers need a session`() {
        assertEquals(401, harness.get("billers").status)
        assertEquals(401, harness.get("billers/BIL-LUZ/debts", query = mapOf("customerCode" to "100200")).status)
        assertEquals(401, harness.billPayment("", billBody()).status)
        assertEquals(401, harness.get("bill-payments/PAY-000001/voucher").status)
    }

    @Test
    fun `query filters by name or category without case or accents`() {
        assertEquals(listOf("BIL-LUZ"), ids("luz"))
        assertEquals(listOf("BIL-LUZ"), ids("  LUZ DEL norte "))
        assertEquals(listOf("BIL-LUZ", "BIL-ESI", "BIL-ECO"), ids("ELECTRICIDAD"))
        assertEquals(listOf("BIL-TEL", "BIL-MSO", "BIL-TVA"), ids("telefonia"))
        assertEquals(listOf("BIL-TEL", "BIL-MSO", "BIL-TVA"), ids("TELEFONÍA"))
        assertEquals(listOf("BIL-AGUA"), ids("trujillo agua"))
        assertEquals(20, ids("").size)
        assertEquals(emptyList<String>(), ids("zzz"))
    }

    @Test
    fun `luz del norte has two pending bills and no history unless asked`() {
        val response = debts("BIL-LUZ", "100200")
        val body = response.json()

        assertEquals(200, response.status)
        assertEquals("Ana Torres", body.string("customerName"))
        assertEquals(listOf("85.40", "92.10"), body.getValue("debts").jsonArray.map { amount(it.jsonObject) })
        assertEquals(
            listOf("2026-10", "2026-11"),
            body.getValue("debts").jsonArray.map {
                it.jsonObject.string("period")
            }
        )
        assertEquals(setOf("customerName", "debts"), body.keys)
        assertEquals(
            setOf("id", "period", "dueDate", "amount"),
            body.getValue("debts").jsonArray.first().jsonObject.keys
        )
        assertEquals(body, debts("BIL-LUZ", "100200", includeHistory = "false").json())
    }

    @Test
    fun `include history adds twenty four months with their lines`() {
        val response = debts("BIL-LUZ", "100200", includeHistory = "true")
        val history = response.json().getValue("history").jsonArray.map { it.jsonObject }

        assertEquals(24, history.size)
        assertEquals(setOf("period", "amount", "paidAt", "lines"), history.first().keys)
        assertEquals(
            setOf("concept", "amount", "detail"),
            history.first().getValue("lines").jsonArray.first().jsonObject.keys
        )
        assertTrue(requireNotNull(response.body).toByteArray().size > 150_000)
        assertEquals(
            debts("BIL-LUZ", "100200").json().getValue("debts"),
            response.json().getValue("debts")
        )
    }

    @Test
    fun `include history is empty for companies that keep none`() {
        val body = debts("BIL-AGUA", "300400", includeHistory = "true").json()

        assertEquals(0, body.getValue("history").jsonArray.size)
        assertEquals(1, body.getValue("debts").jsonArray.size)
    }

    @Test
    fun `the other reference companies match the reference data`() {
        val water = debts("BIL-AGUA", "300400").json()
        val phone = debts("BIL-TEL", "500600").json()
        val internet = debts("BIL-NET", "700800")

        assertEquals("Familia Torres", water.string("customerName"))
        assertEquals(listOf("46.80"), water.getValue("debts").jsonArray.map { amount(it.jsonObject) })
        assertEquals(listOf("59.90"), phone.getValue("debts").jsonArray.map { amount(it.jsonObject) })
        assertEquals(200, internet.status)
        assertEquals(0, internet.json().getValue("debts").jsonArray.size)
        assertEquals("Ana Torres", internet.json().string("customerName"))
    }

    @Test
    fun `unknown customer and unknown biller are not found`() {
        assertProblem(debts("BIL-LUZ", "999999"), 404, "CUSTOMER_NOT_FOUND")
        assertProblem(debts("BIL-ESI", "12345678"), 404, "CUSTOMER_NOT_FOUND")
        assertProblem(debts("BIL-AGUA", "100200"), 404, "CUSTOMER_NOT_FOUND")
        assertProblem(debts("BIL-404", "100200"), 404, "BILLER_NOT_FOUND")
    }

    @Test
    fun `customer code must follow the format of the company`() {
        val badFormat = debts("BIL-LUZ", "abc")
        val missing = debts("BIL-LUZ", null)
        val badHistory = debts("BIL-LUZ", "100200", includeHistory = "si")
        val both = debts("BIL-LUZ", "12", includeHistory = "si")

        assertProblem(badFormat, 422, "VALIDATION_ERROR")
        assertEquals(listOf("customerCode"), badFormat.errorFields())
        assertEquals(listOf("customerCode"), missing.errorFields())
        assertEquals(listOf("includeHistory"), badHistory.errorFields())
        assertEquals(listOf("customerCode", "includeHistory"), both.errorFields())
        assertEquals(listOf("customerCode"), debts("BIL-ESI", "123456").errorFields())
    }

    @Test
    fun `reference payment debits the whole receipts from cuenta gastos`() {
        val response = harness.billPayment(token, billBody())
        val body = response.json()

        assertEquals(201, response.status)
        assertEquals("PAY-000001", body.string("paymentId"))
        assertEquals("80000001", body.string("operationNumber"))
        assertEquals("2026-11-07T10:00:00-05:00", body.string("createdAt"))
        assertEquals(
            listOf("DEB-LUZ-202610" to "85.40", "DEB-LUZ-202611" to "92.10"),
            body.getValue("paidDebts").jsonArray.map { it.jsonObject.string("id") to amount(it.jsonObject) }
        )
        assertEquals("177.50", body.obj("total").string("value"))
        assertEquals("PEN", body.obj("total").string("currency"))
        assertEquals("ACC-002", body.string("accountId"))
        assertEquals("2122.50", body.obj("accountAvailableBalance").string("value"))
        assertEquals("2122.50", harness.balance(token, "ACC-002"))
        assertEquals("8450.75", harness.balance(token, "ACC-001"))
    }

    @Test
    fun `paid receipts leave the pending list`() {
        harness.billPayment(token, billBody(debtIds = listOf("DEB-LUZ-202610")))

        val pending = debts("BIL-LUZ", "100200").json().getValue("debts").jsonArray

        assertEquals(listOf("DEB-LUZ-202611"), pending.map { it.jsonObject.string("id") })
        harness.billPayment(token, billBody(debtIds = listOf("DEB-LUZ-202611")), key = "bill-2")
        assertEquals(0, debts("BIL-LUZ", "100200").json().getValue("debts").jsonArray.size)
    }

    @Test
    fun `the payment creates a services movement with detail and a breakdown that adds up`() {
        val movementId = harness.billPayment(token, billBody()).json().string("movementId")

        val newest = harness.get("movements", token, mapOf("accountId" to "ACC-002")).json().items().first().jsonObject
        val detail = harness.get("movements/$movementId", token).json()
        val breakdown = harness.get("movements/$movementId/breakdown", token).json()

        assertEquals("MOV-002-P0001", movementId)
        assertEquals(movementId, newest.string("id"))
        assertEquals("Pago de Luz del Norte", newest.string("description"))
        assertEquals("SERVICES", newest.string("category"))
        assertEquals("DEBIT", newest.string("type"))
        assertEquals("-177.50", newest.obj("amount").string("value"))
        assertEquals("80000001", detail.string("operationNumber"))
        assertEquals("100200", detail.obj("counterparty").string("reference"))
        assertEquals("Luz del Norte", detail.obj("counterparty").string("name"))
        assertEquals(
            listOf("Recibo de octubre 2026", "Recibo de noviembre 2026"),
            breakdown.getValue("lines").jsonArray.map { it.jsonObject.string("concept") }
        )
        assertEquals("0.00", breakdown.obj("fees").string("value"))
        assertEquals("SERVICES", breakdown.string("category"))
    }

    @Test
    fun `paying the same receipt again with another key is already paid`() {
        harness.billPayment(token, billBody(), key = "first")

        val again = harness.billPayment(token, billBody(), key = "second")

        assertProblem(again, 422, "DEBT_ALREADY_PAID")
        assertEquals("2122.50", harness.balance(token, "ACC-002"))
    }

    @Test
    fun `a paid receipt in a new batch charges nothing, not even the pending ones`() {
        harness.billPayment(token, billBody(debtIds = listOf("DEB-LUZ-202610")), key = "first")

        val mixed = harness.billPayment(token, billBody(), key = "second")

        assertProblem(mixed, 422, "DEBT_ALREADY_PAID")
        assertEquals("2214.60", harness.balance(token, "ACC-002"))
        assertEquals(1, debts("BIL-LUZ", "100200").json().getValue("debts").jsonArray.size)
    }

    @Test
    fun `a wrong operations pin is invalid and charges nothing`() {
        val response = harness.billPayment(token, billBody(pin = "0000"))

        assertProblem(response, 422, "INVALID_PIN")
        assertEquals("2300.00", harness.balance(token, "ACC-002"))
        assertEquals(2, debts("BIL-LUZ", "100200").json().getValue("debts").jsonArray.size)
    }

    @Test
    fun `the pin is checked before the receipts and the balance`() {
        harness.transfer(token, transferBody(value = "2250.00"), key = "empty")

        assertProblem(harness.billPayment(token, billBody(pin = "0000", debtIds = listOf("DEB-X"))), 422, "INVALID_PIN")
        assertProblem(harness.billPayment(token, billBody(pin = "0000")), 422, "INVALID_PIN")
    }

    @Test
    fun `a total over the available balance is insufficient funds`() {
        harness.transfer(token, transferBody(value = "2200.00"), key = "almost-empty")

        val response = harness.billPayment(token, billBody())

        assertProblem(response, 422, "INSUFFICIENT_FUNDS")
        assertEquals("100.00", harness.balance(token, "ACC-002"))
        assertEquals(2, debts("BIL-LUZ", "100200").json().getValue("debts").jsonArray.size)
    }

    @Test
    fun `the whole balance can pay`() {
        harness.transfer(token, transferBody(value = "2122.50"), key = "to-exact")

        assertEquals(201, harness.billPayment(token, billBody()).status)
        assertEquals("0.00", harness.balance(token, "ACC-002"))
    }

    @Test
    fun `the account must be own and in soles`() {
        val foreign = harness.billPayment(token, billBody(accountId = "ACC-404"))
        val dollars = harness.billPayment(token, billBody(accountId = "ACC-003"))

        assertProblem(foreign, 422, "VALIDATION_ERROR")
        assertEquals(listOf("accountId"), foreign.errorFields())
        assertProblem(dollars, 422, "CURRENCY_MISMATCH")
        assertEquals("1050.20", harness.balance(token, "ACC-003"))
    }

    @Test
    fun `receipts must be the customer's own`() {
        val foreign = harness.billPayment(token, billBody(debtIds = listOf("DEB-AGUA-202610")))
        val missing = harness.billPayment(token, billBody(debtIds = listOf("DEB-LUZ-202610", "DEB-LUZ-209912")))
        val customer = harness.billPayment(token, billBody(customerCode = "999999"))

        assertEquals(listOf("debtIds"), foreign.errorFields())
        assertEquals(listOf("debtIds"), missing.errorFields())
        assertEquals(listOf("customerCode"), customer.errorFields())
        assertEquals("2300.00", harness.balance(token, "ACC-002"))
    }

    @Test
    fun `repeated idempotency key returns the same response without charging twice`() {
        val first = harness.billPayment(token, billBody(), key = "same")
        val second = harness.billPayment(token, billBody(), key = "same")

        assertEquals(first, second)
        assertEquals("2122.50", harness.balance(token, "ACC-002"))
        assertEquals(
            1,
            harness.get("movements", token, mapOf("accountId" to "ACC-002")).json().items().count {
                it.jsonObject.string("id").startsWith("MOV-002-P")
            }
        )
    }

    @Test
    fun `the same key with another body returns the first response, as transfers do`() {
        val first = harness.billPayment(token, billBody(debtIds = listOf("DEB-LUZ-202610")), key = "same")
        val second = harness.billPayment(token, billBody(debtIds = listOf("DEB-LUZ-202611")), key = "same")

        assertEquals(first, second)
        assertEquals("2214.60", harness.balance(token, "ACC-002"))
    }

    @Test
    fun `errors are not stored under the key`() {
        assertProblem(harness.billPayment(token, billBody(pin = "0000"), key = "retry"), 422, "INVALID_PIN")

        val retried = harness.billPayment(token, billBody(), key = "retry")

        assertEquals(201, retried.status)
        assertEquals("80000001", retried.json().string("operationNumber"))
    }

    @Test
    fun `a retry after a lost response does not pay twice`() {
        harness.billPayment(token, billBody(), key = "lost")

        val retry = harness.billPayment(token, billBody(), key = "lost")

        assertEquals("PAY-000001", retry.json().string("paymentId"))
        assertEquals("2122.50", harness.balance(token, "ACC-002"))
        assertNull(harness.state.bills.payment("USR-001", "PAY-000002"))
    }

    @Test
    fun `operation numbers are their own sequence`() {
        harness.transfer(token, transferBody(), key = "transfer")

        val first = harness.billPayment(token, billBody(debtIds = listOf("DEB-LUZ-202610")), key = "a").json()
        val second = harness.billPayment(
            token,
            billBody(billerId = "BIL-AGUA", customerCode = "300400", debtIds = listOf("DEB-AGUA-202610")),
            key = "b"
        ).json()

        assertEquals(listOf("80000001", "80000002"), listOf(first, second).map { it.string("operationNumber") })
        assertEquals(listOf("PAY-000001", "PAY-000002"), listOf(first, second).map { it.string("paymentId") })
    }

    @Test
    fun `body is validated field by field`() {
        assertEquals(
            listOf("billerId", "customerCode", "debtIds", "accountId", "pin"),
            harness.billPayment(token, "{}").errorFields()
        )
        assertEquals(listOf("Idempotency-Key"), harness.billPayment(token, billBody(), key = null).errorFields())
        assertEquals(listOf("billerId"), harness.billPayment(token, billBody(billerId = "BIL-404")).errorFields())
        assertEquals(listOf("customerCode"), harness.billPayment(token, billBody(customerCode = "10020")).errorFields())
        assertEquals(listOf("debtIds"), harness.billPayment(token, billBody(debtIds = emptyList())).errorFields())
        assertEquals(
            listOf("debtIds"),
            harness.billPayment(token, billBody(debtIds = listOf("DEB-LUZ-202610", "DEB-LUZ-202610"))).errorFields()
        )
        assertEquals(listOf("pin"), harness.billPayment(token, billBody(pin = "12a4")).errorFields())
        assertEquals(listOf("pin"), harness.billPayment(token, billBody(pin = "12345")).errorFields())
        assertEquals(
            listOf("debtIds"),
            harness.billPayment(token, billBody().replace("[\"DEB-LUZ-202610\",\"DEB-LUZ-202611\"]", "[1,2]"))
                .errorFields()
        )
        assertEquals(
            listOf("debtIds"),
            harness.billPayment(token, billBody().replace("[\"DEB-LUZ-202610\",\"DEB-LUZ-202611\"]", "\"x\""))
                .errorFields()
        )
        assertEquals("2300.00", harness.balance(token, "ACC-002"))
    }

    @Test
    fun `reset returns payments, receipts, numbering and idempotency to the fixtures`() {
        val paymentId = harness.billPayment(token, billBody(), key = "same").json().string("paymentId")

        harness.call("POST", "_demo/reset")

        assertEquals("2300.00", harness.balance(token, "ACC-002"))
        assertEquals(2, debts("BIL-LUZ", "100200").json().getValue("debts").jsonArray.size)
        assertProblem(harness.get("bill-payments/$paymentId/voucher", token), 404, "PAYMENT_NOT_FOUND")
        val again = harness.billPayment(token, billBody(), key = "same")
        assertEquals(201, again.status)
        assertEquals("80000001", again.json().string("operationNumber"))
        assertEquals("2122.50", harness.balance(token, "ACC-002"))
    }

    @Test
    fun `voucher is html with the operation, the receipts, the total and the new balance`() {
        val paymentId = harness.billPayment(token, billBody()).json().string("paymentId")

        val response = harness.get("bill-payments/$paymentId/voucher", token)
        val html = requireNotNull(response.body)

        assertEquals(200, response.status)
        assertEquals("text/html", response.contentType)
        listOf(
            "Constancia de pago",
            "80000001",
            "07/11/2026 10:00",
            "Luz del Norte",
            "Ana Torres",
            "100200",
            "Cuenta Gastos ****9034",
            "Octubre 2026",
            "S/ 85.40",
            "Noviembre 2026",
            "S/ 92.10",
            "Total pagado",
            "S/ 177.50",
            "S/ 2,122.50"
        ).forEach { assertTrue(it, ">$it<" in html) }
        assertFalse(" id=" in html)
        assertFalse("<script" in html.lowercase())
        assertTrue(html.startsWith("<!DOCTYPE html>"))
        assertTrue("""<html lang="es">""" in html)
    }

    @Test
    fun `voucher does not change when the account moves afterwards`() {
        val paymentId = harness.billPayment(token, billBody()).json().string("paymentId")
        val before = harness.get("bill-payments/$paymentId/voucher", token).body

        harness.transfer(token, transferBody(), key = "later")

        assertEquals(before, harness.get("bill-payments/$paymentId/voucher", token).body)
    }

    @Test
    fun `an unknown payment, or one of another person, has no voucher`() {
        val paymentId = harness.billPayment(token, billBody()).json().string("paymentId")

        assertProblem(harness.get("bill-payments/PAY-999999/voucher", token), 404, "PAYMENT_NOT_FOUND")
        val other = assertThrows(ProblemException::class.java) { harness.desks.bills.voucher("USR-002", paymentId) }
        assertEquals(ProblemCode.PAYMENT_NOT_FOUND, other.code)
    }

    @Test
    fun `problems for the new endpoints follow rfc 9457`() {
        val body = debts("BIL-LUZ", "999999").json()

        assertEquals("https://cartera.invalid/problems/customer-not-found", body.string("type"))
        assertEquals(404, body.getValue("status").toString().toInt())
        assertEquals("req-test", body.string("traceId"))
    }

    private fun ids(query: String): List<String> =
        harness.get("billers", token, mapOf("query" to query)).json().items().map { it.jsonObject.string("id") }

    private fun debts(billerId: String, customerCode: String?, includeHistory: String? = null): BackendResponse {
        val query = listOfNotNull(
            customerCode?.let { "customerCode" to it },
            includeHistory?.let { "includeHistory" to it }
        ).toMap()
        return harness.get("billers/$billerId/debts", token, query)
    }

    private fun amount(json: JsonObject): String = json.obj("amount").string("value")

    private fun assertProblem(response: BackendResponse, status: Int, code: String) {
        assertEquals(status, response.status)
        assertEquals(code, response.json().string("code"))
    }
}
