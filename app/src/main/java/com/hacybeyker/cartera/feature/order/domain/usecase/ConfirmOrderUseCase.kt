package com.hacybeyker.cartera.feature.order.domain.usecase

import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.core.domain.account.AccountChanges
import com.hacybeyker.cartera.core.domain.portfolio.PortfolioChanges
import com.hacybeyker.cartera.feature.order.domain.model.MarketOrder
import com.hacybeyker.cartera.feature.order.domain.model.OrderReceipt
import com.hacybeyker.cartera.feature.order.domain.repository.OrderRepository
import javax.inject.Inject

class ConfirmOrderUseCase @Inject constructor(
    private val orders: OrderRepository,
    private val accountChanges: AccountChanges,
    private val portfolioChanges: PortfolioChanges
) {
    suspend operator fun invoke(order: MarketOrder): Outcome<OrderReceipt> = orders.place(order).also {
        // Sin respuesta (timeout o desconexión) o con un error del servidor no se sabe si se ejecutó: se trata como un
        // cambio, así las cuentas y el portafolio se vuelven a pedir. Un rechazo de negocio o de validación no ejecuta.
        val maybeExecuted = it is Outcome.Success ||
            (it is Outcome.Failure && (it.error == AppError.Network || it.error == AppError.Server))
        if (maybeExecuted) {
            accountChanges.notifyChanged()
            portfolioChanges.notifyChanged()
        }
    }
}
