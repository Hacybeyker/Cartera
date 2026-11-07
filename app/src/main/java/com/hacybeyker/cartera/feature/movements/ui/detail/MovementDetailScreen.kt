package com.hacybeyker.cartera.feature.movements.ui.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hacybeyker.cartera.core.ui.marks.ReportContentVisible
import com.hacybeyker.cartera.feature.movements.ui.MovementDetailTestTags

@Composable
fun MovementDetailScreen(
    movementId: String,
    onBack: () -> Unit,
    onBreakdownClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MovementDetailViewModel =
        hiltViewModel<MovementDetailViewModel, MovementDetailViewModel.Factory> { it.create(movementId) }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ReportContentVisible(mark = MovementDetailTestTags.MARK, contentKey = uiState.contentKey)
    MovementDetailContent(
        uiState = uiState,
        onAction = viewModel::onAction,
        onBack = onBack,
        onBreakdownClick = { onBreakdownClick(movementId) },
        modifier = modifier
    )
}
