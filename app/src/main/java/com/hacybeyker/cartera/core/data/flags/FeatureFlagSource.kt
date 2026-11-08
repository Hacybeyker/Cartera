package com.hacybeyker.cartera.core.data.flags

import com.hacybeyker.cartera.core.domain.flags.FeatureFlag

/**
 * Punto de extensión: de dónde sale el valor de cada interruptor en tiempo de ejecución. Release no tiene ninguna
 * fuente; debug y demo aportan la del escenario. `null` significa "esta fuente no lo decide".
 */
fun interface FeatureFlagSource {
    fun valueOf(flag: FeatureFlag): Boolean?
}
