package com.hacybeyker.cartera.legacy.debts.data

import com.hacybeyker.cartera.legacy.core.data.LegacyApiCaller
import com.hacybeyker.cartera.legacy.debts.data.dto.toCustomerDebts
import com.hacybeyker.cartera.legacy.debts.domain.CustomerDebts
import com.hacybeyker.cartera.legacy.debts.domain.DebtRepository
import io.reactivex.rxjava3.core.Single
import javax.inject.Inject

class RemoteDebtRepository @Inject constructor(private val api: DebtsApi, private val caller: LegacyApiCaller) :
    DebtRepository {

    override fun pendingDebts(billerId: String, customerCode: String): Single<CustomerDebts> =
        api.debts(billerId, customerCode)
            .map { it.toCustomerDebts() }
            .compose(caller.translated())
}
