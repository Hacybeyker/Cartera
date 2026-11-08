package com.hacybeyker.cartera.legacy.billers

import com.hacybeyker.cartera.legacy.billers.domain.Biller
import com.hacybeyker.cartera.legacy.core.domain.LegacyError
import com.hacybeyker.cartera.legacy.core.mvp.LoadPhase
import com.hacybeyker.cartera.legacy.flow.domain.PaymentFlow
import com.hacybeyker.cartera.legacy.testing.FakeBillerRepository
import com.hacybeyker.cartera.legacy.testing.RecordingBillersView
import com.hacybeyker.cartera.legacy.testing.RecordingMarker
import com.hacybeyker.cartera.legacy.testing.Samples
import com.hacybeyker.cartera.legacy.testing.TestSchedulerProvider
import com.hacybeyker.cartera.legacy.testing.failure
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.subjects.SingleSubject
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class BillersPresenterTest {
    private val schedulers = TestSchedulerProvider()
    private val repository = FakeBillerRepository().apply { respond = { Single.just(Samples.billers) } }
    private val flow = PaymentFlow()
    private val marker = RecordingMarker()
    private val presenter = BillersPresenter(repository, flow, marker, schedulers)
    private val view = RecordingBillersView()

    private fun settle() = schedulers.scheduler.triggerActions()

    private fun advance(millis: Long) = schedulers.scheduler.advanceTimeBy(millis, TimeUnit.MILLISECONDS)

    @Test
    fun `it shows loading and then every biller under its category`() {
        presenter.attach(view)
        assertEquals(LoadPhase.LOADING, view.last.phase)

        settle()

        assertEquals(LoadPhase.CONTENT, view.last.phase)
        assertEquals(
            listOf(
                BillerRow.Header("Electricidad"),
                BillerRow.Item("BIL-LUZ", "Luz del Norte"),
                BillerRow.Item("BIL-ESI", "Electro Sierra"),
                BillerRow.Header("Agua"),
                BillerRow.Item("BIL-AGUA", "Agua Trujillo"),
                BillerRow.Header("Telefonía"),
                BillerRow.Item("BIL-TEL", "Telefonía Andina")
            ),
            view.last.rows
        )
        assertEquals(listOf(""), repository.requests)
    }

    @Test
    fun `the first load goes out at once, without waiting for the typing pause`() {
        presenter.attach(view)

        settle()

        assertEquals(LoadPhase.CONTENT, view.last.phase)
    }

    @Test
    fun `no result is an empty state`() {
        repository.respond = { Single.just(emptyList()) }

        presenter.attach(view)
        settle()

        assertEquals(LoadPhase.EMPTY, view.last.phase)
        assertEquals(emptyList<BillerRow>(), view.last.rows)
    }

    @Test
    fun `the content mark is reported once, when the loaded list shows for the first time`() {
        presenter.attach(view)
        assertEquals(emptyList<String>(), marker.marks)

        settle()
        presenter.onQueryChanged("agua")
        advance(300)
        presenter.detach()
        presenter.attach(RecordingBillersView())

        assertEquals(listOf("legacy_empresas"), marker.marks)
    }

    @Test
    fun `typing waits for a pause of 300 ms and searches only the last text`() {
        presenter.attach(view)
        settle()

        presenter.onQueryChanged("a")
        advance(100)
        presenter.onQueryChanged("ag")
        advance(100)
        presenter.onQueryChanged("agu")
        advance(299)
        assertEquals(listOf(""), repository.requests)

        advance(1)

        assertEquals(listOf("", "agu"), repository.requests)
        assertEquals("agu", view.last.query)
    }

    @Test
    fun `the rows say which query they answer, and a reload keeps it`() {
        presenter.attach(view)
        assertNull(view.last.resultsFor)
        settle()
        assertEquals("", view.last.resultsFor)

        repository.respond = { Single.never() }
        presenter.onQueryChanged("agua")
        advance(300)
        assertEquals("", view.last.resultsFor)

        repository.respond = { Single.just(listOf(Samples.agua)) }
        presenter.onRetry()
        settle()
        assertEquals("agua", view.last.resultsFor)
    }

    @Test
    fun `a new search cancels the one still in flight`() {
        val first = SingleSubject.create<List<Biller>>()
        val second = SingleSubject.create<List<Biller>>()
        presenter.attach(view)
        settle()
        repository.respond = { query -> if (query == "a") first else second }

        presenter.onQueryChanged("a")
        advance(300)
        presenter.onQueryChanged("ab")
        advance(300)

        assertFalse(first.hasObservers())
        second.onSuccess(listOf(Samples.agua))
        settle()
        first.onSuccess(listOf(Samples.luz))
        settle()
        assertEquals(listOf(BillerRow.Header("Agua"), BillerRow.Item("BIL-AGUA", "Agua Trujillo")), view.last.rows)
    }

    @Test
    fun `the search keeps the previous list while it loads`() {
        presenter.attach(view)
        settle()
        repository.respond = { Single.never() }

        presenter.onQueryChanged("agua")
        advance(300)

        assertEquals(LoadPhase.LOADING, view.last.phase)
        assertEquals(7, view.last.rows.size)
    }

    @Test
    fun `the text is trimmed for the search and the same search is not repeated`() {
        presenter.attach(view)
        settle()

        presenter.onQueryChanged(" luz ")
        advance(300)
        presenter.onQueryChanged(" luz")
        advance(300)

        assertEquals(listOf("", "luz"), repository.requests)
        assertEquals(" luz", view.last.query)
    }

    @Test
    fun `going back to the text that is already shown does not search again`() {
        presenter.attach(view)
        settle()

        presenter.onQueryChanged("a")
        advance(100)
        presenter.onQueryChanged("")
        advance(300)

        assertEquals(listOf(""), repository.requests)
    }

    @Test
    fun `the same text arriving again from the view is ignored`() {
        presenter.attach(view)
        settle()
        val rendered = view.states.size

        presenter.onQueryChanged("")

        assertEquals(rendered, view.states.size)
    }

    @Test
    fun `a network error shows the error and retry loads again with the same text`() {
        repository.respond = { Single.error(failure(LegacyError.Network)) }
        presenter.attach(view)
        settle()
        assertEquals(LoadPhase.FAILED, view.last.phase)
        assertEquals(LegacyError.Network, view.last.error)

        repository.respond = { Single.just(Samples.billers) }
        presenter.onRetry()
        assertEquals(LoadPhase.LOADING, view.last.phase)
        assertNull(view.last.error)
        settle()

        assertEquals(LoadPhase.CONTENT, view.last.phase)
        assertEquals(listOf("", ""), repository.requests)
    }

    @Test
    fun `a server error while searching retries the text that was searched`() {
        presenter.attach(view)
        settle()
        repository.respond = { Single.error(failure(LegacyError.Server)) }
        presenter.onQueryChanged("agua")
        advance(300)
        assertEquals(LegacyError.Server, view.last.error)

        repository.respond = { Single.just(listOf(Samples.agua)) }
        presenter.onRetry()
        settle()

        assertEquals(listOf("", "agua", "agua"), repository.requests)
        assertEquals(LoadPhase.CONTENT, view.last.phase)
    }

    @Test
    fun `cancelling the error goes back`() {
        repository.respond = { Single.error(failure(LegacyError.Network)) }
        presenter.attach(view)
        settle()

        presenter.onCancel()

        assertNull(view.last.error)
        assertEquals(listOf("goBack"), view.events)
    }

    @Test
    fun `an expired session closes the module without an error dialog`() {
        repository.respond = { Single.error(failure(LegacyError.SessionExpired)) }

        presenter.attach(view)
        settle()

        assertEquals(listOf("close"), view.events)
        assertNull(view.last.error)
    }

    @Test
    fun `choosing a biller keeps it in the flow and opens the customer code`() {
        presenter.attach(view)
        settle()

        presenter.onBillerSelected("BIL-AGUA")

        assertEquals(Samples.agua, flow.biller)
        assertEquals(listOf("showCustomerCode"), view.events)
    }

    @Test
    fun `an id that is not in the list chooses nothing`() {
        presenter.attach(view)
        settle()

        presenter.onBillerSelected("BIL-NADA")

        assertNull(flow.biller)
        assertEquals(emptyList<String>(), view.events)
    }

    @Test
    fun `the list survives the view and is given to the next one`() {
        presenter.attach(view)
        settle()
        presenter.detach()
        val next = RecordingBillersView()

        presenter.attach(next)

        assertEquals(view.last, next.last)
        assertEquals(listOf(""), repository.requests)
    }

    @Test
    fun `destroying the presenter cancels the pending search`() {
        val pending = SingleSubject.create<List<Biller>>()
        repository.respond = { pending }
        presenter.attach(view)
        settle()

        presenter.destroy()

        assertFalse(pending.hasObservers())
    }
}
