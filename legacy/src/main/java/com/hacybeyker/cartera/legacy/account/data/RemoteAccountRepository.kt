package com.hacybeyker.cartera.legacy.account.data

import com.hacybeyker.cartera.legacy.account.data.dto.toAccount
import com.hacybeyker.cartera.legacy.account.domain.Account
import com.hacybeyker.cartera.legacy.account.domain.AccountRepository
import com.hacybeyker.cartera.legacy.core.data.LegacyApiCaller
import io.reactivex.rxjava3.core.Single
import javax.inject.Inject

class RemoteAccountRepository @Inject constructor(private val api: AccountsApi, private val caller: LegacyApiCaller) :
    AccountRepository {

    override fun accounts(): Single<List<Account>> = api.accounts()
        .map { list -> list.items.map { it.toAccount() } }
        .compose(caller.translated())
}
