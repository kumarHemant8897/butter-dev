package com.yourcompany.musicsocial.core.database

import androidx.room.TypeConverter
import com.yourcompany.musicsocial.core.database.entity.CachedArtist
import kotlinx.serialization.json.Json

internal class Converters {

    @TypeConverter
    fun artistsToJson(artists: List<CachedArtist>): String = json.encodeToString(artists)

    @TypeConverter
    fun jsonToArtists(value: String): List<CachedArtist> = json.decodeFromString(value)

    private companion object {
        val json = Json { ignoreUnknownKeys = true }
    }
}
