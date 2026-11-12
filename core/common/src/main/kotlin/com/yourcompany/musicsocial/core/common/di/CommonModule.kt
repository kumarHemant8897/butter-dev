package com.yourcompany.musicsocial.core.common.di

import com.yourcompany.musicsocial.core.common.coroutines.ApplicationScope
import com.yourcompany.musicsocial.core.common.coroutines.DefaultDispatcher
import com.yourcompany.musicsocial.core.common.coroutines.IoDispatcher
import com.yourcompany.musicsocial.core.common.coroutines.MainDispatcher
import com.yourcompany.musicsocial.core.common.logging.AppLogger
import com.yourcompany.musicsocial.core.common.logging.CrashReporter
import com.yourcompany.musicsocial.core.common.logging.NoOpCrashReporter
import com.yourcompany.musicsocial.core.common.logging.TimberAppLogger
import com.yourcompany.musicsocial.core.common.network.ConnectivityNetworkMonitor
import com.yourcompany.musicsocial.core.domain.system.NetworkMonitor
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class CommonBindingsModule {

    @Binds
    @Singleton
    abstract fun bindAppLogger(impl: TimberAppLogger): AppLogger

    @Binds
    @Singleton
    abstract fun bindCrashReporter(impl: NoOpCrashReporter): CrashReporter

    @Binds
    @Singleton
    abstract fun bindNetworkMonitor(impl: ConnectivityNetworkMonitor): NetworkMonitor
}

@Module
@InstallIn(SingletonComponent::class)
internal object TimeModule {

    /** Inject [Clock] instead of calling Instant.now() so time-dependent logic is testable. */
    @Provides
    fun provideClock(): Clock = Clock.systemDefaultZone()
}

@Module
@InstallIn(SingletonComponent::class)
internal object CoroutinesModule {

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @MainDispatcher
    fun provideMainDispatcher(): CoroutineDispatcher = Dispatchers.Main.immediate

    @Provides
    @DefaultDispatcher
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(@DefaultDispatcher dispatcher: CoroutineDispatcher): CoroutineScope =
        CoroutineScope(SupervisorJob() + dispatcher)
}
