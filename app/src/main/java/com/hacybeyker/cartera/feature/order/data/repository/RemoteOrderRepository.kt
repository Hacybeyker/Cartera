package com.hacybeyker.cartera.feature.order.data.repository

import com.hacybeyker.cartera.core.data.network.ApiCaller
import com.hacybeyker.cartera.core.data.network.MarketApi
import com.hacybeyker.cartera.core.data.network.dto.OrderRequestDto
import com.hacybeyker.cartera.core.data.network.dto.OrderResponseDto
import com.hacybeyker.cartera.core.data.network.dto.toMoney
import com.hacybeyker.cartera.core.data.network.toOutcome
import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.core.domain.market.OrderSide
import com.hacybeyker.cartera.feature.order.domain.model.MarketOrder
import com.hacybeyker.cartera.feature.order.domain.model.OrderHolding
import com.hacybeyker.cartera.feature.order.domain.model.OrderReceipt
import com.hacybeyker.cartera.feature.order.domain.repository.OrderRepository
import javax.inject.Inject
import kotlinx.serialization.SerializationException

class RemoteOrderRepository @Inject constructor(private val api: MarketApi, private val call: ApiCaller) :
    OrderRepository {

    override suspend fun place(order: MarketOrder): Outcome<OrderReceipt> = call {
        val request = OrderRequestDto(
            symbol = order.symbol,
            side = order.side.name,
            quantity = order.quantity,
            accountId = order.accountId
        )
        api.order(idempotencyKey = order.idempotencyKey, body = request).toReceipt()
    }.toOutcome()
}

private fun OrderResponseDto.toReceipt() = OrderReceipt(
    orderId = orderId,
    operationNumber = operationNumber,
    // Un lado fuera del contrato es una respuesta inválida, no un cierre de la app.
    side = OrderSide.entries.firstOrNull { it.name == side } ?: throw SerializationException("side: $side"),
    symbol = symbol,
    quantity = quantity,
    executedPrice = executedPrice.toMoney(),
    total = total.toMoney(),
    accountId = accountId,
    accountAvailableBalance = accountAvailableBalance.toMoney(),
    movementId = movementId,
    holding = OrderHolding(quantity = holding.quantity, averagePrice = holding.averagePrice?.toMoney())
)
