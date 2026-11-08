package com.hacybeyker.cartera.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Tamaños de elementos que no son espaciado (logos, ilustraciones, indicadores). */
@Immutable
data class Sizes(
    val logo: Dp = 96.dp,
    val illustration: Dp = 120.dp,
    val pageIndicator: Dp = 8.dp,
    val listIcon: Dp = 40.dp,
    /** Progreso dentro de una línea de texto, como el aviso de reconexión. */
    val inlineProgress: Dp = 16.dp,
    val inlineProgressStroke: Dp = 2.dp,
    val chartHeight: Dp = 200.dp,
    val chartStroke: Dp = 2.dp,
    /** Borde y esquinas del hueco que resalta un elemento bajo los coach marks. */
    val highlightStroke: Dp = 2.dp,
    val highlightCorner: Dp = 12.dp
)

val LocalSizes = staticCompositionLocalOf { Sizes() }

val MaterialTheme.sizes: Sizes
    @Composable
    @ReadOnlyComposable
    get() = LocalSizes.current
