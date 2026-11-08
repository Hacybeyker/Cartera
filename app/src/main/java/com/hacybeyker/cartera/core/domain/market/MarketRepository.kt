package com.hacybeyker.cartera.core.domain.market

import com.hacybeyker.cartera.core.domain.Outcome

interface MarketRepository {
    /** Todas las acciones con su precio vigente, por símbolo. */
    suspend fun stocks(): Outcome<List<Quote>>

    suspend fun stock(symbol: String): Outcome<StockDetail>

    suspend fun history(symbol: String, range: HistoryRange): Outcome<PriceHistory>
}
