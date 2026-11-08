package com.hacybeyker.cartera.core.data.market

import com.hacybeyker.cartera.core.data.network.ApiCaller
import com.hacybeyker.cartera.core.data.network.MarketApi
import com.hacybeyker.cartera.core.data.network.dto.toFxQuote
import com.hacybeyker.cartera.core.data.network.dto.toPriceHistory
import com.hacybeyker.cartera.core.data.network.dto.toQuote
import com.hacybeyker.cartera.core.data.network.dto.toStockDetail
import com.hacybeyker.cartera.core.data.network.toOutcome
import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.core.domain.market.FxQuote
import com.hacybeyker.cartera.core.domain.market.HistoryRange
import com.hacybeyker.cartera.core.domain.market.MarketRepository
import com.hacybeyker.cartera.core.domain.market.PriceHistory
import com.hacybeyker.cartera.core.domain.market.Quote
import com.hacybeyker.cartera.core.domain.market.StockDetail
import javax.inject.Inject

/** Sin caché: los precios cambian con cada tick, así que cada pantalla pide los vigentes al abrirse. */
class RemoteMarketRepository @Inject constructor(private val api: MarketApi, private val call: ApiCaller) :
    MarketRepository {

    override suspend fun stocks(): Outcome<List<Quote>> = call { api.stocks().items.map { it.toQuote() } }.toOutcome()

    override suspend fun stock(symbol: String): Outcome<StockDetail> =
        call { api.stock(symbol).toStockDetail() }.toOutcome()

    override suspend fun history(symbol: String, range: HistoryRange): Outcome<PriceHistory> =
        call { api.stockHistory(symbol, range.wireName).toPriceHistory() }.toOutcome()

    override suspend fun fxRates(): Outcome<FxQuote> = call { api.fxRates().toFxQuote() }.toOutcome()
}
