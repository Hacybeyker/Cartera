package com.hacybeyker.cartera.core.domain.portfolio

/**
 * Registro de las operaciones que cambian las tenencias (las órdenes). [revision] sube con cada una: la caché del
 * portafolio deja de valer y quien lo muestra vuelve a pedirlo la próxima vez que se ve.
 */
interface PortfolioChanges {
    val revision: Int

    fun notifyChanged()
}
