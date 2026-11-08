package com.hacybeyker.cartera.feature.transfer.domain.usecase

import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.core.domain.account.AccountChanges
import com.hacybeyker.cartera.feature.transfer.domain.model.TransferOrder
import com.hacybeyker.cartera.feature.transfer.domain.model.TransferReceipt
import com.hacybeyker.cartera.feature.transfer.domain.repository.TransferRepository
import javax.inject.Inject

class ConfirmTransferUseCase @Inject constructor(
    private val transfers: TransferRepository,
    private val changes: AccountChanges
) {
    suspend operator fun invoke(order: TransferOrder): Outcome<TransferReceipt> = transfers.transfer(order).also {
        // Sin respuesta (timeout o desconexión) o con un error del servidor no se sabe si se cobró: se trata como un
        // cambio, así las cuentas y los movimientos se vuelven a pedir. Un rechazo de negocio o de validación no cobra.
        val maybeCharged = it is Outcome.Success ||
            (it is Outcome.Failure && (it.error == AppError.Network || it.error == AppError.Server))
        if (maybeCharged) changes.notifyChanged()
    }
}
