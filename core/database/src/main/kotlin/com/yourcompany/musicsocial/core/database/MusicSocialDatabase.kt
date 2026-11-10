package com.yourcompany.musicsocial.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.yourcompany.musicsocial.core.database.dao.SongDao
import com.yourcompany.musicsocial.core.database.entity.CachedSongEntity

/**
 * Schema changes must bump [version] and add a Migration (or an AutoMigration). The exported
 * schema JSON in core/database/schemas is committed so migrations can be reviewed and tested.
 * Destructive migration is acceptable only while every table is a pure cache.
 */
@Database(
    entities = [CachedSongEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class MusicSocialDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao

    companion object {
        const val NAME = "musicsocial.db"
    }
}
