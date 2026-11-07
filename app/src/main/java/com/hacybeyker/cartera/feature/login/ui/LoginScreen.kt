package com.hacybeyker.cartera.feature.login.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hacybeyker.cartera.core.domain.entry.EntryStep
import com.hacybeyker.cartera.core.ui.marks.ReportContentVisible

@Composable
fun LoginScreen(
    onLoggedIn: (EntryStep) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnLoggedIn by rememberUpdatedState(onLoggedIn)
    LaunchedEffect(uiState.nextStep) {
        uiState.nextStep?.let { currentOnLoggedIn(it) }
    }
    ReportContentVisible(mark = LoginTestTags.MARK)
    LoginContent(uiState = uiState, onAction = viewModel::onAction, modifier = modifier)
}
