package com.hacybeyker.cartera.core.ui.market

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import java.math.BigDecimal

/** Color de una variación: sube con el color de los abonos, baja con el de error y sin cambio, neutro. */
@Composable
@ReadOnlyComposable
fun trendColor(change: BigDecimal): Color = when (change.signum()) {
    1 -> MaterialTheme.colorScheme.primary
    -1 -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}
