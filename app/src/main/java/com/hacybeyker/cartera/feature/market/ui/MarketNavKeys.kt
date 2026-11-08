package com.hacybeyker.cartera.feature.market.ui

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class StockDetailNavKey(val symbol: String) : NavKey

@Serializable
data object PortfolioNavKey : NavKey
