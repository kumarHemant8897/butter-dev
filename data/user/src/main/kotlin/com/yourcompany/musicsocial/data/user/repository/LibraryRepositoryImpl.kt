package com.yourcompany.musicsocial.data.user.repository

import com.yourcompany.musicsocial.core.domain.model.LibraryKind
import com.yourcompany.musicsocial.core.domain.model.Song
import com.yourcompany.musicsocial.core.domain.repository.LibraryRepository
import com.yourcompany.musicsocial.core.domain.result.AppError
import com.yourcompany.musicsocial.core.domain.result.AppResult
import com.yourcompany.musicsocial.core.domain.result.map
import com.yourcompany.musicsocial.data.user.datasource.LibraryRemoteDataSource
import com.yourcompany.musicsocial.data.user.datasource.LibrarySongDto
import com.yourcompany.musicsocial.data.user.mapper.StoredSongFields
import com.yourcompany.musicsocial.data.user.mapper.toSong
import com.yourcompany.musicsocial.data.user.mapper.toStoredFields
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Library rows store display metadata only. The preview address is never stored: when one of
 * these songs is played, the player looks it up from the catalogue (the provider's terms).
 * [mySongIds] is updated optimistically and put back if the backend refuses the change.
 */
@Singleton
internal class LibraryRepositoryImpl @Inject constructor(
    private val remote: LibraryRemoteDataSource,
) : LibraryRepository {

    private val _mySongIds = MutableStateFlow<Map<LibraryKind, Set<String>>>(emptyMap())
    override val mySongIds: StateFlow<Map<LibraryKind, Set<String>>> = _mySongIds.asStateFlow()

    override suspend fun refreshMine(): AppResult<Unit> =
        remote.getMine().map { rows ->
            _mySongIds.value = rows.mapNotNull { row -> kindOf(row.kind)?.let { it to row.songId } }
                .groupBy({ it.first }, { it.second })
                .mapValues { (_, ids) -> ids.toSet() }
        }

    override suspend fun getSongs(userId: String, kind: LibraryKind, limit: Int): AppResult<List<Song>> =
        remote.getSongs(userId, kind.code, limit).map { rows -> rows.map { it.toSong() } }

    override suspend fun add(kind: LibraryKind, song: Song): AppResult<Unit> {
        setMember(kind, song.id, member = true)
        val result = remote.insert(song.toRow(kind))
        return when {
            result is AppResult.Success -> result
            // Already in the list (another device, or a double tap): the end state is what was asked for.
            (result as AppResult.Failure).error == AppError.Conflict -> AppResult.Success(Unit)
            else -> {
                setMember(kind, song.id, member = false)
                result
            }
        }
    }

    override suspend fun remove(kind: LibraryKind, songId: String): AppResult<Unit> {
        setMember(kind, songId, member = false)
        val result = remote.delete(kind.code, songId)
        if (result is AppResult.Failure) setMember(kind, songId, member = true)
        return result
    }

    private fun setMember(kind: LibraryKind, songId: String, member: Boolean) {
        _mySongIds.update { current ->
            val ids = current[kind].orEmpty()
            current + (kind to if (member) ids + songId else ids - songId)
        }
    }

    internal companion object {
        val LibraryKind.code: String get() = if (this == LibraryKind.LIKED) "liked" else "saved"

        fun kindOf(code: String): LibraryKind? = when (code) {
            "liked" -> LibraryKind.LIKED
            "saved" -> LibraryKind.SAVED
            else -> null
        }

        /** [LibrarySongDto.userId] is filled in by the data source with the signed-in user. */
        fun Song.toRow(kind: LibraryKind): LibrarySongDto {
            val fields = toStoredFields()
            return LibrarySongDto(
                userId = "",
                kind = kind.code,
                songId = fields.songId,
                title = fields.title,
                artistNames = fields.artistNames,
                albumTitle = fields.albumTitle,
                artworkUrl = fields.artworkUrl,
                externalUrl = fields.externalUrl,
                durationMs = fields.durationMs,
            )
        }

        fun LibrarySongDto.toSong(): Song =
            StoredSongFields(songId, title, artistNames, albumTitle, artworkUrl, externalUrl, durationMs).toSong()
    }
}
