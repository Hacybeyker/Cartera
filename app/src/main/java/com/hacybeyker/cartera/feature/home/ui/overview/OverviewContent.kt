package com.hacybeyker.cartera.feature.home.ui.overview

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.hacybeyker.cartera.R
import com.hacybeyker.cartera.core.designsystem.CarteraTheme
import com.hacybeyker.cartera.core.designsystem.component.EmptyState
import com.hacybeyker.cartera.core.designsystem.component.ErrorState
import com.hacybeyker.cartera.core.designsystem.component.LoadingState
import com.hacybeyker.cartera.core.designsystem.spacing
import com.hacybeyker.cartera.core.domain.account.Account
import com.hacybeyker.cartera.core.ui.format.formatted
import com.hacybeyker.cartera.core.ui.isRetryable
import com.hacybeyker.cartera.core.ui.messageRes
import com.hacybeyker.cartera.core.ui.preview.previewAccounts
import com.hacybeyker.cartera.feature.home.domain.model.CoachMark
import com.hacybeyker.cartera.feature.home.ui.HomeTestTags
import com.hacybeyker.cartera.feature.home.ui.coach.coachMarkTarget

/**
 * Inicio: barra con el botón del menú lateral y el saludo, y tabs Cuentas · Movimientos. [movementsTab] dibuja el tab
 * de movimientos, que tiene su estado.
 */
@Composable
fun OverviewContent(
    uiState: OverviewUiState,
    onAction: (OverviewAction) -> Unit,
    accountsActions: AccountsTabActions,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
    movementsTab: @Composable () -> Unit
) {
    val tabStates = rememberSaveableStateHolder()
    Column(modifier = modifier.fillMaxSize().testTag(OverviewTestTags.SCREEN)) {
        OverviewTopBar(firstName = uiState.firstName, onMenuClick = onMenuClick)
        PrimaryTabRow(selectedTabIndex = uiState.selectedTab.ordinal) {
            OverviewTab.entries.forEach { tab ->
                Tab(
                    selected = tab == uiState.selectedTab,
                    onClick = { onAction(OverviewAction.TabSelected(tab)) },
                    modifier = Modifier.testTag(tab.testTag),
                    text = { Text(text = stringResource(tab.label)) }
                )
            }
        }
        Box(modifier = Modifier.weight(1f)) {
            tabStates.SaveableStateProvider(key = uiState.selectedTab.name) {
                when (uiState.selectedTab) {
                    OverviewTab.ACCOUNTS -> AccountsTab(uiState, onAction, accountsActions)
                    OverviewTab.MOVEMENTS -> movementsTab()
                }
            }
        }
    }
}

@Composable
private fun OverviewTopBar(firstName: String?, onMenuClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = MaterialTheme.spacing.xs,
                end = MaterialTheme.spacing.screen,
                top = MaterialTheme.spacing.sm,
                bottom = MaterialTheme.spacing.sm
            ),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onMenuClick,
            modifier = Modifier
                .coachMarkTarget(CoachMark.MENU)
                .testTag(HomeTestTags.MENU)
        ) {
            Icon(imageVector = Icons.Filled.Menu, contentDescription = stringResource(R.string.home_menu_open))
        }
        Text(
            text = firstName?.let { stringResource(R.string.overview_greeting, it) }.orEmpty(),
            modifier = Modifier.testTag(OverviewTestTags.GREETING),
            style = MaterialTheme.typography.headlineSmall
        )
    }
}

private val OverviewTab.label: Int
    get() = when (this) {
        OverviewTab.ACCOUNTS -> R.string.overview_tab_accounts
        OverviewTab.MOVEMENTS -> R.string.overview_tab_movements
    }

private val OverviewTab.testTag: String
    get() = when (this) {
        OverviewTab.ACCOUNTS -> OverviewTestTags.TAB_ACCOUNTS
        OverviewTab.MOVEMENTS -> OverviewTestTags.TAB_MOVEMENTS
    }

@Composable
private fun AccountsTab(uiState: OverviewUiState, onAction: (OverviewAction) -> Unit, actions: AccountsTabActions) {
    val error = uiState.error
    when {
        uiState.isLoading -> LoadingState(screen = OverviewTestTags.ACCOUNTS)
        error != null -> ErrorState(
            screen = OverviewTestTags.ACCOUNTS,
            message = stringResource(error.messageRes()),
            onRetry = { onAction(OverviewAction.RetryClicked) }.takeIf { error.isRetryable }
        )
        uiState.accounts.isEmpty() -> EmptyState(
            screen = OverviewTestTags.ACCOUNTS,
            message = stringResource(R.string.overview_accounts_empty)
        )
        else -> AccountList(accounts = uiState.accounts, actions = actions)
    }
}

@Composable
private fun AccountList(accounts: List<Account>, actions: AccountsTabActions) {
    LazyColumn(modifier = Modifier.fillMaxSize().testTag(OverviewTestTags.ACCOUNTS_LIST)) {
        items(items = accounts, key = { it.id }) { account ->
            AccountRow(account = account, onClick = { actions.onAccountClick(account.id) })
            HorizontalDivider()
        }
        item {
            Button(
                onClick = actions.onTransferClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.spacing.screen)
                    .coachMarkTarget(CoachMark.TRANSFER)
                    .testTag(OverviewTestTags.TRANSFER)
            ) {
                Text(text = stringResource(R.string.overview_transfer))
            }
        }
    }
}

@Composable
private fun AccountRow(account: Account, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = MaterialTheme.spacing.screen, vertical = MaterialTheme.spacing.md)
            .testTag(OverviewTestTags.account(account.id)),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = account.alias, style = MaterialTheme.typography.titleMedium)
            Text(
                text = account.numberMasked,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(text = account.availableBalance.formatted(), style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(R.string.overview_available_balance),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun OverviewContentPreview() {
    CarteraTheme {
        OverviewContent(
            uiState = OverviewUiState(isLoading = false, firstName = "Ana", accounts = previewAccounts),
            onAction = {},
            accountsActions = AccountsTabActions(onAccountClick = {}, onTransferClick = {}),
            onMenuClick = {}
        ) {}
    }
}
