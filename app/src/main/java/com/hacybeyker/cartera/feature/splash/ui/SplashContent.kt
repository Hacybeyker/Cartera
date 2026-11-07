package com.hacybeyker.cartera.feature.splash.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.hacybeyker.cartera.R
import com.hacybeyker.cartera.core.designsystem.CarteraTheme
import com.hacybeyker.cartera.core.designsystem.sizes
import com.hacybeyker.cartera.core.designsystem.spacing
import com.hacybeyker.cartera.core.domain.AppError
import com.hacybeyker.cartera.core.ui.messageRes

@Composable
fun SplashContent(uiState: SplashUiState, onAction: (SplashAction) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(MaterialTheme.spacing.screen)
            .testTag(SplashTestTags.SCREEN),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.size(MaterialTheme.sizes.logo)
        )
        Text(text = stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
        Text(text = stringResource(R.string.splash_tagline), style = MaterialTheme.typography.bodyLarge)
        when (uiState) {
            SplashUiState.Loading -> SplashProgress()
            SplashUiState.Ready -> Unit
            is SplashUiState.Error -> SplashError(uiState.error, onRetry = { onAction(SplashAction.RetryClicked) })
        }
    }
}

@Composable
private fun SplashProgress() {
    val description = stringResource(R.string.common_loading)
    CircularProgressIndicator(
        modifier = Modifier
            .testTag(SplashTestTags.LOADING)
            .semantics { contentDescription = description }
    )
}

@Composable
private fun SplashError(error: AppError, onRetry: () -> Unit) {
    Text(
        text = stringResource(error.messageRes()),
        modifier = Modifier.testTag(SplashTestTags.ERROR),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.error,
        textAlign = TextAlign.Center
    )
    Button(onClick = onRetry, modifier = Modifier.testTag(SplashTestTags.RETRY)) {
        Text(text = stringResource(R.string.common_retry))
    }
}

@PreviewLightDark
@Composable
private fun SplashLoadingPreview() {
    CarteraTheme {
        SplashContent(uiState = SplashUiState.Loading, onAction = {})
    }
}

@PreviewLightDark
@Composable
private fun SplashErrorPreview() {
    CarteraTheme {
        SplashContent(uiState = SplashUiState.Error(AppError.Server), onAction = {})
    }
}
