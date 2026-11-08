package com.hacybeyker.cartera.feature.market.ui.detail

import com.hacybeyker.cartera.core.domain.market.Quote
import com.hacybeyker.cartera.core.domain.market.StockDetail
import com.hacybeyker.cartera.core.domain.portfolio.Holding
import com.hacybeyker.cartera.core.ui.preview.previewHistoryPrices

/** El detalle cargado de [quote] en 1D, con los datos del día de QORI en los fixtures y [holding] si se pasa. */
internal fun previewStockDetailState(quote: Quote, holding: Holding?): StockDetailUiState {
    val stock = StockDetail(
        quote = quote,
        open = "116.52".toBigDecimal(),
        high = "117.75".toBigDecimal(),
        low = "112.88".toBigDecimal(),
        previousClose = quote.last - quote.change,
        volume = 2_795_000,
        description = "Exploración y producción de oro en el norte del país."
    )
    return StockDetailUiState(
        isLoading = false,
        name = quote.name,
        price = LivePrice.from(quote),
        day = DayData.from(stock),
        holding = holding?.let(HoldingSummary::from),
        chart = ChartUiState(isLoading = false, chart = PriceChart.from(previewHistoryPrices, quote.last)),
        contentVersion = 1
    )
}
