package com.hacybeyker.cartera.legacy.payment

import com.hacybeyker.cartera.legacy.payment.data.dto.toRequest
import com.hacybeyker.cartera.legacy.payment.domain.PaymentOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class PaymentOrderTest {
    private val order = PaymentOrder("BIL-LUZ", "100200", listOf("DEB-1", "DEB-2"), "ACC-002", "1234", "key-1")

    @Test
    fun `printing the order never shows the pin`() {
        assertFalse(order.toString().contains("1234"))
        assertFalse(order.toString().contains("pin", ignoreCase = true))
        assertEquals(
            "PaymentOrder(billerId=BIL-LUZ, debtIds=[DEB-1, DEB-2], accountId=ACC-002, idempotencyKey=key-1)",
            order.toString()
        )
    }

    @Test
    fun `the request carries the five fields of the contract`() {
        val request = order.toRequest()

        assertEquals("BIL-LUZ", request.billerId)
        assertEquals("100200", request.customerCode)
        assertEquals(listOf("DEB-1", "DEB-2"), request.debtIds)
        assertEquals("ACC-002", request.accountId)
        assertEquals("1234", request.pin)
    }
}
