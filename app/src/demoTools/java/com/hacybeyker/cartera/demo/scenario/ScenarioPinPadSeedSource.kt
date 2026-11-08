package com.hacybeyker.cartera.demo.scenario

import com.hacybeyker.cartera.core.data.pinpad.PinPadSeedSource

/** Lee la semilla del escenario guardado en el momento de usarla: adb o Escenario rigen sin reiniciar. */
class ScenarioPinPadSeedSource(private val store: ScenarioStore) : PinPadSeedSource {
    override fun seed(): Int = store.load().pinPadSeed
}
