package com.hacybeyker.cartera.feature.home.ui.market

import androidx.compose.runtime.Immutable
import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.domain.Money
import com.hacybeyker.cartera.core.domain.market.Quote
import com.hacybeyker.cartera.core.domain.portfolio.Portfolio
import java.math.BigDecimal

/** Una fila de Mercado. Inmutable: Compose salta las filas cuyo precio no cambió. */
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
    val contentVersion: Int = 0
) {
    val contentKey: Int? get() = contentVersion.takeIf { !isLoading && error == null }

    val symbols: Set<String> get() = stocks.mapTo(LinkedHashSet()) { it.symbol }

    /** Cambia solo la fila de [quote] (las demás conservan su instancia) y, si se recalculó, la cabecera. */
    fun withQuote(quote: Quote, header: PortfolioHeader?): MarketUiState {
        val index = stocks.indexOfFirst { it.symbol == quote.symbol }
        if (index < 0) return this
        val item = StockItem.from(quote)
        val rows = if (stocks[index] == item) stocks else stocks.toMutableList().also { it[index] = item }
        return copy(stocks = rows, portfolio = header ?: portfolio)
    }
}

sealed interface MarketAction {
    data object RetryClicked : MarketAction

    /** Mercado volvió a verse: si una orden cambió las tenencias, se pide de nuevo el portafolio. */
    data object Shown : MarketAction
}
