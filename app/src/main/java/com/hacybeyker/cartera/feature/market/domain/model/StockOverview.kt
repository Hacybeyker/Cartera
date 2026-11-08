package com.hacybeyker.cartera.feature.market.domain.model

import com.hacybeyker.cartera.core.domain.market.PriceHistory
import com.hacybeyker.cartera.core.domain.market.StockDetail
import com.hacybeyker.cartera.core.domain.portfolio.Holding

/** Lo que muestra el Detalle de acción: la acción, su historial y la tenencia propia, valorizada a ese precio. */
data class StockOverview(val stock: StockDetail, val history: PriceHistory, val holding: Holding?)
