package com.yourcompany.musicsocial.data.auth.di

import com.yourcompany.musicsocial.core.domain.repository.AuthRepository
import com.yourcompany.musicsocial.data.auth.datasource.AuthRemoteDataSource
import com.yourcompany.musicsocial.data.auth.datasource.SupabaseAuthRemoteDataSource
import com.yourcompany.musicsocial.data.auth.repository.AuthRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class AuthDataModule {

    @Binds
    abstract fun bindAuthRemoteDataSource(impl: SupabaseAuthRemoteDataSource): AuthRemoteDataSource

    @Binds
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository
}
