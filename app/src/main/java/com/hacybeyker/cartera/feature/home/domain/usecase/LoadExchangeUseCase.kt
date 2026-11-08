package com.hacybeyker.cartera.feature.home.domain.usecase

import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.core.domain.account.AccountRepository
import com.hacybeyker.cartera.core.domain.flatMap
import com.hacybeyker.cartera.core.domain.map
import com.hacybeyker.cartera.core.domain.market.MarketRepository
import com.hacybeyker.cartera.feature.home.domain.model.ExchangeOverview
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/** Cambio: las cuentas (caché de Inicio) y `GET /fx/rates` en paralelo; después siguen las cotizaciones del canal. */
class LoadExchangeUseCase @Inject constructor(
    private val accounts: AccountRepository,
    private val market: MarketRepository
) {
    suspend operator fun invoke(): Outcome<ExchangeOverview> = coroutineScope {
        val own = async { accounts.accounts() }
        val quote = async { market.fxRates() }
        own.await().flatMap { list -> quote.await().map { ExchangeOverview(accounts = list, quote = it) } }
    }
}
