package com.hacybeyker.cartera.feature.market.ui.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hacybeyker.cartera.core.ui.marks.ReportContentVisible

@Composable
fun StockDetailScreen(
    symbol: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StockDetailViewModel =
        hiltViewModel<StockDetailViewModel, StockDetailViewModel.Factory> { it.create(symbol) }
) {
    // Solo en STARTED: en segundo plano se deja de recolectar y el canal en vivo se cierra.
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ReportContentVisible(mark = StockDetailTestTags.MARK, contentKey = uiState.contentKey)
    StockDetailContent(
        symbol = symbol,
        uiState = uiState,
        onAction = viewModel::onAction,
        onBack = onBack,
        modifier = modifier
    )
}
