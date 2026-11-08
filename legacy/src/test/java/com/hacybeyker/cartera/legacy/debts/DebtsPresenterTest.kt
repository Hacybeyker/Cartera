package com.hacybeyker.cartera.legacy.debts

import com.hacybeyker.cartera.legacy.core.domain.FieldError
import com.hacybeyker.cartera.legacy.core.domain.LegacyError
import com.hacybeyker.cartera.legacy.core.domain.ProblemCode
import com.hacybeyker.cartera.legacy.core.mvp.LoadPhase
import com.hacybeyker.cartera.legacy.debts.domain.CustomerDebts
import com.hacybeyker.cartera.legacy.flow.domain.PaymentFlow
import com.hacybeyker.cartera.legacy.testing.FakeDebtRepository
import com.hacybeyker.cartera.legacy.testing.RecordingDebtsView
import com.hacybeyker.cartera.legacy.testing.RecordingMarker
import com.hacybeyker.cartera.legacy.testing.Samples
import com.hacybeyker.cartera.legacy.testing.TestSchedulerProvider
import com.hacybeyker.cartera.legacy.testing.failure
import com.hacybeyker.cartera.legacy.testing.pen
import io.reactivex.rxjava3.core.Single
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DebtsPresenterTest {
    private val schedulers = TestSchedulerProvider()
    private val repository = FakeDebtRepository().apply { respond = { Single.just(Samples.luzDebts) } }
    private val flow = PaymentFlow().apply {
        chooseBiller(Samples.luz)
        chooseCustomerCode("100200")
    }
    private val marker = RecordingMarker()
    private val presenter = DebtsPresenter(repository, flow, marker, schedulers)
    private val view = RecordingDebtsView()

    private fun settle() = schedulers.scheduler.triggerActions()

    private fun loaded() {
        presenter.attach(view)
        settle()
    }

    @Test
    fun `it asks for the debts of the customer of the biller and shows loading meanwhile`() {
        presenter.attach(view)
        assertEquals(LoadPhase.LOADING, view.last.phase)

        settle()

        assertEquals(listOf("BIL-LUZ" to "100200"), repository.requests)
    }

    @Test
    fun `the debts are listed unchecked with the customer name and a total of zero soles`() {
        loaded()

        assertEquals(LoadPhase.CONTENT, view.last.phase)
        assertEquals("Ana Torres", view.last.customerName)
        assertEquals(
            listOf(
                DebtItem("DEB-LUZ-202610", "Octubre 2026", "12/11/2026", "S/ 85.40", checked = false),
                DebtItem("DEB-LUZ-202611", "Noviembre 2026", "12/12/2026", "S/ 92.10", checked = false)
            ),
            view.last.items
        )
        assertEquals("S/ 0.00", view.last.totalLabel)
        assertFalse(view.last.canContinue)
    }

    @Test
    fun `checking debts adds their exact total and enables continue`() {
        loaded()

        presenter.onDebtToggled("DEB-LUZ-202610")
        assertEquals("S/ 85.40", view.last.totalLabel)
        assertTrue(view.last.canContinue)
        presenter.onDebtToggled("DEB-LUZ-202611")
        assertEquals("S/ 177.50", view.last.totalLabel)
        assertEquals(listOf(true, true), view.last.items.map { it.checked })
    }

    @Test
    fun `unchecking the last debt disables continue again`() {
        loaded()
        presenter.onDebtToggled("DEB-LUZ-202610")

        presenter.onDebtToggled("DEB-LUZ-202610")

        assertEquals("S/ 0.00", view.last.totalLabel)
        assertFalse(view.last.canContinue)
    }

    @Test
    fun `continuing keeps the checked debts in the flow and opens the account`() {
        loaded()
        presenter.onDebtToggled("DEB-LUZ-202611")

        presenter.onContinue()

        assertEquals(listOf(Samples.noviembre), flow.debts)
        assertEquals("Ana Torres", flow.customerName)
        assertEquals(pen("92.10"), flow.total)
        assertEquals(listOf("showAccount"), view.events)
    }

    @Test
    fun `continuing without checked debts does nothing`() {
        loaded()

        presenter.onContinue()

        assertEquals(emptyList<String>(), view.events)
        assertEquals(emptyList<Any>(), flow.debts)
    }

    @Test
    fun `a customer without pending debts is an empty state with a content mark`() {
        repository.respond = { Single.just(CustomerDebts("Ana Torres", emptyList())) }

        loaded()

        assertEquals(LoadPhase.EMPTY, view.last.phase)
        assertEquals("Ana Torres", view.last.customerName)
        assertEquals(listOf("legacy_recibos"), marker.marks)
    }

    @Test
    fun `the content mark is reported once, also after checking debts or returning to the screen`() {
        loaded()
        presenter.onDebtToggled("DEB-LUZ-202610")
        presenter.detach()
        presenter.attach(RecordingDebtsView())

        assertEquals(listOf("legacy_recibos"), marker.marks)
    }

    @Test
    fun `there is no content mark while loading or after an error`() {
        repository.respond = { Single.error(failure(LegacyError.Network)) }

        loaded()

        assertEquals(emptyList<String>(), marker.marks)
    }

    @Test
    fun `an error shows the dialog state and retry loads again`() {
        repository.respond = { Single.error(failure(LegacyError.Server)) }
        loaded()
        assertEquals(LoadPhase.FAILED, view.last.phase)
        assertEquals(LegacyError.Server, view.last.error)

        repository.respond = { Single.just(Samples.luzDebts) }
        presenter.onRetry()
        assertNull(view.last.error)
        settle()

        assertEquals(LoadPhase.CONTENT, view.last.phase)
        assertEquals(2, repository.requests.size)
    }

    @Test
    fun `cancelling the error goes back`() {
        repository.respond = { Single.error(failure(LegacyError.Network)) }
        loaded()

        presenter.onCancel()

        assertNull(view.last.error)
        assertEquals(listOf("goBack"), view.events)
    }

    @Test
    fun `a customer the server does not know returns to the customer code with the rejection in the flow`() {
        repository.respond = { Single.error(failure(rejected(ProblemCode.CUSTOMER_NOT_FOUND))) }

        loaded()

        assertEquals(listOf("returnToCustomerCode"), view.events)
        assertEquals("100200", flow.takeRejectedCustomerCode())
        assertNull(view.last.error)
    }

    @Test
    fun `another rejection is shown as an error and does not return`() {
        repository.respond = { Single.error(failure(rejected(ProblemCode.BILLER_NOT_FOUND))) }

        loaded()

        assertEquals(LoadPhase.FAILED, view.last.phase)
        assertEquals(emptyList<String>(), view.events)
        assertNull(flow.takeRejectedCustomerCode())
    }

    @Test
    fun `an expired session closes the module`() {
        repository.respond = { Single.error(failure(LegacyError.SessionExpired)) }

        loaded()

        assertEquals(listOf("close"), view.events)
        assertNull(view.last.error)
    }

    @Test
    fun `the checked debts survive a new view`() {
        loaded()
        presenter.onDebtToggled("DEB-LUZ-202610")
        presenter.detach()
        val next = RecordingDebtsView()

        presenter.attach(next)

        assertEquals(view.last, next.last)
        assertEquals(1, repository.requests.size)
    }

    private fun rejected(code: ProblemCode) = LegacyError.Rejected(
        status = 404,
        code = code,
        detail = "detalle",
        fieldErrors = emptyList<FieldError>()
    )
}
