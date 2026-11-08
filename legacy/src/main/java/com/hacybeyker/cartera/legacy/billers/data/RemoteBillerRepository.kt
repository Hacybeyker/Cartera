package com.hacybeyker.cartera.legacy.billers.data

import com.hacybeyker.cartera.legacy.billers.data.dto.toBiller
import com.hacybeyker.cartera.legacy.billers.domain.Biller
import com.hacybeyker.cartera.legacy.billers.domain.BillerRepository
import com.hacybeyker.cartera.legacy.core.data.LegacyApiCaller
import io.reactivex.rxjava3.core.Single
import javax.inject.Inject

class RemoteBillerRepository @Inject constructor(private val api: BillersApi, private val caller: LegacyApiCaller) :
    BillerRepository {

    override fun search(query: String): Single<List<Biller>> = api.billers(query)
        .map { list -> list.items.map { it.toBiller() } }
        .compose(caller.translated())
}
