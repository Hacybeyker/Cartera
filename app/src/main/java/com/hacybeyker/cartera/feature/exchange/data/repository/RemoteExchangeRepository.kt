package com.hacybeyker.cartera.feature.exchange.data.repository

import com.hacybeyker.cartera.core.data.network.ApiCaller
import com.hacybeyker.cartera.core.data.network.MarketApi
import com.hacybeyker.cartera.core.data.network.dto.ExchangeRequestDto
import com.hacybeyker.cartera.core.data.network.dto.ExchangeResponseDto
import com.hacybeyker.cartera.core.data.network.dto.MoneyDto
import com.hacybeyker.cartera.core.data.network.dto.toMoney
import com.hacybeyker.cartera.core.data.network.toOutcome
import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.core.domain.exchange.ExchangeOrder
import com.hacybeyker.cartera.feature.exchange.domain.model.ExchangeReceipt
import com.hacybeyker.cartera.feature.exchange.domain.repository.ExchangeRepository
import javax.inject.Inject

class RemoteExchangeRepository @Inject constructor(private val api: MarketApi, private val call: ApiCaller) :
    ExchangeRepository {

    override suspend fun exchange(order: ExchangeOrder): Outcome<ExchangeReceipt> = call {
        val request = ExchangeRequestDto(
            quoteId = order.quoteId,
            sourceAccountId = order.source.id,
            destinationAccountId = order.destination.id,
            amount = MoneyDto(value = order.amount.amount.toPlainString(), currency = order.amount.currency)
        )
        api.exchange(idempotencyKey = order.idempotencyKey, body = request).toReceipt()
    }.toOutcome()
}

private fun ExchangeResponseDto.toReceipt() = ExchangeReceipt(
    exchangeId = exchangeId,
    operationNumber = operationNumber,
    quoteId = quoteId,
    rate = rate.toBigDecimal(),
    debited = debited.toMoney(),
    credited = credited.toMoney(),
    sourceAccountId = sourceAccountId,
    sourceAvailableBalance = sourceAvailableBalance.toMoney(),
    destinationAccountId = destinationAccountId,
    destinationAvailableBalance = destinationAvailableBalance.toMoney(),
    movementIds = movementIds
)
