package com.hacybeyker.cartera.feature.market.domain.usecase

import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.core.domain.flatMap
import com.hacybeyker.cartera.core.domain.map
import com.hacybeyker.cartera.core.domain.market.HistoryRange
import com.hacybeyker.cartera.core.domain.market.MarketRepository
import com.hacybeyker.cartera.core.domain.portfolio.PortfolioRepository
import com.hacybeyker.cartera.feature.market.domain.model.StockOverview
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * Detalle de acción: `GET /market/stocks/{symbol}`, su historial en [range] y `GET /portfolio` en paralelo. Si la
 * acción no existe gana ese error, así se ve "no encontrada" aunque falle otra llamada.
 */
class LoadStockDetailUseCase @Inject constructor(
    private val market: MarketRepository,
    private val portfolio: PortfolioRepository
) {
    suspend operator fun invoke(symbol: String, range: HistoryRange): Outcome<StockOverview> = coroutineScope {
        val stock = async { market.stock(symbol) }
        val history = async { market.history(symbol, range) }
        val holdings = async { portfolio.portfolio() }
        stock.await().flatMap { detail ->
            history.await().flatMap { points ->
                holdings.await().map { current ->
                    StockOverview(
                        stock = detail,
                        history = points,
                        holding = current.holdings.firstOrNull { it.symbol == symbol }?.withQuote(detail.quote)
                    )
                }
            }
        }
    }
}
