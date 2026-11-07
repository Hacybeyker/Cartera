package com.hacybeyker.cartera.feature.movements.ui.account

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.hacybeyker.cartera.R
import com.hacybeyker.cartera.core.designsystem.CarteraTheme
import com.hacybeyker.cartera.core.designsystem.component.BackTopBar
import com.hacybeyker.cartera.core.designsystem.component.EmptyState
import com.hacybeyker.cartera.core.designsystem.component.ErrorState
import com.hacybeyker.cartera.core.designsystem.component.LoadingState
import com.hacybeyker.cartera.core.designsystem.spacing
import com.hacybeyker.cartera.core.domain.account.Account
import com.hacybeyker.cartera.core.ui.format.formatted
import com.hacybeyker.cartera.core.ui.isRetryable
import com.hacybeyker.cartera.core.ui.messageRes
import com.hacybeyker.cartera.core.ui.movement.MovementListUiState
import com.hacybeyker.cartera.core.ui.movement.PagedMovementList
import com.hacybeyker.cartera.core.ui.paging.PagedList
import com.hacybeyker.cartera.core.ui.preview.previewAccounts
import com.hacybeyker.cartera.core.ui.preview.previewMovements
import com.hacybeyker.cartera.feature.movements.ui.AccountMovementsTestTags

/** Movimientos de una cuenta: cabecera con el saldo y lista paginada. */
@Composable
fun AccountMovementsContent(
    uiState: AccountMovementsUiState,
    onAction: (AccountMovementsAction) -> Unit,
    onBack: () -> Unit,
    onMovementClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize().testTag(AccountMovementsTestTags.SCREEN)) {
        BackTopBar(title = uiState.account?.alias ?: stringResource(R.string.account_title), onBack = onBack)
        val list = uiState.list
        val error = list.error
        val account = uiState.account
        when {
            list.isLoading -> LoadingState(screen = AccountMovementsTestTags.STATES)
            error != null -> ErrorState(
                screen = AccountMovementsTestTags.STATES,
                message = stringResource(error.messageRes()),
                onRetry = { onAction(AccountMovementsAction.RetryClicked) }.takeIf { error.isRetryable }
            )
            account == null -> Unit
            list.isEmpty -> {
                AccountHeader(account)
                EmptyState(screen = AccountMovementsTestTags.STATES, message = stringResource(R.string.movements_empty))
            }
            else -> PagedMovementList(
                movements = list.movements,
                tags = AccountMovementsTestTags.list,
                onMovementClick = onMovementClick,
                onLoadMore = { onAction(AccountMovementsAction.LoadMore) },
                onRetryLoadMore = { onAction(AccountMovementsAction.RetryLoadMoreClicked) },
                modifier = Modifier.fillMaxSize(),
                header = { item(key = AccountMovementsTestTags.BALANCE) { AccountHeader(account) } }
            )
        }
    }
}

@Composable
private fun AccountHeader(account: Account) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(MaterialTheme.spacing.screen)
    ) {
        Text(
            text = stringResource(R.string.account_header_number, account.numberMasked),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = account.availableBalance.formatted(),
            modifier = Modifier.testTag(AccountMovementsTestTags.BALANCE),
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = stringResource(R.string.overview_available_balance),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    HorizontalDivider()
}

@PreviewLightDark
@Composable
private fun AccountMovementsContentPreview() {
    CarteraTheme {
        AccountMovementsContent(
            uiState = AccountMovementsUiState(
                account = previewAccounts[1],
                list = MovementListUiState(isLoading = false, movements = PagedList(items = previewMovements))
            ),
            onAction = {},
            onBack = {},
            onMovementClick = {}
        )
    }
}
