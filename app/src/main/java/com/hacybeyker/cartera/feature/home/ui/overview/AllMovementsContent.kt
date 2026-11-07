package com.hacybeyker.cartera.feature.home.ui.overview

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.hacybeyker.cartera.R
import com.hacybeyker.cartera.core.designsystem.CarteraTheme
import com.hacybeyker.cartera.core.designsystem.component.EmptyState
import com.hacybeyker.cartera.core.designsystem.component.ErrorState
import com.hacybeyker.cartera.core.designsystem.component.LoadingState
import com.hacybeyker.cartera.core.ui.isRetryable
import com.hacybeyker.cartera.core.ui.messageRes
import com.hacybeyker.cartera.core.ui.movement.MovementListUiState
import com.hacybeyker.cartera.core.ui.movement.PagedMovementList
import com.hacybeyker.cartera.core.ui.paging.PagedList
import com.hacybeyker.cartera.core.ui.preview.previewMovements

/** Inicio · Movimientos: los de todas las cuentas, paginados. */
@Composable
fun AllMovementsContent(
    uiState: MovementListUiState,
    onAction: (AllMovementsAction) -> Unit,
    onMovementClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val error = uiState.error
    when {
        uiState.isLoading -> LoadingState(screen = OverviewTestTags.MOVEMENTS, modifier = modifier)
        error != null -> ErrorState(
            screen = OverviewTestTags.MOVEMENTS,
            message = stringResource(error.messageRes()),
            onRetry = { onAction(AllMovementsAction.RetryClicked) }.takeIf { error.isRetryable },
            modifier = modifier
        )
        uiState.isEmpty -> EmptyState(
            screen = OverviewTestTags.MOVEMENTS,
            message = stringResource(R.string.movements_empty),
            modifier = modifier
        )
        else -> PagedMovementList(
            movements = uiState.movements,
            tags = OverviewTestTags.movementList,
            onMovementClick = onMovementClick,
            onLoadMore = { onAction(AllMovementsAction.LoadMore) },
            onRetryLoadMore = { onAction(AllMovementsAction.RetryLoadMoreClicked) },
            modifier = modifier.fillMaxSize()
        )
    }
}

@PreviewLightDark
@Composable
private fun AllMovementsContentPreview() {
    CarteraTheme {
        AllMovementsContent(
            uiState = MovementListUiState(isLoading = false, movements = PagedList(items = previewMovements)),
            onAction = {},
            onMovementClick = {}
        )
    }
}
