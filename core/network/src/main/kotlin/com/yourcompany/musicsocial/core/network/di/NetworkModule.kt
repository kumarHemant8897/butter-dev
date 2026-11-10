package com.yourcompany.musicsocial.core.network.di

import com.yourcompany.musicsocial.core.domain.system.BackendAvailability
import com.yourcompany.musicsocial.core.network.SupabaseClientProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object NetworkModule {

    /**
     * One OkHttp client for the whole app (Supabase, Coil, future REST APIs), so connection
     * pools and thread pools are shared. Feature-specific clients should be derived with newBuilder().
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class BackendAvailabilityModule {

    @Binds
    abstract fun bindBackendAvailability(impl: SupabaseClientProvider): BackendAvailability
}
