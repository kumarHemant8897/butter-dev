package com.yourcompany.musicsocial.feature.home.presentation.state

import androidx.compose.runtime.Immutable
import com.yourcompany.musicsocial.core.domain.model.Playlist
import com.yourcompany.musicsocial.core.domain.model.Song
import com.yourcompany.musicsocial.core.sample.FriendListening
import com.yourcompany.musicsocial.core.sample.SampleAlbum
import com.yourcompany.musicsocial.core.sample.TrendingSong

enum class TimeOfDay { MORNING, AFTERNOON, EVENING, NIGHT }

@Immutable
data class HomeUiState(
    val firstName: String,
    val timeOfDay: TimeOfDay,
    val isBackendConfigured: Boolean,
    val isOffline: Boolean,
    val content: HomeContent,
)

/** What the Home feed shows. Filled from SampleData until the feed repository exists. */
@Immutable
data class HomeContent(
    val recentlyPlayed: List<Song>,
    val recommendedAlbums: List<SampleAlbum>,
    val pickedForYou: Song?,
    val trending: TrendingSong?,
    val friendsListening: List<FriendListening>,
    val sharedWithYou: SharedWithYou?,
    val madeForYou: Playlist?,
    /** True while content comes from SampleData, so the screen can say so. */
    val isSample: Boolean,
)

@Immutable
data class SharedWithYou(val senderName: String, val song: Song, val quote: String, val minutesAgo: Int)
