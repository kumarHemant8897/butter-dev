package com.yourcompany.musicsocial.data.user.di

import com.yourcompany.musicsocial.core.domain.repository.LibraryRepository
import com.yourcompany.musicsocial.core.domain.repository.PlaylistRepository
import com.yourcompany.musicsocial.core.domain.repository.UserRepository
import com.yourcompany.musicsocial.data.user.datasource.LibraryRemoteDataSource
import com.yourcompany.musicsocial.data.user.datasource.PlaylistRemoteDataSource
import com.yourcompany.musicsocial.data.user.datasource.SupabasePlaylistRemoteDataSource
import com.yourcompany.musicsocial.data.user.repository.PlaylistRepositoryImpl
import com.yourcompany.musicsocial.data.user.datasource.SupabaseLibraryRemoteDataSource
import com.yourcompany.musicsocial.data.user.datasource.SupabaseUserRemoteDataSource
import com.yourcompany.musicsocial.data.user.datasource.UserRemoteDataSource
import com.yourcompany.musicsocial.data.user.repository.LibraryRepositoryImpl
import com.yourcompany.musicsocial.data.user.repository.UserRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class UserDataModule {

    @Binds
    abstract fun bindUserRemoteDataSource(impl: SupabaseUserRemoteDataSource): UserRemoteDataSource

    @Binds
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    @Binds
    abstract fun bindLibraryRemoteDataSource(impl: SupabaseLibraryRemoteDataSource): LibraryRemoteDataSource

    @Binds
    abstract fun bindLibraryRepository(impl: LibraryRepositoryImpl): LibraryRepository

    @Binds
    abstract fun bindPlaylistRemoteDataSource(impl: SupabasePlaylistRemoteDataSource): PlaylistRemoteDataSource

    @Binds
    abstract fun bindPlaylistRepository(impl: PlaylistRepositoryImpl): PlaylistRepository
}
