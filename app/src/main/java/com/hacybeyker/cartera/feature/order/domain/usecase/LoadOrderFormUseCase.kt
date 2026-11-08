package com.hacybeyker.cartera.feature.order.domain.usecase

import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.core.domain.account.AccountRepository
import com.hacybeyker.cartera.core.domain.flatMap
import com.hacybeyker.cartera.core.domain.map
import com.hacybeyker.cartera.core.domain.market.OrderSide
import com.hacybeyker.cartera.core.domain.portfolio.PortfolioRepository
import com.hacybeyker.cartera.feature.order.domain.model.OrderFormData
import javax.inject.Inject

/**
 * Lo que necesita el formulario, desde las cachés (sin llamadas si ya se mostraron las cuentas y el portafolio): las
 * cuentas en dólares, porque las acciones se liquidan en USD, y en una venta las acciones que se tienen.
 */
class LoadOrderFormUseCase @Inject constructor(
    private val accounts: AccountRepository,
    private val portfolio: PortfolioRepository
) {
    suspend operator fun invoke(symbol: String, side: OrderSide): Outcome<OrderFormData> =
        accounts.accounts().flatMap { all ->
            val settlement = all.filter { it.currency == SETTLEMENT_CURRENCY }
            when (side) {
                OrderSide.BUY -> Outcome.Success(OrderFormData(settlement, heldQuantity = 0))
                OrderSide.SELL -> portfolio.portfolio().map { current ->
                    OrderFormData(settlement, current.holdings.firstOrNull { it.symbol == symbol }?.quantity ?: 0)
                }
            }
        }

    companion object {
        const val SETTLEMENT_CURRENCY = "USD"
    }
}
