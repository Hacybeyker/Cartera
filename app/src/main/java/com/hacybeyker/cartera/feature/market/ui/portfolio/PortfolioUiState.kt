package com.hacybeyker.cartera.feature.market.ui.portfolio

import androidx.compose.runtime.Immutable
import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.domain.Money
import com.hacybeyker.cartera.core.domain.portfolio.Holding
import com.hacybeyker.cartera.core.domain.portfolio.Portfolio
import java.math.BigDecimal

/** Una tenencia de Mi portafolio, valorizada al último precio. */
@Immutable
data class HoldingItem(
    val symbol: String,
    val name: String,
    val quantity: Int,
    val averagePrice: Money,
    val lastPrice: Money,
    val marketValue: Money,
    val gain: Money,
    val gainPct: BigDecimal
) {
    companion object {
        fun from(holding: Holding) = HoldingItem(
            symbol = holding.symbol,
            name = holding.name,
            quantity = holding.quantity,
            averagePrice = Money(holding.averagePrice, holding.currency),
            lastPrice = Money(holding.lastPrice, holding.currency),
            marketValue = holding.marketValue,
            gain = holding.gain,
            gainPct = holding.gainPct
        )
    }
}

/** Totales de la cabecera: valor de mercado, ganancia sobre el costo y variación del día. */
@Immutable
data class PortfolioSummary(
    val marketValue: Money,
    val gain: Money,
    val gainPct: BigDecimal,
    val dayChange: Money,
    val dayChangePct: BigDecimal
) {
    companion object {
        fun from(portfolio: Portfolio) = portfolio.totals.let {
            PortfolioSummary(
                marketValue = it.marketValue,
                gain = it.gain,
                gainPct = it.gainPct,
                dayChange = it.dayChange,
                dayChangePct = it.dayChangePct
            )
        }
    }
}

@Immutable
data class PortfolioUiState(
    val isLoading: Boolean = true,
    val error: AppError? = null,
    val holdings: List<HoldingItem> = emptyList(),
    val summary: PortfolioSummary? = null,
    /** El canal en vivo se cayó y está reconectando: los precios siguen en su último valor. */
    val isReconnecting: Boolean = false,
    /** Sube con cada carga o refresco correcto (no con los ticks): es la clave de la marca `portafolio`. */
    val contentVersion: Int = 0
) {
    val contentKey: Int? get() = contentVersion.takeIf { !isLoading && error == null }

    val symbols: Set<String> get() = holdings.mapTo(LinkedHashSet()) { it.symbol }

    fun loaded(portfolio: Portfolio): PortfolioUiState = copy(
        isLoading = false,
        error = null,
        holdings = portfolio.holdings.map(HoldingItem::from),
        summary = PortfolioSummary.from(portfolio),
        contentVersion = contentVersion + 1
    )

    /** Cambia la fila de [symbol] y los totales. */
    fun revalued(portfolio: Portfolio, symbol: String): PortfolioUiState {
        val index = holdings.indexOfFirst { it.symbol == symbol }
        val holding = portfolio.holdings.firstOrNull { it.symbol == symbol }
        if (index < 0 || holding == null) return this
        val rows = holdings.toMutableList().also { it[index] = HoldingItem.from(holding) }
        return copy(holdings = rows, summary = PortfolioSummary.from(portfolio))
    }
}

sealed interface PortfolioAction {
    data object RetryClicked : PortfolioAction

    /** Mi portafolio volvió a verse: si una orden cambió las tenencias, se pide de nuevo. */
    data object Shown : PortfolioAction
}
