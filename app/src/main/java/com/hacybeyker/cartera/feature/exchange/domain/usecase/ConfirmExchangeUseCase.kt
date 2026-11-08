package com.hacybeyker.cartera.feature.exchange.domain.usecase

import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.core.domain.account.AccountChanges
import com.hacybeyker.cartera.core.domain.exchange.ExchangeOrder
import com.hacybeyker.cartera.feature.exchange.domain.model.ExchangeReceipt
import com.hacybeyker.cartera.feature.exchange.domain.repository.ExchangeRepository
import javax.inject.Inject

class ConfirmExchangeUseCase @Inject constructor(
    private val exchanges: ExchangeRepository,
    private val accountChanges: AccountChanges
) {
    suspend operator fun invoke(order: ExchangeOrder): Outcome<ExchangeReceipt> = exchanges.exchange(order).also {
        // Sin respuesta (timeout o desconexión) o con un error del servidor no se sabe si se hizo: se trata como un
        // cambio, así las cuentas se vuelven a pedir. Un rechazo de negocio, de validación o por vencimiento no mueve
        // saldos.
        val maybeExecuted = it is Outcome.Success ||
            (it is Outcome.Failure && (it.error == AppError.Network || it.error == AppError.Server))
        if (maybeExecuted) accountChanges.notifyChanged()
    }
}
