package com.hacybeyker.cartera.core.data.flags

import com.hacybeyker.cartera.BuildConfig
import com.hacybeyker.cartera.core.domain.flags.FeatureFlag
import com.hacybeyker.cartera.core.domain.flags.FeatureFlags
import javax.inject.Inject

/**
 * La primera fuente que decide gana; sin fuentes, rige BuildConfig, que es false en todas las variantes. Como
 * release no tiene fuentes, ahí ningún interruptor se puede encender.
 */
class BuildConfigFeatureFlags @Inject constructor(private val sources: Set<@JvmSuppressWildcards FeatureFlagSource>) :
    FeatureFlags {

    override fun isEnabled(flag: FeatureFlag): Boolean =
        sources.firstNotNullOfOrNull { it.valueOf(flag) } ?: buildConfigValue(flag)

    private fun buildConfigValue(flag: FeatureFlag): Boolean = when (flag) {
        FeatureFlag.MOVEMENTS_FULL_PAGE -> BuildConfig.MOVEMENTS_FULL_PAGE
        FeatureFlag.ACCOUNTS_EAGER_REFRESH -> BuildConfig.ACCOUNTS_EAGER_REFRESH
        FeatureFlag.BREAKDOWN_RICH_TRANSITION -> BuildConfig.BREAKDOWN_RICH_TRANSITION
        FeatureFlag.QUOTES_FULL_REFRESH -> BuildConfig.QUOTES_FULL_REFRESH
        FeatureFlag.CHART_LIVE_PATH -> BuildConfig.CHART_LIVE_PATH
    }
}
