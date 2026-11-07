package com.hacybeyker.cartera.feature.terms.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hacybeyker.cartera.core.domain.entry.EntryStep
import com.hacybeyker.cartera.core.ui.marks.ReportContentVisible

private const val MARK = "terminos"

@Composable
fun TermsScreen(
    onAccepted: (EntryStep) -> Unit,
    onRejected: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TermsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnAccepted by rememberUpdatedState(onAccepted)
    val currentOnRejected by rememberUpdatedState(onRejected)
    LaunchedEffect(uiState.exit) {
        when (val exit = uiState.exit) {
            is TermsExit.Accepted -> currentOnAccepted(exit.next)
            TermsExit.Rejected -> currentOnRejected()
            null -> Unit
        }
    }
    // Atrás equivale a Rechazar: sin términos aceptados no se puede seguir con la sesión abierta.
    BackHandler { viewModel.onAction(TermsAction.RejectClicked) }
    ReportContentVisible(mark = MARK)
    TermsContent(uiState = uiState, onAction = viewModel::onAction, modifier = modifier)
}
