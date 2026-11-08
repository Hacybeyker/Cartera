package com.hacybeyker.cartera.core.domain.flags

fun interface FeatureFlags {
    fun isEnabled(flag: FeatureFlag): Boolean
}
