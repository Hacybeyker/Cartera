package com.hacybeyker.cartera.feature.order.ui.form

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hacybeyker.cartera.core.domain.market.OrderSide
import com.hacybeyker.cartera.core.ui.marks.ReportContentVisible
import com.hacybeyker.cartera.feature.order.ui.MarketOrderArgs
import com.hacybeyker.cartera.feature.order.ui.OrderTestTags
import com.hacybeyker.cartera.feature.order.ui.toArgs

@Composable
fun OrderFormScreen(
    symbol: String,
    side: OrderSide,
    onContinue: (MarketOrderArgs) -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OrderFormViewModel =
        hiltViewModel<OrderFormViewModel, OrderFormViewModel.Factory> { it.create(symbol, side) }
) {
    // Solo en STARTED: en segundo plano se deja de recolectar y el canal en vivo se cierra.
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnContinue by rememberUpdatedState(onContinue)
    val currentOnExit by rememberUpdatedState(onExit)
    LaunchedEffect(uiState.order) {
        val order = uiState.order ?: return@LaunchedEffect
        viewModel.onAction(OrderFormAction.OrderOpened)
        currentOnContinue(order.toArgs())
    }
    LaunchedEffect(uiState.isExited) {
        if (uiState.isExited) currentOnExit()
    }
    // Atrás con datos pregunta antes de descartarlos.
    BackHandler { viewModel.onAction(OrderFormAction.BackClicked) }
    ReportContentVisible(mark = OrderTestTags.MARK_FORM, contentKey = uiState.contentKey)
    OrderFormContent(uiState = uiState, onAction = viewModel::onAction, modifier = modifier)
}
