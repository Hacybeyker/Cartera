package com.hacybeyker.cartera.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.hacybeyker.cartera.R
import com.hacybeyker.cartera.core.designsystem.CarteraTheme
import com.hacybeyker.cartera.core.designsystem.spacing

/** Carga de una pantalla. [screen] es el prefijo de sus testTags (`<pantalla>_cargando`). */
@Composable
fun LoadingState(screen: String, modifier: Modifier = Modifier) {
    val description = stringResource(R.string.common_loading)
    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag(CommonTestTags.loading(screen))
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

/**
 * Error a pantalla completa (`<pantalla>_error`) con Reintentar (`<pantalla>_reintentar`). Sin [onRetry] no hay
 * botón: es un error que reintentar no arregla, como un id que no existe.
 */
@Composable
fun ErrorState(screen: String, message: String, onRetry: (() -> Unit)?, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(MaterialTheme.spacing.screen)
            .testTag(CommonTestTags.error(screen)),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(imageVector = Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
        Text(text = message, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        if (onRetry != null) {
            Button(onClick = onRetry, modifier = Modifier.testTag(CommonTestTags.retry(screen))) {
                Text(text = stringResource(R.string.common_retry))
            }
        }
    }
}

/** Carga correcta sin nada que mostrar (`<pantalla>_vacio`). */
@Composable
fun EmptyState(screen: String, message: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(MaterialTheme.spacing.screen)
            .testTag(CommonTestTags.empty(screen)),
        contentAlignment = Alignment.Center
    ) {
        Text(text = message, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
    }
}

/** Pantalla reservada (`<pantalla>_proximamente`). */
@Composable
fun ComingSoonState(screen: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(MaterialTheme.spacing.screen)
            .testTag(CommonTestTags.comingSoon(screen)),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = stringResource(R.string.common_coming_soon), style = MaterialTheme.typography.headlineSmall)
        Text(
            text = stringResource(R.string.common_coming_soon_body),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
    }
}

@PreviewLightDark
@Composable
private fun LoadingStatePreview() {
    CarteraTheme {
        LoadingState(screen = "preview")
    }
}

@PreviewLightDark
@Composable
private fun ComingSoonStatePreview() {
    CarteraTheme {
        ComingSoonState(screen = "preview")
    }
}

@PreviewLightDark
@Composable
private fun ErrorStatePreview() {
    CarteraTheme {
        ErrorState(screen = "preview", message = stringResource(R.string.error_network), onRetry = {})
    }
}
