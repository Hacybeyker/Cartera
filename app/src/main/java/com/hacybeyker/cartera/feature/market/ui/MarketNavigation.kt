package com.hacybeyker.cartera.feature.market.ui

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.hacybeyker.cartera.core.domain.market.OrderSide
import com.hacybeyker.cartera.feature.market.ui.detail.StockDetailScreen
import com.hacybeyker.cartera.feature.market.ui.portfolio.PortfolioScreen

/** [onTrade] abre el formulario de la orden que se eligió en el sheet Comprar/Vender del Detalle. */
fun EntryProviderScope<NavKey>.marketEntries(
    onBack: () -> Unit,
    onStockClick: (symbol: String) -> Unit,
    onTrade: (symbol: String, side: OrderSide) -> Unit
) {
    entry<StockDetailNavKey> { key ->
        StockDetailScreen(symbol = key.symbol, onBack = onBack, onTrade = { side -> onTrade(key.symbol, side) })
    }
    entry<PortfolioNavKey> {
        PortfolioScreen(onBack = onBack, onStockClick = onStockClick)
    }
}
