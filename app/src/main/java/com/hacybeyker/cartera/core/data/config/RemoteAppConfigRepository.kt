package com.hacybeyker.cartera.core.data.config

import com.hacybeyker.cartera.core.data.network.ApiCaller
import com.hacybeyker.cartera.core.data.network.CarteraApi
import com.hacybeyker.cartera.core.data.network.dto.AppConfigDto
import com.hacybeyker.cartera.core.data.network.toOutcome
import com.hacybeyker.cartera.core.domain.Outcome
import com.hacybeyker.cartera.core.domain.config.AppConfig
import com.hacybeyker.cartera.core.domain.config.AppConfigRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteAppConfigRepository @Inject constructor(private val api: CarteraApi, private val call: ApiCaller) :
    AppConfigRepository {

    @Volatile
    private var cached: AppConfig? = null

    override suspend fun refresh(): Outcome<AppConfig> = call { api.appConfig() }
        .toOutcome { it.toDomain() }
        .also { outcome -> if (outcome is Outcome.Success) cached = outcome.value }

    override suspend fun config(): Outcome<AppConfig> = cached?.let { Outcome.Success(it) } ?: refresh()
}

private fun AppConfigDto.toDomain() = AppConfig(minSupportedVersion = minSupportedVersion, termsVersion = termsVersion)
