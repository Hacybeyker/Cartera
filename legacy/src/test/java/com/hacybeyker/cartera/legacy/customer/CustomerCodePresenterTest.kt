package com.hacybeyker.cartera.legacy.customer

import com.hacybeyker.cartera.legacy.customer.domain.ValidateCustomerCodeUseCase
import com.hacybeyker.cartera.legacy.flow.domain.PaymentFlow
import com.hacybeyker.cartera.legacy.testing.RecordingCustomerCodeView
import com.hacybeyker.cartera.legacy.testing.Samples
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CustomerCodePresenterTest {
    private val flow = PaymentFlow().apply { chooseBiller(Samples.luz) }
    private val presenter = CustomerCodePresenter(flow, ValidateCustomerCodeUseCase())
    private val view = RecordingCustomerCodeView()

    @Test
    fun `it shows the biller and an empty field`() {
        presenter.attach(view)

        assertEquals(CustomerCodeViewState(billerName = "Luz del Norte", code = ""), view.last)
    }

    @Test
    fun `a valid code is kept in the flow and opens the debts`() {
        presenter.attach(view)
        presenter.onCodeChanged("100200")

        presenter.onContinue()

        assertEquals("100200", flow.customerCode)
        assertEquals(listOf("showDebts"), view.events)
        assertNull(view.last.problem)
    }

    @Test
    fun `surrounding spaces do not count`() {
        presenter.attach(view)
        presenter.onCodeChanged(" 100200 ")

        presenter.onContinue()

        assertEquals("100200", flow.customerCode)
    }

    @Test
    fun `an empty code asks for it under the field and does not navigate`() {
        presenter.attach(view)

        presenter.onContinue()

        assertEquals(CodeProblem.EMPTY, view.last.problem)
        assertEquals(emptyList<String>(), view.events)
        assertNull(flow.customerCode)
    }

    @Test
    fun `a code that does not fit the format of the biller is explained and keeps what was typed`() {
        presenter.attach(view)
        presenter.onCodeChanged("1002")

        presenter.onContinue()

        assertEquals(CodeProblem.INVALID_FORMAT, view.last.problem)
        assertEquals("1002", view.last.code)
        assertEquals(emptyList<String>(), view.events)
    }

    @Test
    fun `editing the code clears the problem and the same text changes nothing`() {
        presenter.attach(view)
        presenter.onCodeChanged("1002")
        presenter.onContinue()
        val shown = view.states.size

        presenter.onCodeChanged("1002")
        assertEquals(shown, view.states.size)
        presenter.onCodeChanged("10020")

        assertNull(view.last.problem)
        assertEquals("10020", view.last.code)
    }

    @Test
    fun `coming back from a customer the server does not know shows it under the field with the typed code`() {
        presenter.attach(view)
        presenter.onCodeChanged("999999")
        presenter.onContinue()
        presenter.detach()
        flow.rejectCustomerCode()
        val returned = RecordingCustomerCodeView()

        presenter.attach(returned)

        assertEquals(CodeProblem.UNKNOWN_CUSTOMER, returned.last.problem)
        assertEquals("999999", returned.last.code)
        assertNull(flow.takeRejectedCustomerCode())
    }

    @Test
    fun `the rejection is shown once and the next view shows no problem`() {
        presenter.attach(view)
        presenter.onCodeChanged("999999")
        presenter.onContinue()
        presenter.detach()
        flow.rejectCustomerCode()
        presenter.attach(RecordingCustomerCodeView())
        presenter.detach()
        val third = RecordingCustomerCodeView()

        presenter.attach(third)

        assertEquals(CodeProblem.UNKNOWN_CUSTOMER, third.last.problem)
        presenter.onCodeChanged("100200")
        assertNull(third.last.problem)
    }

    @Test
    fun `a code chosen earlier is shown again`() {
        flow.chooseCustomerCode("100200")
        val again = CustomerCodePresenter(flow, ValidateCustomerCodeUseCase())

        again.attach(view)

        assertEquals("100200", view.last.code)
    }
}
