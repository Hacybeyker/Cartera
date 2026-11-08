package com.hacybeyker.cartera.feature.legacy.ui

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** Abre el módulo `:legacy` (servicios clásicos) en su propia Activity y vuelve a la pantalla anterior al salir. */
@Serializable
data object LegacyNavKey : NavKey
