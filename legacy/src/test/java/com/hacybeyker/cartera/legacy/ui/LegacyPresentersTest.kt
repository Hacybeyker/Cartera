package com.hacybeyker.cartera.legacy.ui

import com.hacybeyker.cartera.legacy.account.AccountPresenter
import com.hacybeyker.cartera.legacy.account.domain.LoadPayableAccountsUseCase
import com.hacybeyker.cartera.legacy.billers.BillersPresenter
import com.hacybeyker.cartera.legacy.customer.CustomerCodePresenter
import com.hacybeyker.cartera.legacy.customer.domain.ValidateCustomerCodeUseCase
import com.hacybeyker.cartera.legacy.debts.DebtsPresenter
import com.hacybeyker.cartera.legacy.flow.domain.PaymentFlow
import com.hacybeyker.cartera.legacy.pin.PinPresenter
import com.hacybeyker.cartera.legacy.testing.FakeAccountRepository
import com.hacybeyker.cartera.legacy.testing.FakeBillerRepository
import com.hacybeyker.cartera.legacy.testing.FakeDebtRepository
import com.hacybeyker.cartera.legacy.testing.FakePaymentRepository
import com.hacybeyker.cartera.legacy.testing.FakePinPadSeed
import com.hacybeyker.cartera.legacy.testing.RecordingMarker
import com.hacybeyker.cartera.legacy.testing.Samples
import com.hacybeyker.cartera.legacy.testing.SequenceIdempotencyKeys
import com.hacybeyker.cartera.legacy.testing.TestSchedulerProvider
import com.hacybeyker.cartera.legacy.voucher.VoucherPresenter
import org.junit.Assert.assertNotSame
import org.junit.Test

class LegacyPresentersTest {
    private val flow = PaymentFlow().apply { chooseBiller(Samples.luz) }
    private val schedulers = TestSchedulerProvider()
    private val presenters = LegacyPresenters(
        { BillersPresenter(FakeBillerRepository(), flow, RecordingMarker(), schedulers) },
        { CustomerCodePresenter(flow, ValidateCustomerCodeUseCase()) },
        { DebtsPresenter(FakeDebtRepository(), flow, RecordingMarker(), schedulers) },
        { AccountPresenter(LoadPayableAccountsUseCase(FakeAccountRepository()), flow, schedulers) },
        { PinPresenter(FakePaymentRepository(), flow, FakePinPadSeed(), SequenceIdempotencyKeys(), schedulers) },
        { VoucherPresenter(FakePaymentRepository(), flow, RecordingMarker(), schedulers) }
    )

    @Test
    fun `every call gives a new presenter, so a screen that is entered again starts from scratch`() {
        assertNotSame(presenters.billers(), presenters.billers())
        assertNotSame(presenters.customerCode(), presenters.customerCode())
        assertNotSame(presenters.debts(), presenters.debts())
        assertNotSame(presenters.account(), presenters.account())
        assertNotSame(presenters.pin(), presenters.pin())
        assertNotSame(presenters.voucher(), presenters.voucher())
    }
}
