package com.hacybeyker.cartera.core.domain.movement

import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.core.domain.Page
import javax.inject.Inject

class GetMovementsPageUseCase @Inject constructor(private val movements: MovementRepository) {
    suspend operator fun invoke(accountId: String? = null, cursor: String? = null): Outcome<Page<Movement>> =
        movements.page(accountId = accountId, cursor = cursor)
}
