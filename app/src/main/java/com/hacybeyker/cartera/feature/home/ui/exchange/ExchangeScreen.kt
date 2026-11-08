package com.hacybeyker.cartera.feature.home.ui.exchange

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hacybeyker.cartera.core.domain.exchange.ExchangeOrder
import com.hacybeyker.cartera.core.ui.marks.ReportContentVisible

@Composable
fun ExchangeScreen(
    onContinue: (ExchangeOrder) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExchangeViewModel = hiltViewModel()
) {
    // Solo en STARTED y con el tab a la vista: fuera de eso se deja de recolectar y el canal en vivo se cierra.
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnContinue by rememberUpdatedState(onContinue)
    LaunchedEffect(viewModel) { viewModel.onAction(ExchangeAction.Shown) }
    LaunchedEffect(uiState.order) {
        val order = uiState.order ?: return@LaunchedEffect
        viewModel.onAction(ExchangeAction.OrderOpened)
        currentOnContinue(order)
    }
    ReportContentVisible(mark = ExchangeTestTags.EXCHANGE, contentKey = uiState.contentKey)
    ExchangeContent(uiState = uiState, onAction = viewModel::onAction, modifier = modifier)
}
