package com.hacybeyker.cartera.core.domain.account

import com.hacybeyker.cartera.core.domain.Outcome

interface AccountRepository {
    suspend fun accounts(): Outcome<List<Account>>

    suspend fun account(accountId: String): Outcome<Account>
}
