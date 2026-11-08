package com.hacybeyker.cartera.core.domain.market

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** Cotizaciones en vivo de USD/PEN; el canal queda abierto mientras alguien recolecta el flujo. */
class ObserveFxQuotesUseCase @Inject constructor(private val stream: MarketStream) {
    operator fun invoke(): Flow<FxQuote> = stream.fxQuotes(FxQuote.USD_PEN)
}
