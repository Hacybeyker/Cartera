package com.hacybeyker.cartera.core.data.account

import com.hacybeyker.cartera.core.data.network.ApiCaller
import com.hacybeyker.cartera.core.data.network.CarteraApi
import com.hacybeyker.cartera.core.data.network.dto.toAccount
import com.hacybeyker.cartera.core.data.network.toOutcome
import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.core.domain.account.Account
import com.hacybeyker.cartera.core.domain.account.AccountRepository
import javax.inject.Inject

class RemoteAccountRepository @Inject constructor(private val api: CarteraApi, private val call: ApiCaller) :
    AccountRepository {

    override suspend fun accounts(): Outcome<List<Account>> =
        call { api.accounts().items.map { it.toAccount() } }.toOutcome()

    override suspend fun account(accountId: String): Outcome<Account> =
        call { api.account(accountId).toAccount() }.toOutcome()
}
