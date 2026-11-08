package com.hacybeyker.cartera.feature.home.ui.more

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.hacybeyker.cartera.R
import com.hacybeyker.cartera.core.designsystem.CarteraTheme
import com.hacybeyker.cartera.core.designsystem.component.CommonTestTags
import com.hacybeyker.cartera.core.designsystem.spacing
import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.ui.messageRes
import com.hacybeyker.cartera.feature.home.ui.LogOutConfirmationDialog

/** Más: el menú se puede usar aunque falle `GET /me`; solo la cabecera con el nombre depende de esa carga. */
@Composable
fun MoreContent(
    uiState: MoreUiState,
    version: String,
    onAction: (MoreAction) -> Unit,
    onLegacyClick: () -> Unit,
    onSecurityClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .testTag(MoreTestTags.SCREEN)
    ) {
        ProfileHeader(uiState = uiState, onRetry = { onAction(MoreAction.RetryClicked) })
        HorizontalDivider()
        MenuItem(
            title = stringResource(R.string.more_security),
            testTag = MoreTestTags.SECURITY,
            onClick = onSecurityClick
        )
        MenuItem(
            title = stringResource(R.string.more_legacy),
            testTag = MoreTestTags.LEGACY,
            onClick = onLegacyClick
        )
        MenuItem(
            title = stringResource(R.string.more_log_out),
            testTag = MoreTestTags.LOG_OUT,
            onClick = { onAction(MoreAction.LogoutClicked) }
        )
        MenuItem(
            title = stringResource(R.string.more_version),
            supporting = version,
            testTag = MoreTestTags.VERSION,
            onClick = { onAction(MoreAction.VersionTapped) }
        )
    }
    if (uiState.isLogoutConfirmationVisible) {
        LogOutConfirmationDialog(
            onConfirm = { onAction(MoreAction.LogoutConfirmed) },
            onCancel = { onAction(MoreAction.LogoutCancelled) }
        )
    }
}

@Composable
private fun ProfileHeader(uiState: MoreUiState, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(MaterialTheme.spacing.screen),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)
    ) {
        val error = uiState.error
        when {
            uiState.isLoading -> HeaderLoading()
            error != null -> HeaderError(error = error, onRetry = onRetry)
            else -> Text(
                text = uiState.fullName.orEmpty(),
                modifier = Modifier.testTag(MoreTestTags.NAME),
                style = MaterialTheme.typography.headlineSmall
            )
        }
    }
}

@Composable
private fun HeaderLoading() {
    val description = stringResource(R.string.common_loading)
    CircularProgressIndicator(
        modifier = Modifier
            .testTag(CommonTestTags.loading(MoreTestTags.MORE))
            .semantics { contentDescription = description }
    )
}

@Composable
private fun HeaderError(error: AppError, onRetry: () -> Unit) {
    Text(
        text = stringResource(error.messageRes()),
        modifier = Modifier.testTag(CommonTestTags.error(MoreTestTags.MORE)),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error
    )
    TextButton(onClick = onRetry, modifier = Modifier.testTag(CommonTestTags.retry(MoreTestTags.MORE))) {
        Text(text = stringResource(R.string.common_retry))
    }
}

@Composable
private fun MenuItem(title: String, testTag: String, onClick: () -> Unit, supporting: String? = null) {
    ListItem(
        headlineContent = { Text(text = title) },
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag(testTag),
        supportingContent = supporting?.let { { Text(text = it) } }
    )
}

@PreviewLightDark
@Composable
private fun MoreContentPreview() {
    CarteraTheme {
        MoreContent(
            uiState = MoreUiState(isLoading = false, fullName = "Ana Torres"),
            version = "0.1.0",
            onAction = {},
            onLegacyClick = {},
            onSecurityClick = {}
        )
    }
}
