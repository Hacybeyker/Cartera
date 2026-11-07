package com.hacybeyker.cartera.feature.movements.ui.breakdown

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hacybeyker.cartera.core.ui.marks.ReportContentVisible
import com.hacybeyker.cartera.feature.movements.ui.MovementBreakdownTestTags

@Composable
fun MovementBreakdownScreen(
    movementId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MovementBreakdownViewModel =
        hiltViewModel<MovementBreakdownViewModel, MovementBreakdownViewModel.Factory> { it.create(movementId) }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ReportContentVisible(mark = MovementBreakdownTestTags.MARK, contentKey = uiState.contentKey)
    MovementBreakdownContent(uiState = uiState, onAction = viewModel::onAction, onBack = onBack, modifier = modifier)
}
