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
data class Sizes(val logo: Dp = 96.dp, val illustration: Dp = 120.dp, val pageIndicator: Dp = 8.dp)

val LocalSizes = staticCompositionLocalOf { Sizes() }

val MaterialTheme.sizes: Sizes
    @Composable
    @ReadOnlyComposable
    get() = LocalSizes.current
