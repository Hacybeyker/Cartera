package com.hacybeyker.cartera.feature.transfer.ui.form

import androidx.activity.compose.BackHandler
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
fun TransferFormScreen(
    onContinue: (TransferOrderArgs) -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TransferFormViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnContinue by rememberUpdatedState(onContinue)
    val currentOnExit by rememberUpdatedState(onExit)
    LaunchedEffect(uiState.order) {
        val order = uiState.order ?: return@LaunchedEffect
        viewModel.onAction(TransferFormAction.OrderOpened)
        currentOnContinue(order.toArgs())
    }
    LaunchedEffect(uiState.isExited) {
        if (uiState.isExited) currentOnExit()
    }
    // Atrás con datos pregunta antes de descartarlos.
    BackHandler { viewModel.onAction(TransferFormAction.BackClicked) }
    ReportContentVisible(mark = TransferTestTags.MARK_FORM, contentKey = uiState.contentKey)
    TransferFormContent(uiState = uiState, onAction = viewModel::onAction, modifier = modifier)
}
