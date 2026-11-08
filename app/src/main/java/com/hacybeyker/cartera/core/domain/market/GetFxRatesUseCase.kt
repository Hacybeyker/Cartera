package com.hacybeyker.cartera.core.domain.market

import com.hacybeyker.cartera.core.domain.Outcome
import javax.inject.Inject

/** Pide al servidor una cotización nueva de USD/PEN: cada llamada emite otra, con su propio vencimiento. */
class GetFxRatesUseCase @Inject constructor(private val market: MarketRepository) {
    suspend operator fun invoke(): Outcome<FxQuote> = market.fxRates()
}
