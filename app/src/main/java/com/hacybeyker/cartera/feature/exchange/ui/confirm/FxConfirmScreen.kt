package com.hacybeyker.cartera.feature.exchange.ui.confirm

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hacybeyker.cartera.core.ui.marks.ReportContentVisible
import com.hacybeyker.cartera.feature.exchange.ui.ExchangeOrderArgs
import com.hacybeyker.cartera.feature.exchange.ui.FxTestTags
import com.hacybeyker.cartera.feature.exchange.ui.toArgs

@Composable
fun FxConfirmScreen(
    order: ExchangeOrderArgs,
    navigation: FxConfirmNavigation,
    modifier: Modifier = Modifier,
    viewModel: FxConfirmViewModel =
        hiltViewModel<FxConfirmViewModel, FxConfirmViewModel.Factory> { it.create(order.toOrder()) }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentNavigation by rememberUpdatedState(navigation)
    LaunchedEffect(uiState.exit) {
        when (val exit = uiState.exit) {
            is FxConfirmExit.Done -> currentNavigation.onResult(exit.receipt.toArgs(uiState.order))
            FxConfirmExit.Failed -> currentNavigation.onResult(null)
            FxConfirmExit.BackToForm -> currentNavigation.onBack()
            FxConfirmExit.Cancelled -> currentNavigation.onCancelled()
            null -> Unit
        }
    }
    ReportContentVisible(mark = FxTestTags.MARK_CONFIRMATION)
    FxConfirmContent(uiState = uiState, onAction = viewModel::onAction, onBack = navigation.onBack, modifier = modifier)
}
