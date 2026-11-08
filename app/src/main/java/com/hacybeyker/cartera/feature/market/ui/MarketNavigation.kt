package com.hacybeyker.cartera.feature.market.ui

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.hacybeyker.cartera.feature.market.ui.detail.StockDetailScreen
import com.hacybeyker.cartera.feature.market.ui.portfolio.PortfolioScreen

fun EntryProviderScope<NavKey>.marketEntries(onBack: () -> Unit, onStockClick: (symbol: String) -> Unit) {
    entry<StockDetailNavKey> { key ->
        StockDetailScreen(symbol = key.symbol, onBack = onBack)
    }
    entry<PortfolioNavKey> {
        PortfolioScreen(onBack = onBack, onStockClick = onStockClick)
    }
}
