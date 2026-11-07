package com.hacybeyker.cartera.feature.home.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.hacybeyker.cartera.core.designsystem.component.ComingSoonState
import com.hacybeyker.cartera.core.ui.marks.ReportContentVisible
import com.hacybeyker.cartera.feature.home.ui.more.MoreScreen
import com.hacybeyker.cartera.feature.home.ui.overview.OverviewScreen

@Composable
fun HomeScreen(actions: HomeActions, modifier: Modifier = Modifier) {
    var selectedTab by rememberSaveable { mutableStateOf(HomeTab.START) }
    // Cada tab guarda su estado (scroll, tab superior) mientras se usa otro; sus ViewModels viven con la entrada.
    val tabStates = rememberSaveableStateHolder()
    HomeContent(selectedTab = selectedTab, onTabSelected = { selectedTab = it }, modifier = modifier) { tab ->
        tabStates.SaveableStateProvider(key = tab.name) {
            when (tab) {
                HomeTab.START -> OverviewScreen(
                    onAccountClick = actions.onAccountClick,
                    onMovementClick = actions.onMovementClick
                )
                HomeTab.MARKET -> ComingSoonTab(screen = HomeTestTags.MARKET)
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
