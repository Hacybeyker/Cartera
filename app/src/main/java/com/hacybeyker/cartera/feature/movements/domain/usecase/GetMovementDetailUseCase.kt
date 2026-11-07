package com.hacybeyker.cartera.feature.movements.domain.usecase

import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.feature.movements.domain.model.MovementDetail
import com.hacybeyker.cartera.feature.movements.domain.repository.MovementDetailRepository
import javax.inject.Inject

class GetMovementDetailUseCase @Inject constructor(private val movements: MovementDetailRepository) {
    suspend operator fun invoke(movementId: String): Outcome<MovementDetail> = movements.detail(movementId)
}
