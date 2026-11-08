package com.hacybeyker.cartera.core.data.market.di

import com.hacybeyker.cartera.BuildConfig
import com.hacybeyker.cartera.core.common.IoDispatcher
import com.hacybeyker.cartera.core.data.market.MarketStreamScope
import com.hacybeyker.cartera.core.data.market.MarketStreamUrl
import com.hacybeyker.cartera.core.data.market.ReconnectWait
import com.hacybeyker.cartera.core.data.market.RemoteMarketRepository
import com.hacybeyker.cartera.core.data.market.WebSocketMarketStream
import com.hacybeyker.cartera.core.data.portfolio.InMemoryPortfolioChanges
import com.hacybeyker.cartera.core.data.portfolio.RemotePortfolioRepository
import com.hacybeyker.cartera.core.domain.market.MarketRepository
import com.hacybeyker.cartera.core.domain.market.MarketStream
import com.hacybeyker.cartera.core.domain.portfolio.PortfolioChanges
import com.hacybeyker.cartera.core.domain.portfolio.PortfolioRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.WebSocket

@Module
@InstallIn(SingletonComponent::class)
abstract class MarketDataModule {

    @Binds
    abstract fun bindMarketRepository(repository: RemoteMarketRepository): MarketRepository

    @Binds
    abstract fun bindMarketStream(stream: WebSocketMarketStream): MarketStream

    @Binds
    abstract fun bindPortfolioRepository(repository: RemotePortfolioRepository): PortfolioRepository

    @Binds
    abstract fun bindPortfolioChanges(changes: InMemoryPortfolioChanges): PortfolioChanges

    companion object {
        // El mismo cliente que la API: BearerInterceptor y los interceptores extra también aplican al handshake.
        @Provides
        fun provideWebSocketFactory(client: OkHttpClient): WebSocket.Factory = client

        @Provides
        @MarketStreamUrl
        fun provideMarketStreamUrl(): HttpUrl = BuildConfig.BASE_URL.toHttpUrl().newBuilder()
            .addPathSegments("market/stream")
            .build()

        @Provides
        @Singleton
        @MarketStreamScope
        fun provideMarketStreamScope(@IoDispatcher dispatcher: CoroutineDispatcher): CoroutineScope =
            CoroutineScope(SupervisorJob() + dispatcher.limitedParallelism(1))

        @Provides
        fun provideReconnectWait(): ReconnectWait = ReconnectWait { delay(it) }
    }
}
