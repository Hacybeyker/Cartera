package com.hacybeyker.cartera.feature.transfer.data.repository

import com.hacybeyker.cartera.core.data.network.ApiCaller
import com.hacybeyker.cartera.core.data.network.CarteraApi
import com.hacybeyker.cartera.core.data.network.dto.MoneyDto
import com.hacybeyker.cartera.core.data.network.dto.TransferRequestDto
import com.hacybeyker.cartera.core.data.network.dto.toMoney
import com.hacybeyker.cartera.core.data.network.toOutcome
import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.feature.transfer.domain.model.TransferOrder
import com.hacybeyker.cartera.feature.transfer.domain.model.TransferReceipt
import com.hacybeyker.cartera.feature.transfer.domain.repository.TransferRepository
import javax.inject.Inject

class RemoteTransferRepository @Inject constructor(private val api: CarteraApi, private val call: ApiCaller) :
    TransferRepository {

    override suspend fun transfer(order: TransferOrder): Outcome<TransferReceipt> = call {
        val request = TransferRequestDto(
            sourceAccountId = order.sourceAccountId,
            destinationAccountNumber = order.destinationAccountNumber,
            amount = MoneyDto(value = order.amount.amount.toPlainString(), currency = order.amount.currency),
            description = order.description
        )
        api.transfer(idempotencyKey = order.idempotencyKey, body = request).let {
            TransferReceipt(
                transferId = it.transferId,
                operationNumber = it.operationNumber,
                sourceAccountId = it.sourceAccountId,
                sourceAvailableBalance = it.sourceAvailableBalance.toMoney(),
                movementId = it.movementId
            )
        }
    }.toOutcome()
}
