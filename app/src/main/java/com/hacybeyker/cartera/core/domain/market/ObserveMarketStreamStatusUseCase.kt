package com.hacybeyker.cartera.core.domain.market

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveMarketStreamStatusUseCase @Inject constructor(private val stream: MarketStream) {
    operator fun invoke(): Flow<MarketStreamStatus> = stream.status
}
