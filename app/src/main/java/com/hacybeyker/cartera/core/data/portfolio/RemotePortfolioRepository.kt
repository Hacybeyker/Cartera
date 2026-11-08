package com.hacybeyker.cartera.core.data.portfolio

import com.hacybeyker.cartera.core.data.network.AccessTokenProvider
import com.hacybeyker.cartera.core.data.network.ApiCaller
import com.hacybeyker.cartera.core.data.network.MarketApi
import com.hacybeyker.cartera.core.data.network.dto.toPortfolio
import com.hacybeyker.cartera.core.data.network.toOutcome
import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.core.domain.portfolio.Portfolio
import com.hacybeyker.cartera.core.domain.portfolio.PortfolioChanges
import com.hacybeyker.cartera.core.domain.portfolio.PortfolioRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Guarda en memoria el último portafolio, como las cuentas: vale mientras no cambie la revisión de [changes] (una
 * orden la sube) ni el token. Los precios que trae quedan viejos enseguida; quien lo muestra los sigue en vivo.
 */
@Singleton
class RemotePortfolioRepository @Inject constructor(
    private val api: MarketApi,
    private val call: ApiCaller,
    private val changes: PortfolioChanges,
    private val tokens: AccessTokenProvider
) : PortfolioRepository {

    private data class Cached(val revision: Int, val token: String?, val portfolio: Portfolio)

    private val mutex = Mutex()
    private var cached: Cached? = null

    override suspend fun portfolio(): Outcome<Portfolio> = mutex.withLock {
        val revision = changes.revision
        val token = tokens.accessToken()
        cached?.takeIf { it.revision == revision && it.token == token }?.let { return Outcome.Success(it.portfolio) }
        call { api.portfolio().toPortfolio() }.toOutcome().also { outcome ->
            if (outcome is Outcome.Success) cached = Cached(revision, token, outcome.value)
        }
    }
}
