package com.hacybeyker.cartera.core.domain.market

import com.hacybeyker.cartera.core.domain.Outcome
import javax.inject.Inject

class GetStockUseCase @Inject constructor(private val market: MarketRepository) {
    suspend operator fun invoke(symbol: String): Outcome<StockDetail> = market.stock(symbol)
}
