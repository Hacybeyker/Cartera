package com.hacybeyker.cartera.feature.terms.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.hacybeyker.cartera.R
import com.hacybeyker.cartera.core.designsystem.CarteraTheme
import com.hacybeyker.cartera.core.designsystem.component.BlockingProgress
import com.hacybeyker.cartera.core.designsystem.component.ErrorDialog
import com.hacybeyker.cartera.core.designsystem.spacing
import com.hacybeyker.cartera.core.ui.messageRes

// Sin testTags a propósito: estas pantallas suelen venir de terceros y se prueban por texto.
@Composable
fun TermsContent(uiState: TermsUiState, onAction: (TermsAction) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(MaterialTheme.spacing.screen),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md)
    ) {
        Text(text = stringResource(R.string.terms_title), style = MaterialTheme.typography.headlineSmall)
        Text(
            text = stringResource(R.string.terms_body),
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            style = MaterialTheme.typography.bodyMedium
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(
                    value = uiState.isChecked,
                    role = Role.Checkbox,
                    onValueChange = { onAction(TermsAction.CheckedChanged(it)) }
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = uiState.isChecked, onCheckedChange = null)
            Text(
                text = stringResource(R.string.terms_checkbox),
                modifier = Modifier.padding(start = MaterialTheme.spacing.sm),
                style = MaterialTheme.typography.bodyLarge
            )
        }
        Button(
            onClick = { onAction(TermsAction.AcceptClicked) },
            enabled = uiState.canAccept,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(R.string.terms_accept))
        }
        OutlinedButton(onClick = { onAction(TermsAction.RejectClicked) }, modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(R.string.terms_reject))
        }
    }
    TermsDialogs(uiState = uiState, onAction = onAction)
}

@Composable
private fun TermsDialogs(uiState: TermsUiState, onAction: (TermsAction) -> Unit) {
    if (uiState.isSaving) BlockingProgress()
    uiState.error?.let { error ->
        ErrorDialog(
            title = stringResource(R.string.terms_error_title),
            message = stringResource(error.messageRes()),
            onRetry = { onAction(TermsAction.RetryClicked) },
            onCancel = { onAction(TermsAction.ErrorDismissed) }
        )
    }
}

@PreviewLightDark
@Composable
private fun TermsUncheckedPreview() {
    CarteraTheme {
        TermsContent(uiState = TermsUiState(), onAction = {})
    }
}

@PreviewLightDark
@Composable
private fun TermsCheckedPreview() {
    CarteraTheme {
        TermsContent(uiState = TermsUiState(isChecked = true), onAction = {})
    }
}
