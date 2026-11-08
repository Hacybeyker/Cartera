package com.hacybeyker.cartera.feature.home.domain.model

import com.hacybeyker.cartera.core.domain.market.Quote
import com.hacybeyker.cartera.core.domain.portfolio.Portfolio

/** Lo que muestra Mercado: las acciones y el portafolio, valorizado a esos mismos precios. */
data class MarketOverview(val stocks: List<Quote>, val portfolio: Portfolio)
