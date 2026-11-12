package com.yourcompany.musicsocial.data.music.di

import com.yourcompany.musicsocial.core.domain.repository.MusicRepository
import com.yourcompany.musicsocial.data.music.datasource.MusicRemoteDataSource
import com.yourcompany.musicsocial.data.music.datasource.ItunesMusicRemoteDataSource
import com.yourcompany.musicsocial.data.music.repository.MusicRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class MusicDataModule {

    @Binds
    abstract fun bindMusicRemoteDataSource(impl: ItunesMusicRemoteDataSource): MusicRemoteDataSource

    @Binds
    abstract fun bindMusicRepository(impl: MusicRepositoryImpl): MusicRepository
}
