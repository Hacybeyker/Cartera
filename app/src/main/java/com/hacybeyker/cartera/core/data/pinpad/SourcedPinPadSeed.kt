package com.hacybeyker.cartera.core.data.pinpad

import com.hacybeyker.cartera.core.domain.pinpad.PinPadSeed
import javax.inject.Inject

/** La primera fuente que decide gana; sin fuentes, 0: el orden fijo. */
class SourcedPinPadSeed @Inject constructor(private val sources: Set<@JvmSuppressWildcards PinPadSeedSource>) :
    PinPadSeed {

    override fun current(): Int = sources.firstNotNullOfOrNull { it.seed() } ?: FIXED_ORDER

    private companion object {
        const val FIXED_ORDER = 0
    }
}
