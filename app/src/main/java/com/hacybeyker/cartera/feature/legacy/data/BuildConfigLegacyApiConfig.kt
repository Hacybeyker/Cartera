package com.hacybeyker.cartera.feature.legacy.data

import com.hacybeyker.cartera.BuildConfig
import com.hacybeyker.cartera.legacy.integration.LegacyApiConfig
import javax.inject.Inject

class BuildConfigLegacyApiConfig @Inject constructor() : LegacyApiConfig {
    override val baseUrl: String = BuildConfig.BASE_URL
}
