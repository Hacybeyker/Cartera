package com.hacybeyker.cartera.legacy.ui

import com.hacybeyker.cartera.legacy.account.AccountPresenter
import com.hacybeyker.cartera.legacy.billers.BillersPresenter
import com.hacybeyker.cartera.legacy.customer.CustomerCodePresenter
import com.hacybeyker.cartera.legacy.debts.DebtsPresenter
import javax.inject.Inject
import javax.inject.Provider

internal class LegacyPresenters @Inject constructor(
    private val billersProvider: Provider<BillersPresenter>,
    private val customerCodeProvider: Provider<CustomerCodePresenter>,
    private val debtsProvider: Provider<DebtsPresenter>,
    private val accountProvider: Provider<AccountPresenter>
) : LegacyPresenterSource {
    override fun billers(): BillersPresenter = billersProvider.get()

    override fun customerCode(): CustomerCodePresenter = customerCodeProvider.get()

    override fun debts(): DebtsPresenter = debtsProvider.get()

    override fun account(): AccountPresenter = accountProvider.get()
}
