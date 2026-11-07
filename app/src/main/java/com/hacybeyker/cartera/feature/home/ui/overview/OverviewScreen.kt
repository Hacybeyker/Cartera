package com.hacybeyker.cartera.feature.home.ui.overview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hacybeyker.cartera.core.ui.marks.ReportContentVisible

@Composable
fun OverviewScreen(
    onAccountClick: (String) -> Unit,
    onMovementClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OverviewViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    if (uiState.selectedTab == OverviewTab.ACCOUNTS) {
        ReportContentVisible(mark = OverviewTestTags.MARK_ACCOUNTS, contentKey = uiState.contentKey)
    }
    OverviewContent(
        uiState = uiState,
        onAction = viewModel::onAction,
        onAccountClick = onAccountClick,
        modifier = modifier
    ) {
        AllMovementsTab(onMovementClick = onMovementClick)
    }
}

@Composable
private fun AllMovementsTab(onMovementClick: (String) -> Unit, viewModel: AllMovementsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ReportContentVisible(mark = OverviewTestTags.MARK_MOVEMENTS, contentKey = uiState.contentKey)
    AllMovementsContent(uiState = uiState, onAction = viewModel::onAction, onMovementClick = onMovementClick)
}
