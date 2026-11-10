package com.yourcompany.musicsocial.core.domain.repository

import com.yourcompany.musicsocial.core.domain.model.Page
import com.yourcompany.musicsocial.core.domain.model.PageRequest
import com.yourcompany.musicsocial.core.domain.model.Song
import com.yourcompany.musicsocial.core.domain.result.AppResult

interface MusicRepository {
    /** Returns the song, preferring the local cache and falling back to the backend. */
    suspend fun getSong(songId: String): AppResult<Song>

    suspend fun searchSongs(query: String, page: PageRequest): AppResult<Page<Song>>
}
