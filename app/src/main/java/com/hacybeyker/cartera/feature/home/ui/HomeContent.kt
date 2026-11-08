package com.hacybeyker.cartera.feature.home.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.hacybeyker.cartera.core.designsystem.CarteraTheme
import com.hacybeyker.cartera.core.ui.preview.previewPortfolio
import com.hacybeyker.cartera.core.ui.preview.previewQuotes
import com.hacybeyker.cartera.feature.home.domain.model.CoachMark
import com.hacybeyker.cartera.feature.home.ui.coach.CoachMarkTargets
import com.hacybeyker.cartera.feature.home.ui.coach.CoachMarksContent
import com.hacybeyker.cartera.feature.home.ui.coach.LocalCoachMarkTargets
import com.hacybeyker.cartera.feature.home.ui.coach.coachMarkTarget
import com.hacybeyker.cartera.feature.home.ui.market.MarketContent
import com.hacybeyker.cartera.feature.home.ui.market.MarketUiState
import com.hacybeyker.cartera.feature.home.ui.market.PortfolioHeader
import com.hacybeyker.cartera.feature.home.ui.market.StockItem
import com.hacybeyker.cartera.feature.home.ui.menu.HomeMenu
import com.hacybeyker.cartera.feature.home.ui.menu.HomeMenuItem

/**
 * Barra inferior, menú lateral y coach marks de Home; [tabContent] dibuja el tab elegido. Solo el botón de Inicio abre
 * el menú: deslizar sirve para cerrarlo, no para abrirlo.
 */
@Composable
fun HomeContent(
    selectedTab: HomeTab,
    onTabSelected: (HomeTab) -> Unit,
    modifier: Modifier = Modifier,
    uiState: HomeUiState = HomeUiState(),
    onAction: (HomeAction) -> Unit = {},
    drawerState: DrawerState = rememberDrawerState(DrawerValue.Closed),
    onMenuItemClick: (HomeMenuItem) -> Unit = {},
    tabContent: @Composable (HomeTab) -> Unit
) {
    val coachTargets = remember { CoachMarkTargets() }
    CompositionLocalProvider(LocalCoachMarkTargets provides coachTargets) {
        Box(modifier = modifier.fillMaxSize().testTag(HomeTestTags.SCREEN)) {
            ModalNavigationDrawer(
                drawerContent = { HomeMenu(onItemClick = onMenuItemClick) },
                drawerState = drawerState,
                gesturesEnabled = drawerState.isOpen
            ) {
                HomeScaffold(selectedTab = selectedTab, onTabSelected = onTabSelected, tabContent = tabContent)
            }
            uiState.coachMark?.let { mark ->
                CoachMarksContent(
                    mark = mark,
                    targetBounds = coachTargets[mark],
                    onNext = { onAction(HomeAction.CoachNextClicked) },
                    onSkip = { onAction(HomeAction.CoachSkipClicked) }
                )
            }
        }
    }
    if (uiState.isLogoutConfirmationVisible) {
        LogOutConfirmationDialog(
            onConfirm = { onAction(HomeAction.LogoutConfirmed) },
            onCancel = { onAction(HomeAction.LogoutCancelled) }
        )
    }
}

@Composable
private fun HomeScaffold(
    selectedTab: HomeTab,
    onTabSelected: (HomeTab) -> Unit,
    tabContent: @Composable (HomeTab) -> Unit
) {
    Scaffold(
        bottomBar = {
            NavigationBar {
                HomeTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = tab == selectedTab,
                        onClick = { onTabSelected(tab) },
                        icon = { Icon(imageVector = tab.icon, contentDescription = null) },
                        label = { Text(text = stringResource(tab.label)) },
                        modifier = Modifier
                            .coachMarkTargetOf(tab)
                            .testTag(tab.testTag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
        ) {
            tabContent(selectedTab)
        }
    }
}

/** El tab Mercado es el paso 2 de los coach marks. */
private fun Modifier.coachMarkTargetOf(tab: HomeTab): Modifier =
    if (tab == HomeTab.MARKET) coachMarkTarget(CoachMark.MARKET_TAB) else this

@PreviewLightDark
@Composable
private fun HomeContentPreview() {
    CarteraTheme {
        HomeContent(selectedTab = HomeTab.MARKET, onTabSelected = {}) {
            MarketContent(
                uiState = MarketUiState(
                    isLoading = false,
                    stocks = previewQuotes.map(StockItem::from),
                    portfolio = PortfolioHeader.from(previewPortfolio)
                ),
                onAction = {},
                onStockClick = {},
                onPortfolioClick = {}
            )
        }
    }
}
