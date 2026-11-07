package com.hacybeyker.cartera.feature.movements.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.hacybeyker.cartera.R
import com.hacybeyker.cartera.core.designsystem.spacing

/** Fila etiqueta · valor de Detalle y Desglose. */
@Composable
internal fun LabeledRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.spacing.screen, vertical = MaterialTheme.spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md)
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.End
        )
    }
}

// Los códigos que no conoce la app se muestran tal como llegan.
@Composable
internal fun channelLabel(channel: String): String = channelRes(channel)?.let { stringResource(it) } ?: channel

@Composable
internal fun statusLabel(status: String): String = statusRes(status)?.let { stringResource(it) } ?: status

@StringRes
private fun channelRes(channel: String): Int? = when (channel) {
    "POS" -> R.string.channel_pos
    "ONLINE" -> R.string.channel_online
    "APP" -> R.string.channel_app
    "BRANCH" -> R.string.channel_branch
    else -> null
}

@StringRes
private fun statusRes(status: String): Int? = when (status) {
    "COMPLETED" -> R.string.status_completed
    else -> null
}
