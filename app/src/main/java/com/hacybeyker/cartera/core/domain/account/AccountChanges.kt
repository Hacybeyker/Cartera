package com.hacybeyker.cartera.core.domain.account

/**
 * Registro de las operaciones que mueven dinero. [revision] sube con cada una: la caché de cuentas deja de valer y
 * quien muestra saldos o movimientos sabe que debe volver a pedirlos la próxima vez que se muestre.
 */
interface AccountChanges {
    val revision: Int

    fun notifyChanged()
}
