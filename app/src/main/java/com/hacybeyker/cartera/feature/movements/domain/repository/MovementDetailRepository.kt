package com.hacybeyker.cartera.feature.movements.domain.repository

import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.feature.movements.domain.model.Breakdown
import com.hacybeyker.cartera.feature.movements.domain.model.MovementDetail

interface MovementDetailRepository {
    suspend fun detail(movementId: String): Outcome<MovementDetail>

    suspend fun breakdown(movementId: String): Outcome<Breakdown>
}
