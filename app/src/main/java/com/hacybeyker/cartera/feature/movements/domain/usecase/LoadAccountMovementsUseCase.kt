package com.hacybeyker.cartera.feature.movements.domain.usecase

import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.core.domain.account.AccountRepository
import com.hacybeyker.cartera.core.domain.flatMap
import com.hacybeyker.cartera.core.domain.map
import com.hacybeyker.cartera.core.domain.movement.MovementRepository
import com.hacybeyker.cartera.feature.movements.domain.model.AccountMovements
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/** Cabecera y primera página de una cuenta: `GET /accounts/{id}` y `GET /movements?accountId=` en paralelo. */
class LoadAccountMovementsUseCase @Inject constructor(
    private val accounts: AccountRepository,
    private val movements: MovementRepository
) {
    suspend operator fun invoke(accountId: String): Outcome<AccountMovements> = coroutineScope {
        val account = async { accounts.account(accountId) }
        val firstPage = async { movements.page(accountId = accountId, cursor = null) }
        account.await().flatMap { header -> firstPage.await().map { AccountMovements(header, it) } }
    }
}
