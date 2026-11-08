package com.hacybeyker.cartera.legacy.core.di

import com.hacybeyker.cartera.legacy.account.data.AccountsApi
import com.hacybeyker.cartera.legacy.billers.data.BillersApi
import com.hacybeyker.cartera.legacy.core.data.LegacyApiCaller
import com.hacybeyker.cartera.legacy.core.data.LegacyJson
import com.hacybeyker.cartera.legacy.debts.data.DebtsApi
import com.hacybeyker.cartera.legacy.integration.LegacyApiConfig
import com.hacybeyker.cartera.legacy.integration.LegacyHttpClient
import com.hacybeyker.cartera.legacy.integration.LegacySessionExpiry
import com.hacybeyker.cartera.legacy.payment.data.PaymentsApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/** Retrofit de `:legacy` sobre el cliente HTTP de quien aloja el módulo, con RxJava 3 y su propio JSON. */
@Module
@InstallIn(SingletonComponent::class)
object LegacyNetworkModule {

    @Provides
    @Singleton
    @LegacyNetwork
    fun provideRetrofit(client: LegacyHttpClient, config: LegacyApiConfig): Retrofit = Retrofit.Builder()
        .baseUrl(config.baseUrl)
        .callFactory(client.callFactory())
        .addCallAdapterFactory(RxJava3CallAdapterFactory.createSynchronous())
        .addConverterFactory(LegacyJson.instance.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    @Singleton
    fun provideApiCaller(sessionExpiry: LegacySessionExpiry): LegacyApiCaller =
        LegacyApiCaller(LegacyJson.instance, sessionExpiry)

    @Provides
    @Singleton
    fun provideBillersApi(@LegacyNetwork retrofit: Retrofit): BillersApi = retrofit.create(BillersApi::class.java)

    @Provides
    @Singleton
    fun provideDebtsApi(@LegacyNetwork retrofit: Retrofit): DebtsApi = retrofit.create(DebtsApi::class.java)

    @Provides
    @Singleton
    fun provideAccountsApi(@LegacyNetwork retrofit: Retrofit): AccountsApi = retrofit.create(AccountsApi::class.java)

    @Provides
    @Singleton
    fun providePaymentsApi(@LegacyNetwork retrofit: Retrofit): PaymentsApi = retrofit.create(PaymentsApi::class.java)
}
