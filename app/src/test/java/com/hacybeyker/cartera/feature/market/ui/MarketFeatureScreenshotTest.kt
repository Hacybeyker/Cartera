package com.hacybeyker.cartera.feature.market.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.domain.market.HistoryRange
import com.hacybeyker.cartera.core.testing.TestPortfolio
import com.hacybeyker.cartera.core.testing.TestQuotes
import com.hacybeyker.cartera.core.testing.captureLightAndDark
import com.hacybeyker.cartera.feature.market.ui.detail.ChartUiState
import com.hacybeyker.cartera.feature.market.ui.detail.StockDetailContent
import com.hacybeyker.cartera.feature.market.ui.detail.StockDetailUiState
import com.hacybeyker.cartera.feature.market.ui.detail.previewStockDetailState
import com.hacybeyker.cartera.feature.market.ui.portfolio.PortfolioContent
import com.hacybeyker.cartera.feature.market.ui.portfolio.PortfolioUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel5)
class MarketFeatureScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private val qori = TestQuotes.single { it.symbol == "QORI" }

    // Más alto que el Pixel 5 para que entren la tenencia y los datos del día sin desplazar.
    @Test
    @Config(qualifiers = TALL)
    fun stockDetailWithHolding() = compose.captureLightAndDark("accion_contenido_tenencia") {
        StockDetailContent(
            symbol = qori.symbol,
            uiState = previewStockDetailState(qori, TestPortfolio.holdings.single { it.symbol == "QORI" }),
            onAction = {},
            onBack = {}
        )
    }

    @Test
    @Config(qualifiers = TALL)
    fun stockDetailWithoutHolding() = compose.captureLightAndDark("accion_sin_tenencia") {
        StockDetailContent(
            symbol = qori.symbol,
            uiState = previewStockDetailState(qori, holding = null),
            onAction = {},
            onBack = {}
        )
    }

    @Test
    fun tradeSheetWithHolding() = compose.captureLightAndDark("accion_sheet_operar", withWindows = true) {
        StockDetailContent(
            symbol = qori.symbol,
            uiState = previewStockDetailState(qori, TestPortfolio.holdings.single { it.symbol == "QORI" })
                .copy(isTradeSheetVisible = true),
            onAction = {},
            onBack = {}
        )
    }

    @Test
    fun tradeSheetWithoutHolding() =
        compose.captureLightAndDark("accion_sheet_operar_sin_tenencia", withWindows = true) {
            StockDetailContent(
                symbol = qori.symbol,
                uiState = previewStockDetailState(qori, holding = null).copy(isTradeSheetVisible = true),
                onAction = {},
                onBack = {}
            )
        }

    @Test
    fun stockDetailLoading() = compose.captureLightAndDark("accion_cargando") {
        StockDetailContent(symbol = qori.symbol, uiState = StockDetailUiState(), onAction = {}, onBack = {})
    }

    @Test
    fun stockDetailError() = compose.captureLightAndDark("accion_error") {
        StockDetailContent(
            symbol = qori.symbol,
            uiState = StockDetailUiState(isLoading = false, error = AppError.Network),
            onAction = {},
            onBack = {}
        )
    }

    @Test
    fun stockDetailNotFound() = compose.captureLightAndDark("accion_no_encontrada") {
        StockDetailContent(
            symbol = "NOPE",
            uiState = StockDetailUiState(isLoading = false, error = AppError.NotFound),
            onAction = {},
            onBack = {}
        )
    }

    @Test
    fun stockDetailChartError() = compose.captureLightAndDark("accion_grafico_error") {
        val loaded = previewStockDetailState(qori, holding = null)
        StockDetailContent(
            symbol = qori.symbol,
            uiState = loaded.copy(
                range = HistoryRange.MONTH,
                chart = ChartUiState(isLoading = false, error = AppError.Network)
            ),
            onAction = {},
            onBack = {}
        )
    }

    @Test
    fun portfolio() = compose.captureLightAndDark("portafolio_contenido") {
        PortfolioContent(
            uiState = PortfolioUiState().loaded(TestPortfolio),
            onAction = {},
            onBack = {},
            onStockClick = {}
        )
    }

    @Test
    fun portfolioEmpty() = compose.captureLightAndDark("portafolio_vacio") {
        PortfolioContent(
            uiState = PortfolioUiState().loaded(TestPortfolio.copy(holdings = emptyList())),
            onAction = {},
            onBack = {},
            onStockClick = {}
        )
    }

    @Test
    fun portfolioError() = compose.captureLightAndDark("portafolio_error") {
        PortfolioContent(
            uiState = PortfolioUiState(isLoading = false, error = AppError.Network),
            onAction = {},
            onBack = {},
            onStockClick = {}
        )
    }

    private companion object {
        const val TALL = "+h1200dp"
    }
}
