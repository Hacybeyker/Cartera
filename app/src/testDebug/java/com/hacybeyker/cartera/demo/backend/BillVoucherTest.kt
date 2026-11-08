package com.hacybeyker.cartera.demo.backend

import java.math.BigDecimal
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BillVoucherTest {

    @Test
    fun `money uses soles with grouping and two decimals whatever the locale`() {
        assertEquals("S/ 2,122.50", BillFormat.money(BigDecimal("2122.5"), "PEN"))
        assertEquals("S/ 0.05", BillFormat.money(BigDecimal("0.05"), "PEN"))
        assertEquals("S/ 1,234,567.00", BillFormat.money(BigDecimal("1234567"), "PEN"))
        assertEquals("USD 10.00", BillFormat.money(BigDecimal("10"), "USD"))
    }

    @Test
    fun `period reads as month and year`() {
        assertEquals("octubre 2026", BillFormat.period("2026-10"))
        assertEquals("enero 2025", BillFormat.period("2025-01"))
        assertEquals("diciembre 2026", BillFormat.period("2026-12"))
        assertEquals("2026-13", BillFormat.period("2026-13"))
        assertEquals("2026-00", BillFormat.period("2026-00"))
        assertEquals("sin período", BillFormat.period("sin período"))
    }

    @Test
    fun `everything that comes from data is escaped`() {
        val html = BillVoucher.render(
            payment(
                billerName = "<script>alert(1)</script>",
                customerName = "Ana & \"Luis\" <b>",
                accountLabel = "O'Brien"
            )
        )

        assertFalse("<script" in html)
        assertTrue("&lt;script&gt;alert(1)&lt;/script&gt;" in html)
        assertTrue("Ana &amp; &quot;Luis&quot; &lt;b&gt;" in html)
        assertTrue("O&#39;Brien" in html)
    }

    @Test
    fun `the page is simple html without ids, scripts or external resources`() {
        val html = BillVoucher.render(payment())

        assertFalse(" id=" in html)
        assertFalse("<script" in html.lowercase())
        assertFalse("http://" in html || "https://" in html)
        assertTrue("<main>" in html && "<table>" in html && "<h1>Constancia de pago</h1>" in html)
        assertTrue("default-src 'none'" in html)
    }

    @Test
    fun `the date is shown in lima time`() {
        val html = BillVoucher.render(payment(createdAt = Instant.parse("2026-11-08T03:30:00Z")))

        assertTrue(">07/11/2026 22:30<" in html)
    }

    private fun payment(
        billerName: String = "Luz del Norte",
        customerName: String = "Ana Torres",
        accountLabel: String = "Cuenta Gastos ****9034",
        createdAt: Instant = Instant.parse("2026-11-07T15:00:00Z")
    ) = BillPayment(
        id = "PAY-000001",
        userId = "USR-001",
        operationNumber = "80000001",
        createdAt = createdAt,
        billerName = billerName,
        customerCode = "100200",
        customerName = customerName,
        paidDebts = listOf(PaidDebt("DEB-LUZ-202610", "2026-10", BigDecimal("85.40"))),
        currency = "PEN",
        accountId = "ACC-002",
        accountLabel = accountLabel,
        newBalance = BigDecimal("2214.60")
    )
}
