package com.hacybeyker.cartera.feature.splash.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SplashScreen(onReady: () -> Unit, modifier: Modifier = Modifier, viewModel: SplashViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnReady by rememberUpdatedState(onReady)
    LaunchedEffect(uiState) {
        if (uiState == SplashUiState.Ready) currentOnReady()
    }
    SplashContent(uiState = uiState, onAction = viewModel::onAction, modifier = modifier)
}
