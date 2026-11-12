package com.yourcompany.musicsocial.core.playback.di

import com.yourcompany.musicsocial.core.domain.playback.PlaybackController
import com.yourcompany.musicsocial.core.playback.ExoPlaybackController
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class PlaybackModule {

    @Binds
    @Singleton
    abstract fun bindPlaybackController(impl: ExoPlaybackController): PlaybackController
}
