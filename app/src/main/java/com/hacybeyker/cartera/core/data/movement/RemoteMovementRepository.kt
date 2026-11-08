package com.hacybeyker.cartera.core.data.movement

import com.hacybeyker.cartera.core.common.MainDispatcher
import com.hacybeyker.cartera.core.data.network.ApiCaller
import com.hacybeyker.cartera.core.data.network.CarteraApi
import com.hacybeyker.cartera.core.data.network.dto.MovementPageDto
import com.hacybeyker.cartera.core.data.network.dto.toMovement
import com.hacybeyker.cartera.core.data.network.toOutcome
import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.core.domain.Page
import com.hacybeyker.cartera.core.domain.flags.FeatureFlag
import com.hacybeyker.cartera.core.domain.flags.FeatureFlags
import com.hacybeyker.cartera.core.domain.movement.Movement
import com.hacybeyker.cartera.core.domain.movement.MovementRepository
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class RemoteMovementRepository @Inject constructor(
    private val api: CarteraApi,
    private val call: ApiCaller,
    private val json: Json,
    private val flags: FeatureFlags,
    @MainDispatcher private val main: CoroutineDispatcher
) : MovementRepository {

    override suspend fun page(accountId: String?, cursor: String?): Outcome<Page<Movement>> = call {
        val page = if (accountId != null && flags.isEnabled(FeatureFlag.MOVEMENTS_FULL_PAGE)) {
            fullPage(accountId, cursor)
        } else {
            // El converter de Retrofit decodifica en el hilo de OkHttp, antes de volver.
            api.movements(accountId = accountId, cursor = cursor, limit = PAGE_SIZE)
        }
        Page(items = page.items.map { it.toMovement() }, nextCursor = page.nextCursor)
    }.toOutcome()

    private suspend fun fullPage(accountId: String, cursor: String?): MovementPageDto {
        val body = api.movementsBody(accountId = accountId, cursor = cursor, limit = FULL_PAGE_SIZE)
        return withContext(main) { json.decodeFromString(MovementPageDto.serializer(), body.string()) }
    }

    companion object {
        const val PAGE_SIZE = 20
        const val FULL_PAGE_SIZE = 1000
    }
}
