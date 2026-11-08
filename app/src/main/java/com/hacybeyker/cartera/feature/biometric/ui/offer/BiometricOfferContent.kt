package com.hacybeyker.cartera.feature.biometric.ui.offer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.hacybeyker.cartera.R
import com.hacybeyker.cartera.core.designsystem.CarteraTheme
import com.hacybeyker.cartera.core.designsystem.component.BlockingProgress
import com.hacybeyker.cartera.core.designsystem.component.ErrorDialog
import com.hacybeyker.cartera.core.designsystem.sizes
import com.hacybeyker.cartera.core.designsystem.spacing
import com.hacybeyker.cartera.core.ui.messageRes

/** Activar abre el diálogo de huella del sistema, que no tiene testTags. */
@Composable
fun BiometricOfferContent(
    uiState: BiometricOfferUiState,
    onAction: (BiometricOfferAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(MaterialTheme.spacing.screen)
            .testTag(BiometricOfferTestTags.SCREEN),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Filled.Lock,
            contentDescription = null,
            modifier = Modifier.size(MaterialTheme.sizes.illustration),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = stringResource(R.string.biometric_offer_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(R.string.biometric_offer_body),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        if (uiState.isUnavailable) {
            Text(
                text = stringResource(R.string.biometric_unavailable),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
        OfferButtons(onAction = onAction)
    }
    if (uiState.isActivating) BlockingProgress()
    uiState.dialogError?.let { error ->
        ErrorDialog(
            title = stringResource(R.string.biometric_enable_error_title),
            message = stringResource(error.messageRes()),
            onRetry = { onAction(BiometricOfferAction.RetryClicked) },
            onCancel = { onAction(BiometricOfferAction.ErrorDismissed) }
        )
    }
}

@Composable
private fun OfferButtons(onAction: (BiometricOfferAction) -> Unit) {
    Button(
        onClick = { onAction(BiometricOfferAction.ActivateClicked) },
        modifier = Modifier
            .fillMaxWidth()
            .testTag(BiometricOfferTestTags.ACTIVATE)
    ) {
        Text(text = stringResource(R.string.biometric_offer_activate))
    }
    TextButton(
        onClick = { onAction(BiometricOfferAction.SkipClicked) },
        modifier = Modifier
            .fillMaxWidth()
            .testTag(BiometricOfferTestTags.SKIP)
    ) {
        Text(text = stringResource(R.string.biometric_offer_skip))
    }
}

@PreviewLightDark
@Composable
private fun BiometricOfferContentPreview() {
    CarteraTheme {
        BiometricOfferContent(uiState = BiometricOfferUiState(), onAction = {})
    }
}
