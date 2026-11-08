package com.hacybeyker.cartera.feature.biometric.ui.security

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hacybeyker.cartera.core.ui.marks.ReportContentVisible

@Composable
fun SecurityScreen(onBack: () -> Unit, modifier: Modifier = Modifier, viewModel: SecurityViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ReportContentVisible(mark = SecurityTestTags.MARK, contentKey = uiState.contentKey)
    SecurityContent(uiState = uiState, onAction = viewModel::onAction, onBack = onBack, modifier = modifier)
}
