package com.hacybeyker.cartera.feature.market.ui.portfolio

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hacybeyker.cartera.core.ui.marks.ReportContentVisible

@Composable
fun PortfolioScreen(
    onBack: () -> Unit,
    onStockClick: (symbol: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PortfolioViewModel = hiltViewModel()
) {
    // Solo en STARTED: en segundo plano se deja de recolectar y el canal en vivo se cierra.
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.onAction(PortfolioAction.Shown) }
    ReportContentVisible(mark = PortfolioTestTags.PORTFOLIO, contentKey = uiState.contentKey)
    PortfolioContent(
        uiState = uiState,
        onAction = viewModel::onAction,
        onBack = onBack,
        onStockClick = onStockClick,
        modifier = modifier
    )
}
