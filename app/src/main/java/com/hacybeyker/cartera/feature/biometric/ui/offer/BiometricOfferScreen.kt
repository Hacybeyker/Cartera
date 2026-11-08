package com.hacybeyker.cartera.feature.biometric.ui.offer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hacybeyker.cartera.core.ui.marks.ReportContentVisible

@Composable
fun BiometricOfferScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BiometricOfferViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnFinished by rememberUpdatedState(onFinished)
    LaunchedEffect(uiState.isFinished) {
        if (uiState.isFinished) currentOnFinished()
    }
    ReportContentVisible(mark = BiometricOfferTestTags.MARK)
    BiometricOfferContent(uiState = uiState, onAction = viewModel::onAction, modifier = modifier)
}
