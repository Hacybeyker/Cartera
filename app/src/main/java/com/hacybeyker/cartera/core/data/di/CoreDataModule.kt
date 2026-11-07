package com.hacybeyker.cartera.core.data.di

import android.content.Context
import android.content.SharedPreferences
import com.hacybeyker.cartera.core.data.config.RemoteAppConfigRepository
import com.hacybeyker.cartera.core.data.firstrun.SharedPreferencesFirstRunRepository
import com.hacybeyker.cartera.core.data.network.SessionExpiryListener
import com.hacybeyker.cartera.core.data.session.InMemorySessionRepository
import com.hacybeyker.cartera.core.domain.config.AppConfigRepository
import com.hacybeyker.cartera.core.domain.firstrun.FirstRunRepository
import com.hacybeyker.cartera.core.domain.session.SessionRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class CoreDataModule {

    @Binds
    abstract fun bindSessionRepository(repository: InMemorySessionRepository): SessionRepository

    @Binds
    abstract fun bindSessionExpiryListener(repository: InMemorySessionRepository): SessionExpiryListener

    @Binds
    abstract fun bindAppConfigRepository(repository: RemoteAppConfigRepository): AppConfigRepository

    @Binds
    abstract fun bindFirstRunRepository(repository: SharedPreferencesFirstRunRepository): FirstRunRepository

    companion object {
        @Provides
        fun provideFirstRunPreferences(@ApplicationContext context: Context): SharedPreferences =
            context.getSharedPreferences(SharedPreferencesFirstRunRepository.FILE_NAME, Context.MODE_PRIVATE)
    }
}
