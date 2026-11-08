package com.hacybeyker.cartera.feature.home.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.hacybeyker.cartera.core.designsystem.component.ComingSoonState
import com.hacybeyker.cartera.core.ui.marks.ReportContentVisible
import com.hacybeyker.cartera.feature.home.ui.market.MarketScreen
import com.hacybeyker.cartera.feature.home.ui.more.MoreScreen
import com.hacybeyker.cartera.feature.home.ui.overview.AccountsTabActions
import com.hacybeyker.cartera.feature.home.ui.overview.OverviewScreen

@Composable
fun HomeScreen(actions: HomeActions, modifier: Modifier = Modifier, tabRequest: HomeTabRequest? = null) {
    var selectedTab by rememberSaveable { mutableStateOf(HomeTab.START) }
    val requestedTab = tabRequest?.tab
    LaunchedEffect(requestedTab) {
        requestedTab?.let {
            selectedTab = it
            tabRequest.consume()
        }
    }
    // Cada tab guarda su estado (scroll, tab superior) mientras se usa otro; sus ViewModels viven con la entrada.
    val tabStates = rememberSaveableStateHolder()
    HomeContent(selectedTab = selectedTab, onTabSelected = { selectedTab = it }, modifier = modifier) { tab ->
        tabStates.SaveableStateProvider(key = tab.name) {
            when (tab) {
                HomeTab.START -> OverviewScreen(
                    accountsActions = AccountsTabActions(
                        onAccountClick = actions.onAccountClick,
                        onTransferClick = actions.onTransferClick
                    ),
                    onMovementClick = actions.onMovementClick
                )
                HomeTab.MARKET -> MarketScreen(
                    onStockClick = actions.onStockClick,
                    onPortfolioClick = actions.onPortfolioClick
                )
                HomeTab.EXCHANGE -> ComingSoonTab(screen = HomeTestTags.EXCHANGE)
                HomeTab.MORE -> MoreScreen(
                    onLegacyClick = actions.onLegacyClick,
                    onLoggedOut = actions.onLoggedOut,
                    onHiddenScreenRequested = actions.onHiddenScreenRequested
                )
            }
        }
    }
}

@Composable
private fun ComingSoonTab(screen: String) {
    ReportContentVisible(mark = screen)
    ComingSoonState(screen = screen)
}
