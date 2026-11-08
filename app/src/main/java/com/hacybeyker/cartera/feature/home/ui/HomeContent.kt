package com.hacybeyker.cartera.feature.home.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.hacybeyker.cartera.core.designsystem.CarteraTheme
import com.hacybeyker.cartera.core.ui.preview.previewPortfolio
import com.hacybeyker.cartera.core.ui.preview.previewQuotes
import com.hacybeyker.cartera.feature.home.ui.market.MarketContent
import com.hacybeyker.cartera.feature.home.ui.market.MarketUiState
import com.hacybeyker.cartera.feature.home.ui.market.PortfolioHeader
import com.hacybeyker.cartera.feature.home.ui.market.StockItem

/** Barra inferior de Home; [tabContent] dibuja el tab elegido. */
@Composable
fun HomeContent(
    selectedTab: HomeTab,
    onTabSelected: (HomeTab) -> Unit,
    modifier: Modifier = Modifier,
    tabContent: @Composable (HomeTab) -> Unit
) {
    Scaffold(
        modifier = modifier.testTag(HomeTestTags.SCREEN),
        bottomBar = {
            NavigationBar {
                HomeTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = tab == selectedTab,
                        onClick = { onTabSelected(tab) },
                        icon = { Icon(imageVector = tab.icon, contentDescription = null) },
                        label = { Text(text = stringResource(tab.label)) },
                        modifier = Modifier.testTag(tab.testTag)
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
