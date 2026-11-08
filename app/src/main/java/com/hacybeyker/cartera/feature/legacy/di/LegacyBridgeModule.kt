package com.hacybeyker.cartera.feature.legacy.di

import com.hacybeyker.cartera.feature.legacy.data.AccessTokenLegacySession
import com.hacybeyker.cartera.feature.legacy.data.BuildConfigLegacyApiConfig
import com.hacybeyker.cartera.feature.legacy.data.ListenerLegacySessionExpiry
import com.hacybeyker.cartera.feature.legacy.data.OkHttpLegacyHttpClient
import com.hacybeyker.cartera.feature.legacy.data.VisibleContentLegacyMarker
import com.hacybeyker.cartera.legacy.integration.LegacyApiConfig
import com.hacybeyker.cartera.legacy.integration.LegacyContentMarker
import com.hacybeyker.cartera.legacy.integration.LegacyHttpClient
import com.hacybeyker.cartera.legacy.integration.LegacySession
import com.hacybeyker.cartera.legacy.integration.LegacySessionExpiry
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Conecta las interfaces de `:legacy` con la sesión, la URL base, el cliente HTTP y las marcas de la app. */
@Module
@InstallIn(SingletonComponent::class)
abstract class LegacyBridgeModule {

    @Binds
    abstract fun bindLegacySession(session: AccessTokenLegacySession): LegacySession

    @Binds
    abstract fun bindLegacyApiConfig(config: BuildConfigLegacyApiConfig): LegacyApiConfig

    @Binds
    abstract fun bindLegacySessionExpiry(expiry: ListenerLegacySessionExpiry): LegacySessionExpiry

    @Binds
    abstract fun bindLegacyHttpClient(client: OkHttpLegacyHttpClient): LegacyHttpClient

    @Binds
    abstract fun bindLegacyContentMarker(marker: VisibleContentLegacyMarker): LegacyContentMarker
}
