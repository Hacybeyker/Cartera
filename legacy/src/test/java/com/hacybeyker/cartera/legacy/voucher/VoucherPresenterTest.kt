package com.hacybeyker.cartera.legacy.voucher

import com.hacybeyker.cartera.legacy.core.domain.LegacyError
import com.hacybeyker.cartera.legacy.core.mvp.LoadPhase
import com.hacybeyker.cartera.legacy.flow.domain.PaymentFlow
import com.hacybeyker.cartera.legacy.testing.FakePaymentRepository
import com.hacybeyker.cartera.legacy.testing.RecordingMarker
import com.hacybeyker.cartera.legacy.testing.RecordingVoucherView
import com.hacybeyker.cartera.legacy.testing.Samples
import com.hacybeyker.cartera.legacy.testing.TestSchedulerProvider
import com.hacybeyker.cartera.legacy.testing.failure
import io.reactivex.rxjava3.core.Single
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VoucherPresenterTest {
    private val schedulers = TestSchedulerProvider()
    private val payments = FakePaymentRepository().apply { respondVoucher = { Single.just(Samples.voucherHtml) } }
    private val flow = PaymentFlow().apply { completePayment(Samples.payment) }
    private val marker = RecordingMarker()
    private val presenter = VoucherPresenter(payments, flow, marker, schedulers)
    private val view = RecordingVoucherView()

    private fun settle() = schedulers.scheduler.triggerActions()

    @Test
    fun `it loads the voucher of the payment that was just made`() {
        presenter.attach(view)
        assertEquals(LoadPhase.LOADING, view.last.phase)

        settle()

        assertEquals(listOf("PAY-000001"), payments.vouchers)
        assertEquals(LoadPhase.CONTENT, view.last.phase)
        assertEquals(Samples.voucherHtml, view.last.html)
    }

    @Test
    fun `the content mark waits for the page to finish loading and is reported once`() {
        presenter.attach(view)
        settle()
        assertEquals(emptyList<String>(), marker.marks)

        presenter.onPageFinished()
        presenter.onPageFinished()
        presenter.detach()
        presenter.attach(RecordingVoucherView())
        presenter.onPageFinished()

        assertEquals(listOf("legacy_constancia"), marker.marks)
    }

    @Test
    fun `a page that finishes before the voucher arrived or after it failed is not content`() {
        payments.respondVoucher = { Single.error(failure(LegacyError.Network)) }
        presenter.attach(view)
        presenter.onPageFinished()
        settle()
        presenter.onPageFinished()

        assertEquals(emptyList<String>(), marker.marks)
    }

    @Test
    fun `a network error asks to retry and retrying loads the voucher again`() {
        payments.respondVoucher = { Single.error(failure(LegacyError.Network)) }
        presenter.attach(view)
        settle()
        assertEquals(LoadPhase.FAILED, view.last.phase)
        assertEquals(LegacyError.Network, view.last.error)

        payments.respondVoucher = { Single.just(Samples.voucherHtml) }
        presenter.onRetry()
        settle()

        assertEquals(LoadPhase.CONTENT, view.last.phase)
        assertNull(view.last.error)
        assertEquals(listOf("PAY-000001", "PAY-000001"), payments.vouchers)
    }

    @Test
    fun `cancelling the error keeps the screen failed without the dialog, so it can still finish`() {
        payments.respondVoucher = { Single.error(failure(LegacyError.Server)) }
        presenter.attach(view)
        settle()

        presenter.onCancel()

        assertEquals(LoadPhase.FAILED, view.last.phase)
        assertNull(view.last.error)
        assertEquals(emptyList<String>(), view.events)
    }

    @Test
    fun `finishing closes the module`() {
        presenter.attach(view)
        settle()

        presenter.onFinish()

        assertEquals(listOf("close"), view.events)
    }

    @Test
    fun `finishing while the voucher is loading or failed also closes the module`() {
        payments.respondVoucher = { Single.never() }
        presenter.attach(view)
        settle()

        presenter.onFinish()

        assertEquals(listOf("close"), view.events)
    }

    @Test
    fun `an expired session closes the module`() {
        payments.respondVoucher = { Single.error(failure(LegacyError.SessionExpired)) }

        presenter.attach(view)
        settle()

        assertEquals(listOf("close"), view.events)
    }
}
