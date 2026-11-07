package com.hacybeyker.cartera.feature.movements.data.repository

import com.hacybeyker.cartera.core.data.network.ApiCaller
import com.hacybeyker.cartera.core.data.network.CarteraApi
import com.hacybeyker.cartera.core.data.network.dto.BreakdownDto
import com.hacybeyker.cartera.core.data.network.dto.MovementDetailDto
import com.hacybeyker.cartera.core.data.network.dto.toMoney
import com.hacybeyker.cartera.core.data.network.dto.toMovement
import com.hacybeyker.cartera.core.data.network.toOutcome
import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.core.domain.movement.MovementCategory
import com.hacybeyker.cartera.feature.movements.domain.model.Breakdown
import com.hacybeyker.cartera.feature.movements.domain.model.BreakdownLine
import com.hacybeyker.cartera.feature.movements.domain.model.Merchant
import com.hacybeyker.cartera.feature.movements.domain.model.MovementDetail
import com.hacybeyker.cartera.feature.movements.domain.repository.MovementDetailRepository
import javax.inject.Inject

class RemoteMovementDetailRepository @Inject constructor(private val api: CarteraApi, private val call: ApiCaller) :
    MovementDetailRepository {

    override suspend fun detail(movementId: String): Outcome<MovementDetail> =
        call { api.movement(movementId).toDetail() }.toOutcome()

    override suspend fun breakdown(movementId: String): Outcome<Breakdown> =
        call { api.breakdown(movementId).toBreakdown() }.toOutcome()

    private fun MovementDetailDto.toDetail() = MovementDetail(
        movement = toMovement(),
        operationNumber = operationNumber,
        channel = channel,
        status = status,
        counterpartyName = counterparty.name,
        counterpartyReference = counterparty.reference
    )

    private fun BreakdownDto.toBreakdown() = Breakdown(
        lines = lines.map { BreakdownLine(concept = it.concept, amount = it.amount.toMoney()) },
        fees = fees.toMoney(),
        taxes = taxes.toMoney(),
        exchangeRate = exchangeRate?.toBigDecimal(),
        merchant = merchant?.let { Merchant(name = it.name, city = it.city) },
        category = MovementCategory.fromWire(category)
    )
}
