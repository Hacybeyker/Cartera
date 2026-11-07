package com.hacybeyker.cartera.core.data.network.di

import com.hacybeyker.cartera.BuildConfig
import com.hacybeyker.cartera.core.data.network.AccessTokenProvider
import com.hacybeyker.cartera.core.data.network.BearerInterceptor
import com.hacybeyker.cartera.core.data.network.CarteraApi
import com.hacybeyker.cartera.core.data.network.CarteraHttpClient
import com.hacybeyker.cartera.core.data.network.ExtraInterceptors
import com.hacybeyker.cartera.core.data.session.InMemoryAccessTokenStore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = CarteraHttpClient.json

    @Provides
    @Singleton
    fun provideOkHttpClient(
        bearer: BearerInterceptor,
        @ExtraInterceptors extras: Set<@JvmSuppressWildcards Interceptor>
    ): OkHttpClient = CarteraHttpClient.okHttpClient(BuildConfig.HTTP_TIMEOUT_SECONDS, listOf(bearer) + extras)

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, json: Json): Retrofit =
        CarteraHttpClient.retrofit(BuildConfig.BASE_URL, client, json)

    @Provides
    @Singleton
    fun provideCarteraApi(retrofit: Retrofit): CarteraApi = retrofit.create(CarteraApi::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkBindingsModule {

    @Binds
    abstract fun bindAccessTokenProvider(store: InMemoryAccessTokenStore): AccessTokenProvider

    @Multibinds
    @ExtraInterceptors
    abstract fun extraInterceptors(): Set<Interceptor>
}
