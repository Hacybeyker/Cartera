package com.hacybeyker.cartera.feature.home.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.domain.movement.Movement
import com.hacybeyker.cartera.core.testing.captureLightAndDark
import com.hacybeyker.cartera.core.ui.movement.MovementListUiState
import com.hacybeyker.cartera.core.ui.paging.PagedList
import com.hacybeyker.cartera.core.ui.preview.previewAccounts
import com.hacybeyker.cartera.core.ui.preview.previewMovements
import com.hacybeyker.cartera.core.ui.preview.previewPortfolio
import com.hacybeyker.cartera.core.ui.preview.previewQuotes
import com.hacybeyker.cartera.feature.home.ui.market.MarketContent
import com.hacybeyker.cartera.feature.home.ui.market.MarketUiState
import com.hacybeyker.cartera.feature.home.ui.market.PortfolioHeader
import com.hacybeyker.cartera.feature.home.ui.market.StockItem
import com.hacybeyker.cartera.feature.home.ui.more.MoreContent
import com.hacybeyker.cartera.feature.home.ui.more.MoreUiState
import com.hacybeyker.cartera.feature.home.ui.overview.AccountsTabActions
import com.hacybeyker.cartera.feature.home.ui.overview.AllMovementsContent
import com.hacybeyker.cartera.feature.home.ui.overview.OverviewContent
import com.hacybeyker.cartera.feature.home.ui.overview.OverviewTab
import com.hacybeyker.cartera.feature.home.ui.overview.OverviewUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel5)
class HomeScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private val noActions = AccountsTabActions(onAccountClick = {}, onTransferClick = {})
    private val loadedMarket = MarketUiState(
        isLoading = false,
        stocks = previewQuotes.map(StockItem::from),
        portfolio = PortfolioHeader.from(previewPortfolio)
    )
    private val loadedOverview = OverviewUiState(isLoading = false, firstName = "Ana", accounts = previewAccounts)

    @Test
    fun homeMarketTab() = compose.captureLightAndDark("home_mercado") {
        HomeContent(selectedTab = HomeTab.MARKET, onTabSelected = {}) {
            MarketContent(uiState = loadedMarket, onAction = {}, onStockClick = {}, onPortfolioClick = {})
        }
    }

    @Test
    fun marketLoaded() = compose.captureLightAndDark("mercado_contenido") {
        MarketContent(uiState = loadedMarket, onAction = {}, onStockClick = {}, onPortfolioClick = {})
    }

    @Test
    fun marketLoading() = compose.captureLightAndDark("mercado_cargando") {
        MarketContent(uiState = MarketUiState(), onAction = {}, onStockClick = {}, onPortfolioClick = {})
    }

    @Test
    fun marketReconnecting() = compose.captureLightAndDark("mercado_reconectando") {
        MarketContent(
            uiState = loadedMarket.copy(isReconnecting = true),
            onAction = {},
            onStockClick = {},
            onPortfolioClick = {}
        )
    }

    @Test
    fun marketError() = compose.captureLightAndDark("mercado_error") {
        MarketContent(
            uiState = MarketUiState(isLoading = false, error = AppError.Network),
            onAction = {},
            onStockClick = {},
            onPortfolioClick = {}
        )
    }

    @Test
    fun overviewAccounts() = compose.captureLightAndDark("inicio_cuentas") {
        OverviewContent(uiState = loadedOverview, onAction = {}, accountsActions = noActions, onMenuClick = {}) {}
    }

    @Test
    fun overviewLoading() = compose.captureLightAndDark("inicio_cargando") {
        OverviewContent(uiState = OverviewUiState(), onAction = {}, accountsActions = noActions, onMenuClick = {}) {}
    }

    @Test
    fun overviewError() = compose.captureLightAndDark("inicio_error") {
        OverviewContent(
            uiState = OverviewUiState(isLoading = false, error = AppError.Network),
            onAction = {},
            accountsActions = noActions,
            onMenuClick = {}
        ) {}
    }

    @Test
    fun overviewMovementsTab() = compose.captureLightAndDark("inicio_movimientos") {
        OverviewContent(
            uiState = loadedOverview.copy(selectedTab = OverviewTab.MOVEMENTS),
            onAction = {},
            accountsActions = noActions,
            onMenuClick = {}
        ) {
            AllMovementsContent(uiState = movements(PagedList(previewMovements)), onAction = {}, onMovementClick = {})
        }
    }

    @Test
    fun movementsLoadMoreError() = compose.captureLightAndDark("movimientos_mas_error") {
        AllMovementsContent(
            uiState = movements(
                PagedList(previewMovements, nextCursor = "MOV-001-0001", loadMoreError = AppError.Network)
            ),
            onAction = {},
            onMovementClick = {}
        )
    }

    @Test
    fun movementsLoadingMore() = compose.captureLightAndDark("movimientos_mas_cargando") {
        AllMovementsContent(
            uiState = movements(PagedList(previewMovements, nextCursor = "MOV-001-0001", isLoadingMore = true)),
            onAction = {},
            onMovementClick = {}
        )
    }

    @Test
    fun movementsEmpty() = compose.captureLightAndDark("movimientos_vacio") {
        AllMovementsContent(uiState = movements(PagedList()), onAction = {}, onMovementClick = {})
    }

    @Test
    fun more() = compose.captureLightAndDark("mas_contenido") {
        MoreContent(uiState = MoreUiState(isLoading = false, fullName = "Ana Torres"), version = "0.1.0", onAction = {
        }, onLegacyClick = {}, onSecurityClick = {})
    }

    @Test
    fun moreError() = compose.captureLightAndDark("mas_error") {
        MoreContent(
            uiState = MoreUiState(isLoading = false, error = AppError.Server),
            version = "0.1.0",
            onAction = {},
            onLegacyClick = {},
            onSecurityClick = {}
        )
    }

    @Test
    fun moreLogOutConfirmation() = compose.captureLightAndDark("mas_cerrar_sesion", withWindows = true) {
        MoreContent(
            uiState = MoreUiState(isLoading = false, fullName = "Ana Torres", isLogoutConfirmationVisible = true),
            version = "0.1.0",
            onAction = {},
            onLegacyClick = {},
            onSecurityClick = {}
        )
    }

    private fun movements(list: PagedList<Movement>) = MovementListUiState(isLoading = false, movements = list)
}
