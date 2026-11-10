package com.yourcompany.musicsocial.core.database.di

import android.content.Context
import androidx.room.Room
import com.yourcompany.musicsocial.core.database.MusicSocialDatabase
import com.yourcompany.musicsocial.core.database.dao.SongDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MusicSocialDatabase =
        Room.databaseBuilder(context, MusicSocialDatabase::class.java, MusicSocialDatabase.NAME)
            // Every table is currently a cache that can be re-fetched. Replace with real migrations
            // as soon as a table holds data that exists only on the device (e.g. a message outbox).
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun provideSongDao(database: MusicSocialDatabase): SongDao = database.songDao()
}
