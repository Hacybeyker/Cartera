package com.hacybeyker.cartera.core.domain.account

import com.hacybeyker.cartera.core.domain.Outcome

interface AccountRepository {
    /** Desde la caché mientras valga. */
    suspend fun accounts(): Outcome<List<Account>>

    /** Siempre al servidor, sin leer ni escribir la caché. */
    suspend fun fetchAccounts(): Outcome<List<Account>>

    suspend fun account(accountId: String): Outcome<Account>
}
