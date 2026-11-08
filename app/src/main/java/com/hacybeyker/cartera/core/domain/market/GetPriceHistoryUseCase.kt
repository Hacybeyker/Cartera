package com.hacybeyker.cartera.core.domain.market

import com.hacybeyker.cartera.core.domain.Outcome
import javax.inject.Inject

class GetPriceHistoryUseCase @Inject constructor(private val market: MarketRepository) {
    suspend operator fun invoke(symbol: String, range: HistoryRange): Outcome<PriceHistory> =
        market.history(symbol, range)
}
