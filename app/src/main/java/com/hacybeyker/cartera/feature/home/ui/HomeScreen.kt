package com.hacybeyker.cartera.feature.home.ui

import androidx.activity.compose.BackHandler
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hacybeyker.cartera.feature.home.ui.exchange.ExchangeScreen
import com.hacybeyker.cartera.feature.home.ui.market.MarketScreen
import com.hacybeyker.cartera.feature.home.ui.menu.HomeMenuItem
import com.hacybeyker.cartera.feature.home.ui.more.MoreScreen
import com.hacybeyker.cartera.feature.home.ui.overview.AccountsTabActions
import com.hacybeyker.cartera.feature.home.ui.overview.OverviewScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    actions: HomeActions,
    modifier: Modifier = Modifier,
    tabRequest: HomeTabRequest? = null,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableStateOf(HomeTab.START) }
    // El tab pedido se dibuja desde el primer frame; el efecto solo lo deja como elegido y consume el pedido.
    val requestedTab = tabRequest?.tab
    LaunchedEffect(requestedTab) {
        requestedTab?.let {
            selectedTab = it
            tabRequest.consume()
        }
    }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    BackHandler(enabled = drawerState.isOpen || drawerState.targetValue == DrawerValue.Open) {
        scope.launch { drawerState.close() }
    }
    val currentOnLoggedOut by rememberUpdatedState(actions.onLoggedOut)
    LaunchedEffect(uiState.isLoggedOut) {
        if (uiState.isLoggedOut) currentOnLoggedOut()
    }
    // Cada tab guarda su estado (scroll, tab superior) mientras se usa otro; sus ViewModels viven con la entrada.
    val tabStates = rememberSaveableStateHolder()
    HomeContent(
        selectedTab = requestedTab ?: selectedTab,
        onTabSelected = { selectedTab = it },
        modifier = modifier,
        uiState = uiState,
        onAction = viewModel::onAction,
        drawerState = drawerState,
        onMenuItemClick = { item ->
            when (item) {
                HomeMenuItem.EXCHANGE -> selectedTab = HomeTab.EXCHANGE
                HomeMenuItem.LOG_OUT -> viewModel.onAction(HomeAction.LogoutClicked)
                else -> Unit
            }
            scope.closeThen(drawerState) { actions.open(item) }
        }
    ) { tab ->
        tabStates.SaveableStateProvider(key = tab.name) {
            HomeTabContent(tab = tab, actions = actions, onMenuClick = { scope.launch { drawerState.open() } })
        }
    }
}

// Se navega cuando el menú terminó de cerrarse: así la entrada de Home queda guardada con el menú cerrado.
private fun CoroutineScope.closeThen(drawerState: DrawerState, then: () -> Unit) {
    launch {
        drawerState.close()
        then()
    }
}

private fun HomeActions.open(item: HomeMenuItem) {
    when (item) {
        HomeMenuItem.PORTFOLIO -> onPortfolioClick()
        HomeMenuItem.SECURITY -> onSecurityClick()
        HomeMenuItem.HELP -> onHelpClick()
        HomeMenuItem.EXCHANGE, HomeMenuItem.LOG_OUT -> Unit
    }
}

@Composable
private fun HomeTabContent(tab: HomeTab, actions: HomeActions, onMenuClick: () -> Unit) {
    when (tab) {
        HomeTab.START -> OverviewScreen(
            accountsActions = AccountsTabActions(
                onAccountClick = actions.onAccountClick,
                onTransferClick = actions.onTransferClick
            ),
            onMovementClick = actions.onMovementClick,
            onMenuClick = onMenuClick
        )
        HomeTab.MARKET -> MarketScreen(
            onStockClick = actions.onStockClick,
            onPortfolioClick = actions.onPortfolioClick
        )
        HomeTab.EXCHANGE -> ExchangeScreen(onContinue = actions.onExchangeContinue)
        HomeTab.MORE -> MoreScreen(
            onLegacyClick = actions.onLegacyClick,
            onSecurityClick = actions.onSecurityClick,
            onLoggedOut = actions.onLoggedOut,
            onHiddenScreenRequested = actions.onHiddenScreenRequested
        )
    }
}
