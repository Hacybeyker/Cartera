package com.hacybeyker.cartera.feature.movements.domain.usecase

import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.feature.movements.domain.model.Breakdown
import com.hacybeyker.cartera.feature.movements.domain.repository.MovementDetailRepository
import javax.inject.Inject

class GetBreakdownUseCase @Inject constructor(private val movements: MovementDetailRepository) {
    suspend operator fun invoke(movementId: String): Outcome<Breakdown> = movements.breakdown(movementId)
}
