package com.hacybeyker.cartera.feature.home.ui.market

import androidx.compose.runtime.Immutable
import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.domain.Money
import com.hacybeyker.cartera.core.domain.market.Quote
import com.hacybeyker.cartera.core.domain.portfolio.Portfolio
import java.math.BigDecimal

/** Una fila de Mercado. */
@Immutable
data class StockItem(
    val symbol: String,
    val name: String,
    val price: Money,
    val change: BigDecimal,
    val changePct: BigDecimal
) {
    companion object {
        fun from(quote: Quote) = StockItem(
            symbol = quote.symbol,
            name = quote.name,
            price = Money(quote.last, quote.currency),
            change = quote.change,
            changePct = quote.changePct
        )
    }
}

/** Cabecera de Mi portafolio: valor de mercado y variación del día. */
@Immutable
data class PortfolioHeader(val marketValue: Money, val dayChange: Money, val dayChangePct: BigDecimal) {
    companion object {
        fun from(portfolio: Portfolio) = portfolio.totals.let {
            PortfolioHeader(marketValue = it.marketValue, dayChange = it.dayChange, dayChangePct = it.dayChangePct)
        }
    }
}

@Immutable
data class MarketUiState(
    val isLoading: Boolean = true,
    val error: AppError? = null,
    val stocks: List<StockItem> = emptyList(),
    val portfolio: PortfolioHeader? = null,
    /** El canal en vivo se cayó y está reconectando: los precios siguen en su último valor. */
    val isReconnecting: Boolean = false,
    /** Sube con cada carga o refresco correcto (no con los ticks): es la clave de la marca `mercado`. */
    val contentVersion: Int = 0,
    /** Camino de actualización de los precios: `QUOTES_FULL_REFRESH`. */
    val fullRefresh: Boolean = false
) {
    val contentKey: Int? get() = contentVersion.takeIf { !isLoading && error == null }

    val symbols: Set<String> get() = stocks.mapTo(LinkedHashSet()) { it.symbol }

    /** El precio de [quote] en su fila y, si se recalculó, la cabecera; un símbolo que no está no cambia nada. */
    fun withQuote(quote: Quote, header: PortfolioHeader?): MarketUiState {
        val index = stocks.indexOfFirst { it.symbol == quote.symbol }
        if (index < 0) return this
        val rows = if (fullRefresh) fullRefreshRows(quote) else perSymbolRows(index, StockItem.from(quote))
        return copy(stocks = rows, portfolio = header ?: portfolio)
    }

    private fun perSymbolRows(index: Int, item: StockItem): List<StockItem> =
        if (stocks[index] == item) stocks else stocks.toMutableList().also { it[index] = item }

    private fun fullRefreshRows(quote: Quote): List<StockItem> =
        stocks.map { if (it.symbol == quote.symbol) StockItem.from(quote) else it.copy() }
}

sealed interface MarketAction {
    data object RetryClicked : MarketAction

    /** Mercado volvió a verse: si una orden cambió las tenencias, se pide de nuevo el portafolio. */
    data object Shown : MarketAction
}
