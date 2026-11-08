package com.hacybeyker.cartera.feature.legacy.data

import com.hacybeyker.cartera.core.domain.pinpad.PinPadSeed
import com.hacybeyker.cartera.legacy.integration.LegacyPinPadSeed
import javax.inject.Inject

/** La semilla del teclado de `:legacy` es la misma de la app: en release rige el orden fijo. */
class PinPadSeedLegacySeed @Inject constructor(private val seed: PinPadSeed) : LegacyPinPadSeed {
    override fun current(): Int = seed.current()
}
