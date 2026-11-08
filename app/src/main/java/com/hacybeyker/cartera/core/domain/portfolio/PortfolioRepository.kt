package com.hacybeyker.cartera.core.domain.portfolio

import com.hacybeyker.cartera.core.domain.Outcome

interface PortfolioRepository {
    /** Desde la caché mientras no cambien las tenencias ([PortfolioChanges]) ni la sesión. */
    suspend fun portfolio(): Outcome<Portfolio>
}
