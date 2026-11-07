package com.hacybeyker.cartera.core.domain.movement

import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.core.domain.Page

interface MovementRepository {
    /** Movimientos en orden de fecha descendente; sin [accountId], los de todas las cuentas. */
    suspend fun page(accountId: String?, cursor: String?): Outcome<Page<Movement>>
}
