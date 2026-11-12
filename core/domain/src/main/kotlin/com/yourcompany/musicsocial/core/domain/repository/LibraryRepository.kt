package com.yourcompany.musicsocial.core.domain.repository

import com.yourcompany.musicsocial.core.domain.model.LibraryKind
import com.yourcompany.musicsocial.core.domain.model.Song
import com.yourcompany.musicsocial.core.domain.result.AppResult
import kotlinx.coroutines.flow.StateFlow

/**
 * Liked and saved songs. The signed-in user's own song ids are kept in [mySongIds] so every screen
 * (Player, profile) shows the same heart state; other people's lists are read on demand and are
 * only returned to approved followers.
 */
interface LibraryRepository {
    /** Song ids per list for the signed-in user. Empty until [refreshMine] succeeds. */
    val mySongIds: StateFlow<Map<LibraryKind, Set<String>>>

    suspend fun refreshMine(): AppResult<Unit>

    /**
     * [userId]'s songs in one list, most recently added first. For someone the signed-in user does
     * not follow the database returns nothing, so check Relationship.canSeeLibrary before asking.
     */
    suspend fun getSongs(userId: String, kind: LibraryKind, limit: Int): AppResult<List<Song>>

    /** Idempotent: adding a song already in the list succeeds. */
    suspend fun add(kind: LibraryKind, song: Song): AppResult<Unit>

    suspend fun remove(kind: LibraryKind, songId: String): AppResult<Unit>
}
