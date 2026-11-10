package com.yourcompany.musicsocial.data.music.di

import com.yourcompany.musicsocial.core.domain.repository.MusicRepository
import com.yourcompany.musicsocial.data.music.datasource.MusicRemoteDataSource
import com.yourcompany.musicsocial.data.music.datasource.SupabaseMusicRemoteDataSource
import com.yourcompany.musicsocial.data.music.repository.MusicRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class MusicDataModule {

    @Binds
    abstract fun bindMusicRemoteDataSource(impl: SupabaseMusicRemoteDataSource): MusicRemoteDataSource

    @Binds
    abstract fun bindMusicRepository(impl: MusicRepositoryImpl): MusicRepository
}
