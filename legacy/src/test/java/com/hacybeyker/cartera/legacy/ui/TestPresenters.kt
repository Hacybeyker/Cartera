package com.hacybeyker.cartera.legacy.ui

import com.hacybeyker.cartera.legacy.account.AccountPresenter
import com.hacybeyker.cartera.legacy.account.domain.LoadPayableAccountsUseCase
import com.hacybeyker.cartera.legacy.billers.BillersPresenter
import com.hacybeyker.cartera.legacy.customer.CustomerCodePresenter
import com.hacybeyker.cartera.legacy.customer.domain.ValidateCustomerCodeUseCase
import com.hacybeyker.cartera.legacy.debts.DebtsPresenter
import com.hacybeyker.cartera.legacy.flow.domain.PaymentFlow
import com.hacybeyker.cartera.legacy.testing.FakeAccountRepository
import com.hacybeyker.cartera.legacy.testing.FakeBillerRepository
import com.hacybeyker.cartera.legacy.testing.FakeDebtRepository
import com.hacybeyker.cartera.legacy.testing.ImmediateSchedulerProvider
import com.hacybeyker.cartera.legacy.testing.RecordingMarker

/** Presenters reales sobre repositorios falsos y llamadas al instante: lo único virtual es el reloj de las esperas. */
internal class TestPresenters(
    val flow: PaymentFlow = PaymentFlow(),
    val billers: FakeBillerRepository = FakeBillerRepository(),
    val debts: FakeDebtRepository = FakeDebtRepository(),
    val accounts: FakeAccountRepository = FakeAccountRepository(),
    val schedulers: ImmediateSchedulerProvider = ImmediateSchedulerProvider(),
    val marker: RecordingMarker = RecordingMarker()
) : LegacyPresenterSource {

    var onBillersPresenter: (BillersPresenter) -> Unit = {}
    var onCustomerCodePresenter: (CustomerCodePresenter) -> Unit = {}
    var onDebtsPresenter: (DebtsPresenter) -> Unit = {}
    var onAccountPresenter: (AccountPresenter) -> Unit = {}

    override fun billers() = BillersPresenter(billers, flow, marker, schedulers).also(onBillersPresenter)

    override fun customerCode() = CustomerCodePresenter(flow, ValidateCustomerCodeUseCase())
        .also(onCustomerCodePresenter)

    override fun debts() = DebtsPresenter(debts, flow, marker, schedulers).also(onDebtsPresenter)

    override fun account() = AccountPresenter(LoadPayableAccountsUseCase(accounts), flow, schedulers)
        .also(onAccountPresenter)
}
