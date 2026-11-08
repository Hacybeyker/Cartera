package com.hacybeyker.cartera.demo.di

import android.content.Context
import com.hacybeyker.cartera.BuildConfig
import com.hacybeyker.cartera.core.data.flags.FeatureFlagSource
import com.hacybeyker.cartera.core.data.network.ExtraInterceptors
import com.hacybeyker.cartera.demo.scenario.DemoResetClient
import com.hacybeyker.cartera.demo.scenario.DemoScenarioInterceptor
import com.hacybeyker.cartera.demo.scenario.ScenarioFeatureFlagSource
import com.hacybeyker.cartera.demo.scenario.ScenarioStore
import com.hacybeyker.cartera.demo.scenario.ui.ScenarioNavKey
import com.hacybeyker.cartera.demo.scenario.ui.ScenarioScreen
import com.hacybeyker.cartera.demo.scenarioStore
import com.hacybeyker.cartera.navigation.HiddenScreen
import com.hacybeyker.cartera.navigation.NavEntryInstaller
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient

@Module
@InstallIn(SingletonComponent::class)
object DemoToolsModule {

    @Provides
    @Singleton
    fun provideScenarioStore(@ApplicationContext context: Context): ScenarioStore = context.scenarioStore()

    @Provides
    @IntoSet
    @ExtraInterceptors
    fun provideScenarioInterceptor(store: ScenarioStore): Interceptor = DemoScenarioInterceptor(store)

    @Provides
    @IntoSet
    fun provideScenarioFeatureFlags(store: ScenarioStore): FeatureFlagSource = ScenarioFeatureFlagSource(store)

    // Sin los interceptores de la app: restablecer datos no lleva cabeceras de escenario ni token.
    @Provides
    fun provideDemoResetClient(): DemoResetClient = DemoResetClient(OkHttpClient(), BuildConfig.BASE_URL.toHttpUrl())

    @Provides
    @IntoSet
    fun provideScenarioEntry(): NavEntryInstaller = NavEntryInstaller { onBack ->
        entry<ScenarioNavKey> { ScenarioScreen(onBack = onBack) }
    }

    @Provides
    @IntoSet
    fun provideScenarioHiddenScreen(): HiddenScreen = HiddenScreen { ScenarioNavKey }
}
