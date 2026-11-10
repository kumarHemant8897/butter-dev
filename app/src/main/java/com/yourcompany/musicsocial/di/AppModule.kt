package com.yourcompany.musicsocial.di

import com.yourcompany.musicsocial.BuildConfig
import com.yourcompany.musicsocial.core.network.BackendConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Bindings that need values only the app module has (BuildConfig). Everything else is bound in
 * the module that owns the implementation.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideBackendConfig(): BackendConfig = BackendConfig(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseAnonKey = BuildConfig.SUPABASE_ANON_KEY,
        enableHttpLogging = BuildConfig.DEBUG,
    )
}
