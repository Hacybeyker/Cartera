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
import com.hacybeyker.cartera.core.ui.marks.STATIC_CONTENT

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
    // Se avisa cuando ya se sabe si va "Ingresar con huella", para que la marca vea la pantalla completa.
    ReportContentVisible(mark = LoginTestTags.MARK, contentKey = STATIC_CONTENT.takeIf { uiState.isBiometricChecked })
    LoginContent(uiState = uiState, onAction = viewModel::onAction, modifier = modifier)
}
