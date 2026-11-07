package com.hacybeyker.cartera.feature.splash.domain.usecase

import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.core.domain.config.AppConfig
import com.hacybeyker.cartera.core.domain.config.AppConfigRepository
import javax.inject.Inject

class LoadAppConfigUseCase @Inject constructor(private val appConfig: AppConfigRepository) {
    suspend operator fun invoke(): Outcome<AppConfig> = appConfig.refresh()
}
