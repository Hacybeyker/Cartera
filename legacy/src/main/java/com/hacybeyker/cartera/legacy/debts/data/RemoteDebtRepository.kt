package com.hacybeyker.cartera.legacy.debts.data

import com.hacybeyker.cartera.legacy.core.data.LegacyApiCaller
import com.hacybeyker.cartera.legacy.core.domain.SchedulerProvider
import com.hacybeyker.cartera.legacy.debts.data.dto.toCustomerDebts
import com.hacybeyker.cartera.legacy.debts.domain.CustomerDebts
import com.hacybeyker.cartera.legacy.debts.domain.DebtRepository
import com.hacybeyker.cartera.legacy.integration.LegacyFeatureFlags
import io.reactivex.rxjava3.core.Single
import javax.inject.Inject

class RemoteDebtRepository @Inject constructor(
    private val api: DebtsApi,
    private val caller: LegacyApiCaller,
    private val flags: LegacyFeatureFlags,
    private val schedulers: SchedulerProvider,
    private val decoder: DebtsBodyDecoder
) : DebtRepository {

    override fun pendingDebts(billerId: String, customerCode: String): Single<CustomerDebts> {
        if (flags.debtsMainDecode()) return pendingDebtsWithHistory(billerId, customerCode)
        return api.debts(billerId, customerCode)
            .map { it.toCustomerDebts() }
            .compose(caller.translated())
    }

    private fun pendingDebtsWithHistory(billerId: String, customerCode: String): Single<CustomerDebts> =
        api.debtsBody(billerId, customerCode, includeHistory = true)
            .subscribeOn(schedulers.io())
            .map { it.string() }
            .observeOn(schedulers.main())
            .map(decoder::decode)
            .compose(caller.translated())
}
