package com.hacybeyker.cartera.core.data.movement

import com.hacybeyker.cartera.core.data.network.ApiCaller
import com.hacybeyker.cartera.core.data.network.CarteraApi
import com.hacybeyker.cartera.core.data.network.dto.toMovement
import com.hacybeyker.cartera.core.data.network.toOutcome
import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.core.domain.Page
import com.hacybeyker.cartera.core.domain.movement.Movement
import com.hacybeyker.cartera.core.domain.movement.MovementRepository
import javax.inject.Inject

class RemoteMovementRepository @Inject constructor(private val api: CarteraApi, private val call: ApiCaller) :
    MovementRepository {

    override suspend fun page(accountId: String?, cursor: String?): Outcome<Page<Movement>> = call {
        val page = api.movements(accountId = accountId, cursor = cursor, limit = PAGE_SIZE)
        Page(items = page.items.map { it.toMovement() }, nextCursor = page.nextCursor)
    }.toOutcome()

    companion object {
        const val PAGE_SIZE = 20
    }
}
