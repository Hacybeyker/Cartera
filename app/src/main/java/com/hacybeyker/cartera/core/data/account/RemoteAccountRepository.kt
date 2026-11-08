package com.hacybeyker.cartera.core.data.account

import com.hacybeyker.cartera.core.data.network.AccessTokenProvider
import com.hacybeyker.cartera.core.data.network.ApiCaller
import com.hacybeyker.cartera.core.data.network.CarteraApi
import com.hacybeyker.cartera.core.data.network.dto.toAccount
import com.hacybeyker.cartera.core.data.network.toOutcome
import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.core.domain.account.Account
import com.hacybeyker.cartera.core.domain.account.AccountChanges
import com.hacybeyker.cartera.core.domain.account.AccountRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Guarda en memoria la última lista de cuentas. Vale mientras no cambie la revisión de [changes] (una transferencia
 * la sube) ni el token: otra sesión nunca ve las cuentas de la anterior.
 */
@Singleton
class RemoteAccountRepository @Inject constructor(
    private val api: CarteraApi,
    private val call: ApiCaller,
    private val changes: AccountChanges,
    private val tokens: AccessTokenProvider
) : AccountRepository {

    private data class Cached(val revision: Int, val token: String?, val accounts: List<Account>)

    // Quien llega mientras otra llamada está en curso espera su resultado en vez de pedir de nuevo.
    private val mutex = Mutex()
    private var cached: Cached? = null

    override suspend fun accounts(): Outcome<List<Account>> = mutex.withLock {
        val revision = changes.revision
        val token = tokens.accessToken()
        cached?.takeIf { it.revision == revision && it.token == token }?.let { return Outcome.Success(it.accounts) }
        call { api.accounts().items.map { it.toAccount() } }.toOutcome().also { outcome ->
            if (outcome is Outcome.Success) cached = Cached(revision, token, outcome.value)
        }
    }

    override suspend fun account(accountId: String): Outcome<Account> =
        call { api.account(accountId).toAccount() }.toOutcome()
}
