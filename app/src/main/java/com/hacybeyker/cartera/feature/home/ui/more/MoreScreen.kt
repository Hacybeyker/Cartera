package com.hacybeyker.cartera.feature.home.ui.more

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hacybeyker.cartera.BuildConfig
import com.hacybeyker.cartera.core.ui.marks.ReportContentVisible

@Composable
fun MoreScreen(
    onLegacyClick: () -> Unit,
    onSecurityClick: () -> Unit,
    onLoggedOut: () -> Unit,
    onHiddenScreenRequested: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MoreViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnLoggedOut by rememberUpdatedState(onLoggedOut)
    val currentOnHiddenScreenRequested by rememberUpdatedState(onHiddenScreenRequested)
    LaunchedEffect(uiState.isLoggedOut) {
        if (uiState.isLoggedOut) currentOnLoggedOut()
    }
    LaunchedEffect(uiState.isHiddenScreenRequested) {
        if (!uiState.isHiddenScreenRequested) return@LaunchedEffect
        currentOnHiddenScreenRequested()
        viewModel.onAction(MoreAction.HiddenScreenOpened)
    }
    ReportContentVisible(mark = MoreTestTags.MORE, contentKey = uiState.contentKey)
    MoreContent(
        uiState = uiState,
        version = BuildConfig.VERSION_NAME,
        onAction = viewModel::onAction,
        onLegacyClick = onLegacyClick,
        onSecurityClick = onSecurityClick,
        modifier = modifier
    )
}
