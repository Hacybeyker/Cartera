package com.hacybeyker.cartera.feature.home.domain.usecase

import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.core.domain.flatMap
import com.hacybeyker.cartera.core.domain.map
import com.hacybeyker.cartera.core.domain.market.MarketRepository
import com.hacybeyker.cartera.core.domain.portfolio.PortfolioRepository
import com.hacybeyker.cartera.feature.home.domain.model.MarketOverview
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * Mercado: `GET /market/stocks` y `GET /portfolio` en paralelo. Entre una respuesta y otra puede haber pasado un tick,
 * así que el portafolio se revaloriza con los precios de la lista y la cabecera coincide con las filas.
 */
class LoadMarketUseCase @Inject constructor(
    private val market: MarketRepository,
    private val portfolio: PortfolioRepository
) {
    suspend operator fun invoke(): Outcome<MarketOverview> = coroutineScope {
        val stocks = async { market.stocks() }
        val holdings = async { portfolio.portfolio() }
        stocks.await().flatMap { quotes ->
            holdings.await().map { MarketOverview(stocks = quotes, portfolio = it.withQuotes(quotes)) }
        }
    }
}
