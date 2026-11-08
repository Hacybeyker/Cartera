package com.hacybeyker.cartera.legacy.flow

import com.hacybeyker.cartera.legacy.core.domain.Money
import com.hacybeyker.cartera.legacy.core.domain.PEN
import com.hacybeyker.cartera.legacy.flow.domain.PaymentFlow
import com.hacybeyker.cartera.legacy.testing.Samples
import com.hacybeyker.cartera.legacy.testing.pen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PaymentFlowTest {
    private val flow = PaymentFlow()

    private fun fillAll() {
        flow.chooseBiller(Samples.luz)
        flow.chooseCustomerCode("100200")
        flow.chooseDebts("Ana Torres", Samples.luzDebts.debts)
        flow.chooseAccount(Samples.gastos)
    }

    @Test
    fun `a new flow is not started and has nothing chosen`() {
        assertFalse(flow.isStarted)
        assertNull(flow.biller)
        assertEquals(Money.zero(PEN), flow.total)
    }

    @Test
    fun `starting marks the flow as started`() {
        flow.start()

        assertTrue(flow.isStarted)
    }

    @Test
    fun `each choice is kept and the total is the sum of the chosen debts`() {
        fillAll()

        assertEquals(Samples.luz, flow.biller)
        assertEquals("100200", flow.customerCode)
        assertEquals("Ana Torres", flow.customerName)
        assertEquals(Samples.gastos, flow.account)
        assertEquals(pen("177.50"), flow.total)
    }

    @Test
    fun `choosing another biller drops everything that came after it`() {
        fillAll()

        flow.chooseBiller(Samples.agua)

        assertEquals(Samples.agua, flow.biller)
        assertNull(flow.customerCode)
        assertNull(flow.customerName)
        assertNull(flow.account)
        assertEquals(emptyList<Any>(), flow.debts)
    }

    @Test
    fun `choosing another customer code drops the debts and the account`() {
        fillAll()

        flow.chooseCustomerCode("300400")

        assertEquals("300400", flow.customerCode)
        assertEquals(emptyList<Any>(), flow.debts)
        assertNull(flow.account)
    }

    @Test
    fun `choosing other debts drops the account`() {
        fillAll()

        flow.chooseDebts("Ana Torres", listOf(Samples.octubre))

        assertNull(flow.account)
        assertEquals(pen("85.40"), flow.total)
    }

    @Test
    fun `a rejected customer code is given back once and keeps what was typed`() {
        flow.chooseBiller(Samples.luz)
        flow.chooseCustomerCode("999999")

        flow.rejectCustomerCode()

        assertEquals("999999", flow.customerCode)
        assertEquals("999999", flow.takeRejectedCustomerCode())
        assertNull(flow.takeRejectedCustomerCode())
    }

    @Test
    fun `choosing a customer code clears an earlier rejection`() {
        flow.chooseBiller(Samples.luz)
        flow.chooseCustomerCode("999999")
        flow.rejectCustomerCode()

        flow.chooseCustomerCode("100200")

        assertNull(flow.takeRejectedCustomerCode())
    }
}
