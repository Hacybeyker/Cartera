package com.hacybeyker.cartera.feature.onboarding.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hacybeyker.cartera.core.domain.entry.EntryStep
import com.hacybeyker.cartera.core.ui.marks.ReportContentVisible

private const val MARK = "onboarding"

@Composable
fun OnboardingScreen(
    onFinished: (EntryStep) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnFinished by rememberUpdatedState(onFinished)
    LaunchedEffect(uiState.next) {
        uiState.next?.let { currentOnFinished(it) }
    }
    ReportContentVisible(mark = MARK)
    OnboardingContent(onAction = viewModel::onAction, modifier = modifier)
}
