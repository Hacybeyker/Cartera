package com.hacybeyker.cartera.feature.home.domain.usecase

import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.core.domain.account.Account
import com.hacybeyker.cartera.core.domain.map
import com.hacybeyker.cartera.core.domain.market.MarketRepository
import com.hacybeyker.cartera.feature.home.domain.model.ExchangeDraft
import com.hacybeyker.cartera.feature.home.domain.model.PreparedExchange
import javax.inject.Inject

/**
 * Continuar en Cambio: pide una cotización nueva y valida el cambio con ella. Así la confirmación empieza con la
 * vigencia completa (`quote_ttl_s`), aunque la última cotización del canal sea de hace rato (con `tick_ms 0` no llegan
 * más). Quien llama ya validó el borrador con la cotización en vivo, para no pedir nada si hay errores.
 */
class StartExchangeUseCase @Inject constructor(
    private val market: MarketRepository,
    private val validate: ValidateExchangeUseCase
) {
    suspend operator fun invoke(draft: ExchangeDraft, accounts: List<Account>): Outcome<PreparedExchange> =
        market.fxRates().map { quote -> PreparedExchange(quote = quote, validation = validate(draft, accounts, quote)) }
}
