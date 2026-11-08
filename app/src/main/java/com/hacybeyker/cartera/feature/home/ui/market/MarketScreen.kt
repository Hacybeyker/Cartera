package com.hacybeyker.cartera.feature.home.ui.market

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hacybeyker.cartera.core.ui.marks.ReportContentVisible

@Composable
fun MarketScreen(
    onStockClick: (symbol: String) -> Unit,
    onPortfolioClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MarketViewModel = hiltViewModel()
) {
    // Solo en STARTED: en segundo plano se deja de recolectar y el canal en vivo se cierra.
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.onAction(MarketAction.Shown) }
    ReportContentVisible(mark = MarketTestTags.MARKET, contentKey = uiState.contentKey)
    MarketContent(
        uiState = uiState,
        onAction = viewModel::onAction,
        onStockClick = onStockClick,
        onPortfolioClick = onPortfolioClick,
        modifier = modifier
    )
}
