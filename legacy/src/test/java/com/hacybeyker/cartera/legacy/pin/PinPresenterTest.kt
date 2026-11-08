package com.hacybeyker.cartera.legacy.pin

import com.hacybeyker.cartera.legacy.core.domain.FieldError
import com.hacybeyker.cartera.legacy.core.domain.LegacyError
import com.hacybeyker.cartera.legacy.core.domain.ProblemCode
import com.hacybeyker.cartera.legacy.flow.domain.PaymentFlow
import com.hacybeyker.cartera.legacy.payment.domain.BillPayment
import com.hacybeyker.cartera.legacy.payment.domain.PaymentOrder
import com.hacybeyker.cartera.legacy.pin.domain.PinNotice
import com.hacybeyker.cartera.legacy.pin.domain.PinPadOrder
import com.hacybeyker.cartera.legacy.testing.FakePaymentRepository
import com.hacybeyker.cartera.legacy.testing.FakePinPadSeed
import com.hacybeyker.cartera.legacy.testing.RecordingPinView
import com.hacybeyker.cartera.legacy.testing.Samples
import com.hacybeyker.cartera.legacy.testing.SequenceIdempotencyKeys
import com.hacybeyker.cartera.legacy.testing.TestSchedulerProvider
import com.hacybeyker.cartera.legacy.testing.failure
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.subjects.SingleSubject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PinPresenterTest {
    private val schedulers = TestSchedulerProvider()
    private val payments = FakePaymentRepository().apply { respondPay = { Single.just(Samples.payment) } }
    private val seed = FakePinPadSeed()
    private val keys = SequenceIdempotencyKeys()
    private val flow = PaymentFlow().apply {
        chooseBiller(Samples.luz)
        chooseCustomerCode("100200")
        chooseDebts("Ana Torres", Samples.luzDebts.debts)
        chooseAccount(Samples.gastos)
    }
    private val presenter = PinPresenter(payments, flow, seed, keys, schedulers)
    private val view = RecordingPinView()

    private fun settle() = schedulers.scheduler.triggerActions()

    private fun type(vararg digits: Int) = digits.forEach(presenter::onDigit)

    private fun rejected(code: ProblemCode, status: Int = 422, fields: List<FieldError> = emptyList()) =
        failure(LegacyError.Rejected(status, code, "detalle", fields))

    @Test
    fun `it starts with the fixed order and no digit typed`() {
        presenter.attach(view)

        assertEquals(PinPadOrder.FIXED, view.last.order)
        assertEquals(0, view.last.filled)
        assertFalse(view.last.sending)
        assertFalse(view.last.invalidPin)
    }

    @Test
    fun `the order comes from the seed of the scenario`() {
        seed.seed = 7

        presenter.attach(view)

        assertEquals(listOf(1, 2, 0, 4, 8, 5, 9, 6, 3, 7), view.last.order)
    }

    @Test
    fun `each digit fills a dot and delete empties the last one`() {
        presenter.attach(view)

        type(1, 2)
        assertEquals(2, view.last.filled)
        presenter.onDelete()
        assertEquals(1, view.last.filled)
        presenter.onDelete()
        presenter.onDelete()
        assertEquals(0, view.last.filled)
        assertEquals(emptyList<PaymentOrder>(), payments.orders)
    }

    @Test
    fun `the state never carries the digits that were typed`() {
        presenter.attach(view)

        type(1, 2, 3, 4)

        assertTrue(view.states.none { it.toString().contains("1234") })
    }

    @Test
    fun `the fourth digit sends the payment by itself with the data of the flow and a new idempotency key`() {
        presenter.attach(view)

        type(1, 2, 3)
        assertEquals(emptyList<PaymentOrder>(), payments.orders)
        type(4)
        settle()

        assertEquals(
            listOf(
                PaymentOrder(
                    billerId = "BIL-LUZ",
                    customerCode = "100200",
                    debtIds = listOf("DEB-LUZ-202610", "DEB-LUZ-202611"),
                    accountId = "ACC-002",
                    pin = "1234",
                    idempotencyKey = "key-1"
                )
            ),
            payments.orders
        )
    }

    @Test
    fun `while the request is in flight the keyboard ignores more taps and shows the progress`() {
        payments.respondPay = { Single.never() }
        presenter.attach(view)
        type(1, 2, 3, 4)
        settle()

        type(5, 6)
        presenter.onDelete()

        assertTrue(view.last.sending)
        assertEquals(4, view.last.filled)
        assertEquals(1, payments.orders.size)
    }

    @Test
    fun `a payment keeps the voucher data in the flow and opens the voucher`() {
        presenter.attach(view)
        type(1, 2, 3, 4)
        settle()

        assertEquals(Samples.payment, flow.payment)
        assertTrue(flow.accountsChanged)
        assertEquals(listOf("showVoucher"), view.events)
    }

    @Test
    fun `after the payment the pin is gone and a retry sends nothing`() {
        presenter.attach(view)
        type(1, 2, 3, 4)
        settle()

        presenter.onRetry()
        settle()

        assertEquals(1, payments.orders.size)
    }

    @Test
    fun `a wrong pin shows the message, empties the dots and shuffles the keyboard with the next seed`() {
        seed.seed = 7
        payments.respondPay = { Single.error(rejected(ProblemCode.INVALID_PIN)) }
        presenter.attach(view)

        type(0, 0, 0, 0)
        settle()

        assertTrue(view.last.invalidPin)
        assertEquals(0, view.last.filled)
        assertFalse(view.last.sending)
        assertEquals(PinPadOrder.forSeed(8), view.last.order)
        assertNull(view.last.notice)
        assertNull(view.last.error)
        assertEquals(emptyList<String>(), view.events)
    }

    @Test
    fun `each wrong pin reorders again and a new attempt uses a new idempotency key and the new pin`() {
        seed.seed = 7
        payments.respondPay = { Single.error(rejected(ProblemCode.INVALID_PIN)) }
        presenter.attach(view)
        type(0, 0, 0, 0)
        settle()

        type(1, 1, 1, 1)
        settle()

        assertEquals(PinPadOrder.forSeed(9), view.last.order)
        assertEquals(listOf("key-1", "key-2"), payments.orders.map { it.idempotencyKey })
        assertEquals(listOf("0000", "1111"), payments.orders.map { it.pin })
    }

    @Test
    fun `a wrong pin with seed 0 leaves the keyboard fixed`() {
        payments.respondPay = { Single.error(rejected(ProblemCode.INVALID_PIN)) }
        presenter.attach(view)

        type(0, 0, 0, 0)
        settle()

        assertEquals(PinPadOrder.FIXED, view.last.order)
        assertTrue(view.last.invalidPin)
    }

    @Test
    fun `typing again clears the wrong pin message`() {
        payments.respondPay = { Single.error(rejected(ProblemCode.INVALID_PIN)) }
        presenter.attach(view)
        type(0, 0, 0, 0)
        settle()

        type(1)

        assertFalse(view.last.invalidPin)
        assertEquals(1, view.last.filled)
    }

    @Test
    fun `not enough balance shows its notice and accepting it goes back to the account`() {
        payments.respondPay = { Single.error(rejected(ProblemCode.INSUFFICIENT_FUNDS)) }
        presenter.attach(view)

        type(1, 2, 3, 4)
        settle()

        assertEquals(PinNotice.INSUFFICIENT_FUNDS, view.last.notice)
        assertEquals(emptyList<String>(), view.events)
        presenter.onNoticeAccepted()
        assertNull(view.last.notice)
        assertEquals(listOf("goBack"), view.events)
        assertFalse(flow.accountsChanged)
    }

    @Test
    fun `a debt that was already paid shows its notice and accepting it reloads the debts`() {
        payments.respondPay = { Single.error(rejected(ProblemCode.DEBT_ALREADY_PAID)) }
        presenter.attach(view)

        type(1, 2, 3, 4)
        settle()

        assertEquals(PinNotice.DEBT_ALREADY_PAID, view.last.notice)
        presenter.onNoticeAccepted()
        assertEquals(listOf("reloadDebts"), view.events)
    }

    @Test
    fun `any other rejection shows the generic notice and accepting it goes back to the account`() {
        listOf(
            rejected(ProblemCode.VALIDATION_ERROR, fields = listOf(FieldError("debtIds", "UNKNOWN_DEBT", "m"))),
            rejected(ProblemCode.CURRENCY_MISMATCH),
            rejected(ProblemCode.UNKNOWN, status = 409)
        ).forEach { rejection ->
            val separate = RecordingPinView()
            val other = PinPresenter(
                FakePaymentRepository().apply { respondPay = { Single.error(rejection) } },
                flow,
                seed,
                SequenceIdempotencyKeys(),
                schedulers
            )
            other.attach(separate)
            listOf(1, 2, 3, 4).forEach(other::onDigit)
            settle()

            assertEquals(PinNotice.REJECTED, separate.last.notice)
            other.onNoticeAccepted()
            assertEquals(listOf("goBack"), separate.events)
        }
    }

    @Test
    fun `while a notice is showing the keyboard ignores taps`() {
        payments.respondPay = { Single.error(rejected(ProblemCode.INSUFFICIENT_FUNDS)) }
        presenter.attach(view)
        type(1, 2, 3, 4)
        settle()

        type(5)

        assertEquals(0, view.last.filled)
    }

    @Test
    fun `accepting without a notice does nothing`() {
        presenter.attach(view)

        presenter.onNoticeAccepted()

        assertEquals(emptyList<String>(), view.events)
    }

    @Test
    fun `a network error asks to retry and marks that the payment may have been charged`() {
        payments.respondPay = { Single.error(failure(LegacyError.Network)) }
        presenter.attach(view)

        type(1, 2, 3, 4)
        settle()

        assertEquals(LegacyError.Network, view.last.error)
        assertFalse(view.last.sending)
        assertTrue(flow.accountsChanged)
        assertNull(flow.payment)
        type(5)
        assertEquals(4, view.last.filled)
    }

    @Test
    fun `retrying after a network error or a 5xx sends the same body with the same idempotency key`() {
        payments.respondPay = { Single.error(failure(LegacyError.Network)) }
        presenter.attach(view)
        type(1, 2, 3, 4)
        settle()
        payments.respondPay = { Single.error(failure(LegacyError.Server)) }
        presenter.onRetry()
        settle()
        assertEquals(LegacyError.Server, view.last.error)
        payments.respondPay = { Single.just(Samples.payment) }

        presenter.onRetry()
        settle()

        assertEquals(3, payments.orders.size)
        assertEquals(setOf(payments.orders.first()), payments.orders.toSet())
        assertEquals(1, keys.created)
        assertNull(view.last.error)
        assertEquals(listOf("showVoucher"), view.events)
        assertEquals(Samples.payment, flow.payment)
    }

    @Test
    fun `cancelling the error goes back without paying and the next attempt is a new one`() {
        payments.respondPay = { Single.error(failure(LegacyError.Server)) }
        presenter.attach(view)
        type(1, 2, 3, 4)
        settle()

        presenter.onCancel()

        assertNull(view.last.error)
        assertEquals(0, view.last.filled)
        assertEquals(listOf("goBack"), view.events)
        presenter.onRetry()
        settle()
        assertEquals(1, payments.orders.size)
        payments.respondPay = { Single.just(Samples.payment) }
        type(4, 3, 2, 1)
        settle()
        assertEquals(listOf("key-1", "key-2"), payments.orders.map { it.idempotencyKey })
        assertEquals("4321", payments.orders.last().pin)
    }

    @Test
    fun `an expired session closes the module without a dialog`() {
        payments.respondPay = { Single.error(failure(LegacyError.SessionExpired)) }
        presenter.attach(view)

        type(1, 2, 3, 4)
        settle()

        assertEquals(listOf("close"), view.events)
        assertNull(view.last.error)
        assertNull(view.last.notice)
    }

    @Test
    fun `releasing the presenter drops the pin of a payment that was not resolved`() {
        payments.respondPay = { Single.error(failure(LegacyError.Network)) }
        presenter.attach(view)
        type(1, 2, 3, 4)
        settle()

        presenter.destroy()
        presenter.onRetry()
        settle()

        assertEquals(1, payments.orders.size)
    }

    @Test
    fun `leaving while the request is in flight cancels it`() {
        val inFlight = SingleSubject.create<BillPayment>()
        payments.respondPay = { inFlight }
        presenter.attach(view)
        type(1, 2, 3, 4)
        settle()
        assertTrue(inFlight.hasObservers())

        presenter.destroy()

        assertFalse(inFlight.hasObservers())
    }

    @Test
    fun `a rotation keeps the typed dots and the order`() {
        seed.seed = 7
        presenter.attach(view)
        type(1, 2)
        presenter.detach()
        val second = RecordingPinView()

        presenter.attach(second)

        assertEquals(2, second.last.filled)
        assertEquals(PinPadOrder.forSeed(7), second.last.order)
    }
}
