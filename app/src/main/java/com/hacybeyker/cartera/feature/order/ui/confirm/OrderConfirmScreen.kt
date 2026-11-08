package com.hacybeyker.cartera.feature.order.ui.confirm

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hacybeyker.cartera.core.ui.marks.ReportContentVisible
import com.hacybeyker.cartera.feature.order.ui.MarketOrderArgs
import com.hacybeyker.cartera.feature.order.ui.OrderTestTags
import com.hacybeyker.cartera.feature.order.ui.toArgs

@Composable
fun OrderConfirmScreen(
    order: MarketOrderArgs,
    navigation: OrderConfirmNavigation,
    modifier: Modifier = Modifier,
    viewModel: OrderConfirmViewModel =
        hiltViewModel<OrderConfirmViewModel, OrderConfirmViewModel.Factory> { it.create(order.toOrder()) }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentNavigation by rememberUpdatedState(navigation)
    LaunchedEffect(uiState.exit) {
        when (val exit = uiState.exit) {
            is OrderConfirmExit.Done -> currentNavigation.onResult(exit.receipt.toArgs())
            OrderConfirmExit.Failed -> currentNavigation.onResult(null)
            OrderConfirmExit.BackToForm -> currentNavigation.onBack()
            OrderConfirmExit.Cancelled -> currentNavigation.onCancelled()
            null -> Unit
        }
    }
    ReportContentVisible(mark = OrderTestTags.MARK_CONFIRMATION)
    OrderConfirmContent(
        uiState = uiState,
        onAction = viewModel::onAction,
        onBack = navigation.onBack,
        modifier = modifier
    )
}
