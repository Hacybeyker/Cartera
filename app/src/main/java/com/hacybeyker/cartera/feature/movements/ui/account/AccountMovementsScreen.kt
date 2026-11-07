package com.hacybeyker.cartera.feature.movements.ui.account

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hacybeyker.cartera.core.ui.marks.ReportContentVisible
import com.hacybeyker.cartera.feature.movements.ui.AccountMovementsTestTags

@Composable
fun AccountMovementsScreen(
    accountId: String,
    onBack: () -> Unit,
    onMovementClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AccountMovementsViewModel =
        hiltViewModel<AccountMovementsViewModel, AccountMovementsViewModel.Factory> { it.create(accountId) }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ReportContentVisible(mark = AccountMovementsTestTags.MARK, contentKey = uiState.list.contentKey)
    AccountMovementsContent(
        uiState = uiState,
        onAction = viewModel::onAction,
        onBack = onBack,
        onMovementClick = onMovementClick,
        modifier = modifier
    )
}
