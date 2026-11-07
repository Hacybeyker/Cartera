package com.hacybeyker.cartera.feature.movements

import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.core.domain.movement.MovementCategory
import com.hacybeyker.cartera.core.testing.pen
import com.hacybeyker.cartera.core.testing.testMovement
import com.hacybeyker.cartera.feature.movements.domain.model.Breakdown
import com.hacybeyker.cartera.feature.movements.domain.model.BreakdownLine
import com.hacybeyker.cartera.feature.movements.domain.model.Merchant
import com.hacybeyker.cartera.feature.movements.domain.model.MovementDetail
import com.hacybeyker.cartera.feature.movements.domain.repository.MovementDetailRepository

val TestDetail = MovementDetail(
    movement = testMovement(1),
    operationNumber = "20000001",
    channel = "POS",
    status = "COMPLETED",
    counterpartyName = "Supermercado La Canasta",
    counterpartyReference = "VISA ****9034"
)

val TestBreakdown = Breakdown(
    lines = listOf(BreakdownLine("Arroz extra 5 kg", pen("9.53")), BreakdownLine("Huevos x15", pen("29.24"))),
    fees = pen("0.00"),
    taxes = pen("210.39"),
    exchangeRate = null,
    merchant = Merchant("Supermercado La Canasta", "Lima"),
    category = MovementCategory.GROCERIES
)

class FakeMovementDetailRepository : MovementDetailRepository {
    val failures = ArrayDeque<AppError>()
    val requested = mutableListOf<String>()

    override suspend fun detail(movementId: String): Outcome<MovementDetail> {
        requested += movementId
        return failures.removeFirstOrNull()?.let { Outcome.Failure(it) } ?: Outcome.Success(TestDetail)
    }

    override suspend fun breakdown(movementId: String): Outcome<Breakdown> {
        requested += movementId
        return failures.removeFirstOrNull()?.let { Outcome.Failure(it) } ?: Outcome.Success(TestBreakdown)
    }
}
