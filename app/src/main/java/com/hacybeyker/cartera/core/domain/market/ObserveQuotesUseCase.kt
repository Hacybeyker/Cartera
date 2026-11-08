package com.hacybeyker.cartera.core.domain.market

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** Precios en vivo de [symbols]; el canal queda abierto mientras alguien recolecta el flujo. */
class ObserveQuotesUseCase @Inject constructor(private val stream: MarketStream) {
    operator fun invoke(symbols: Set<String>): Flow<Quote> = stream.quotes(symbols)
}
