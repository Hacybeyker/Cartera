package com.hacybeyker.cartera.feature.legacy.data

import com.hacybeyker.cartera.core.domain.flags.FeatureFlag
import com.hacybeyker.cartera.core.domain.flags.FeatureFlags
import com.hacybeyker.cartera.legacy.integration.LegacyFeatureFlags
import javax.inject.Inject

/** Los interruptores de `:legacy` son los del cliente: se consultan en cada uso, así el escenario rige al instante. */
class ClientFlagsLegacyFeatureFlags @Inject constructor(private val flags: FeatureFlags) : LegacyFeatureFlags {
    override fun billersFullRebind(): Boolean = flags.isEnabled(FeatureFlag.BILLERS_FULL_REBIND)

    override fun debtsMainDecode(): Boolean = flags.isEnabled(FeatureFlag.DEBTS_MAIN_DECODE)
}
