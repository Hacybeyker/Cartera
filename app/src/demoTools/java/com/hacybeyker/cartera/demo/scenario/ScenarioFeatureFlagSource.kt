package com.hacybeyker.cartera.demo.scenario

import com.hacybeyker.cartera.core.data.flags.FeatureFlagSource
import com.hacybeyker.cartera.core.domain.flags.FeatureFlag

/** Lee cada interruptor del escenario guardado en el momento de usarlo: adb o Escenario rigen sin reiniciar. */
class ScenarioFeatureFlagSource(private val store: ScenarioStore) : FeatureFlagSource {
    override fun valueOf(flag: FeatureFlag): Boolean = flag in store.load().flags
}
