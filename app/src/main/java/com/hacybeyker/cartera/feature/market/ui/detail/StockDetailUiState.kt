package com.hacybeyker.cartera.feature.market.ui.detail

import androidx.compose.runtime.Immutable
import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.domain.Money
import com.hacybeyker.cartera.core.domain.market.HistoryRange
import com.hacybeyker.cartera.core.domain.market.Quote
import com.hacybeyker.cartera.core.domain.market.StockDetail
import com.hacybeyker.cartera.core.domain.portfolio.Holding
import java.math.BigDecimal

/** Precio vigente y su variación contra el cierre anterior. */
@Immutable
data class LivePrice(val price: Money, val change: BigDecimal, val changePct: BigDecimal) {
    companion object {
        fun from(quote: Quote) = LivePrice(
            price = Money(quote.last, quote.currency),
            change = quote.change,
            changePct = quote.changePct
        )
    }
}

/** Datos del día, tal como llegan en el detalle. */
@Immutable
data class DayData(
    val open: Money,
    val high: Money,
    val low: Money,
    val previousClose: Money,
    val volume: Long,
    val description: String
) {
    companion object {
        fun from(stock: StockDetail): DayData {
            val currency = stock.quote.currency
            return DayData(
                open = Money(stock.open, currency),
                high = Money(stock.high, currency),
                low = Money(stock.low, currency),
                previousClose = Money(stock.previousClose, currency),
                volume = stock.volume,
                description = stock.description
            )
        }
    }
}

/** La tenencia propia de la acción, valorizada al precio vigente. */
@Immutable
data class HoldingSummary(
    val quantity: Int,
    val averagePrice: Money,
    val marketValue: Money,
    val gain: Money,
    val gainPct: BigDecimal
) {
    companion object {
        fun from(holding: Holding) = HoldingSummary(
            quantity = holding.quantity,
            averagePrice = Money(holding.averagePrice, holding.currency),
            marketValue = holding.marketValue,
            gain = holding.gain,
            gainPct = holding.gainPct
        )
    }
}

/** El gráfico del rango elegido: carga y error propios, sin tapar el resto del detalle. */
@Immutable
data class ChartUiState(val isLoading: Boolean = true, val error: AppError? = null, val chart: PriceChart? = null) {
    fun withLivePrice(price: BigDecimal): ChartUiState = chart?.let { copy(chart = it.withLast(price)) } ?: this
}

@Immutable
data class StockDetailUiState(
    val isLoading: Boolean = true,
    val error: AppError? = null,
    val name: String = "",
    val price: LivePrice? = null,
    val day: DayData? = null,
    val holding: HoldingSummary? = null,
    val range: HistoryRange = HistoryRange.DAY,
    val chart: ChartUiState = ChartUiState(),
    /** El canal en vivo se cayó y está reconectando: el precio sigue en su último valor. */
    val isReconnecting: Boolean = false,
    /** Sube con la carga y con cada rango cargado (no con los ticks): es la clave de la marca `accion_detalle`. */
    val contentVersion: Int = 0,
    /** El sheet Comprar/Vender que abre Operar. */
    val isTradeSheetVisible: Boolean = false
) {
    /** Vender solo tiene sentido con acciones de este símbolo. */
    val canSell: Boolean get() = holding != null

    val contentKey: Int? get() = contentVersion.takeIf { !isLoading && error == null && price != null }
}

sealed interface StockDetailAction {
    data object RetryClicked : StockDetailAction

    data object ChartRetryClicked : StockDetailAction

    data class RangeSelected(val range: HistoryRange) : StockDetailAction

    /** La pantalla volvió a verse: si una orden cambió las tenencias, se vuelve a pedir el portafolio. */
    data object Shown : StockDetailAction

    data object TradeClicked : StockDetailAction

    data object TradeSheetDismissed : StockDetailAction
}
