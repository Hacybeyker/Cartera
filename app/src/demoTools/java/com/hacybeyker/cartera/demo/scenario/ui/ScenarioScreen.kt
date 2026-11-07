package com.hacybeyker.cartera.demo.scenario.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ScenarioScreen(onBack: () -> Unit, modifier: Modifier = Modifier, viewModel: ScenarioViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ScenarioContent(uiState = uiState, onAction = viewModel::onAction, onBack = onBack, modifier = modifier)
}
