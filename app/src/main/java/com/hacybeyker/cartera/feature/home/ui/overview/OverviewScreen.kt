package com.hacybeyker.cartera.feature.home.ui.overview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hacybeyker.cartera.core.ui.marks.ReportContentVisible

@Composable
fun OverviewScreen(
    accountsActions: AccountsTabActions,
    onMovementClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OverviewViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // Corre cada vez que Inicio entra en composición: al abrir Home y al volver desde otra pantalla o tab.
    LaunchedEffect(viewModel) { viewModel.onAction(OverviewAction.Shown) }
    if (uiState.selectedTab == OverviewTab.ACCOUNTS) {
        ReportContentVisible(mark = OverviewTestTags.MARK_ACCOUNTS, contentKey = uiState.contentKey)
    }
    OverviewContent(
        uiState = uiState,
        onAction = viewModel::onAction,
        accountsActions = accountsActions,
        modifier = modifier
    ) {
        AllMovementsTab(onMovementClick = onMovementClick)
    }
}

@Composable
private fun AllMovementsTab(onMovementClick: (String) -> Unit, viewModel: AllMovementsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.onAction(AllMovementsAction.Shown) }
    ReportContentVisible(mark = OverviewTestTags.MARK_MOVEMENTS, contentKey = uiState.contentKey)
    AllMovementsContent(uiState = uiState, onAction = viewModel::onAction, onMovementClick = onMovementClick)
}
