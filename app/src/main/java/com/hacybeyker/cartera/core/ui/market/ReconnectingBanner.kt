package com.hacybeyker.cartera.core.ui.market

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.hacybeyker.cartera.R
import com.hacybeyker.cartera.core.designsystem.sizes
import com.hacybeyker.cartera.core.designsystem.spacing

/** Aviso "Reconectando…" de las pantallas con precios en vivo; [testTag] es el de cada pantalla. */
@Composable
fun ReconnectingBanner(testTag: String, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag)
            .semantics { liveRegion = LiveRegionMode.Polite }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = MaterialTheme.spacing.screen, vertical = MaterialTheme.spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(MaterialTheme.sizes.inlineProgress),
                strokeWidth = MaterialTheme.sizes.inlineProgressStroke
            )
            Text(text = stringResource(R.string.market_reconnecting), style = MaterialTheme.typography.bodyMedium)
        }
    }
}
