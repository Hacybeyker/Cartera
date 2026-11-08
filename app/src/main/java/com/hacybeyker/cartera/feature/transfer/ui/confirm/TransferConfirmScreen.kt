package com.hacybeyker.cartera.feature.transfer.ui.confirm

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hacybeyker.cartera.core.ui.marks.ReportContentVisible
import com.hacybeyker.cartera.feature.transfer.ui.TransferOrderArgs
import com.hacybeyker.cartera.feature.transfer.ui.TransferTestTags
import com.hacybeyker.cartera.feature.transfer.ui.toArgs

@Composable
fun TransferConfirmScreen(
    order: TransferOrderArgs,
    navigation: TransferConfirmNavigation,
    modifier: Modifier = Modifier,
    viewModel: TransferConfirmViewModel =
        hiltViewModel<TransferConfirmViewModel, TransferConfirmViewModel.Factory> { it.create(order.toOrder()) }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentNavigation by rememberUpdatedState(navigation)
    LaunchedEffect(uiState.exit) {
        when (val exit = uiState.exit) {
            is TransferConfirmExit.Done -> currentNavigation.onResult(exit.receipt.toArgs())
            TransferConfirmExit.Failed -> currentNavigation.onResult(null)
            TransferConfirmExit.BackToForm -> currentNavigation.onBack()
            TransferConfirmExit.Cancelled -> currentNavigation.onCancelled()
            null -> Unit
        }
    }
    ReportContentVisible(mark = TransferTestTags.MARK_CONFIRMATION)
    TransferConfirmContent(
        uiState = uiState,
        onAction = viewModel::onAction,
        onBack = navigation.onBack,
        modifier = modifier
    )
}
