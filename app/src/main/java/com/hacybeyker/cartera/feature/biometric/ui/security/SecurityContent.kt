package com.hacybeyker.cartera.feature.biometric.ui.security

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.hacybeyker.cartera.R
import com.hacybeyker.cartera.core.designsystem.CarteraTheme
import com.hacybeyker.cartera.core.designsystem.component.BackTopBar
import com.hacybeyker.cartera.core.designsystem.component.BlockingProgress
import com.hacybeyker.cartera.core.designsystem.component.ErrorDialog
import com.hacybeyker.cartera.core.ui.messageRes

/** Encender el interruptor abre el diálogo de huella del sistema; apagarlo revoca el dispositivo. */
@Composable
fun SecurityContent(
    uiState: SecurityUiState,
    onAction: (SecurityAction) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag(SecurityTestTags.SCREEN)
    ) {
        BackTopBar(title = stringResource(R.string.security_title), onBack = onBack)
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            if (!uiState.isLoading) BiometricRow(uiState = uiState, onAction = onAction)
        }
    }
    SecurityDialogs(uiState = uiState, onAction = onAction)
}

@Composable
private fun BiometricRow(uiState: SecurityUiState, onAction: (SecurityAction) -> Unit) {
    val supporting = if (uiState.isAvailable) R.string.security_biometric_body else R.string.biometric_unavailable
    ListItem(
        headlineContent = { Text(text = stringResource(R.string.security_biometric)) },
        modifier = Modifier
            .toggleable(
                value = uiState.isBiometricEnabled,
                enabled = uiState.isAvailable && !uiState.isWorking,
                role = Role.Switch,
                onValueChange = { onAction(SecurityAction.BiometricToggled(it)) }
            )
            .testTag(SecurityTestTags.BIOMETRIC),
        supportingContent = {
            Text(
                text = stringResource(supporting),
                color = if (uiState.isAvailable) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.error
                }
            )
        },
        trailingContent = {
            Switch(checked = uiState.isBiometricEnabled, onCheckedChange = null, enabled = uiState.isAvailable)
        }
    )
}

@Composable
private fun SecurityDialogs(uiState: SecurityUiState, onAction: (SecurityAction) -> Unit) {
    if (uiState.isWorking) BlockingProgress()
    val error = uiState.dialogError ?: return
    val title = when (uiState.failedChange) {
        SecurityChange.DISABLE -> R.string.biometric_disable_error_title
        else -> R.string.biometric_enable_error_title
    }
    ErrorDialog(
        title = stringResource(title),
        message = stringResource(error.messageRes()),
        onRetry = { onAction(SecurityAction.RetryClicked) },
        onCancel = { onAction(SecurityAction.ErrorDismissed) }
    )
}

@PreviewLightDark
@Composable
private fun SecurityEnabledPreview() {
    CarteraTheme {
        SecurityContent(
            uiState = SecurityUiState(isLoading = false, isAvailable = true, isBiometricEnabled = true),
            onAction = {},
            onBack = {}
        )
    }
}

@PreviewLightDark
@Composable
private fun SecurityUnavailablePreview() {
    CarteraTheme {
        SecurityContent(uiState = SecurityUiState(isLoading = false), onAction = {}, onBack = {})
    }
}
