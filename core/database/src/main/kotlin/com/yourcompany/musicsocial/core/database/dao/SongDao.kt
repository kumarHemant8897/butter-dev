package com.yourcompany.musicsocial.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.yourcompany.musicsocial.core.database.entity.CachedSongEntity

@Dao
interface SongDao {

    @Query("SELECT * FROM cached_songs WHERE id = :id")
    suspend fun getById(id: String): CachedSongEntity?

    @Upsert
    suspend fun upsert(songs: List<CachedSongEntity>)

    /** Bounded cache: removes entries older than [olderThanEpochMs]. */
    @Query("DELETE FROM cached_songs WHERE cached_at < :olderThanEpochMs")
    suspend fun deleteOlderThan(olderThanEpochMs: Long): Int
}
