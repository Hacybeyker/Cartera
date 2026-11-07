package com.hacybeyker.cartera.demo.di

import android.content.Context
import com.hacybeyker.cartera.core.data.network.ExtraInterceptors
import com.hacybeyker.cartera.demo.scenario.DemoScenarioInterceptor
import com.hacybeyker.cartera.demo.scenario.ScenarioStore
import com.hacybeyker.cartera.demo.scenarioStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton
import okhttp3.Interceptor

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
}
