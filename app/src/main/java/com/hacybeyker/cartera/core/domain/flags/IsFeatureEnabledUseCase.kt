package com.hacybeyker.cartera.core.domain.flags

import javax.inject.Inject

class IsFeatureEnabledUseCase @Inject constructor(private val flags: FeatureFlags) {
    operator fun invoke(flag: FeatureFlag): Boolean = flags.isEnabled(flag)
}
