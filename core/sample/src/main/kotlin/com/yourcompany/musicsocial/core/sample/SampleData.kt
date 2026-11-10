package com.yourcompany.musicsocial.core.sample

import com.yourcompany.musicsocial.core.domain.model.Artist
import com.yourcompany.musicsocial.core.domain.model.Conversation
import com.yourcompany.musicsocial.core.domain.model.ConversationType
import com.yourcompany.musicsocial.core.domain.model.DeliveryStatus
import com.yourcompany.musicsocial.core.domain.model.Message
import com.yourcompany.musicsocial.core.domain.model.MessageContent
import com.yourcompany.musicsocial.core.domain.model.Playlist
import com.yourcompany.musicsocial.core.domain.model.Song
import com.yourcompany.musicsocial.core.domain.model.UserProfile
import com.yourcompany.musicsocial.core.domain.model.UserSummary
import java.time.Instant

/**
 * ============================  SAMPLE DATA, FOR TESTING ONLY  ============================
 * Hand-written content that mirrors the Figma screens so the UI can be tried end to end before
 * the backend exists. Nothing here is fetched, stored or sent anywhere.
 *
 * Every model uses the real domain types, so replacing this with repositories is a change inside
 * each ViewModel only. Images are null on purpose: the UI draws gradient artwork and initials
 * instead of shipping stock photos in the app.
 * =========================================================================================
 */
object SampleData {

    // ---- People -------------------------------------------------------------------------

    /** The signed-in user shown on the Profile tab. */
    val me = UserProfile(
        id = "user-hemant",
        username = "hemantkumar",
        displayName = "Hemant Kumar",
        bio = "building butter. songs that sound like 2am drives and late commits.",
        avatarUrl = null,
        followerCount = 1_240,
        followingCount = 348,
    )

    val eli = UserSummary("user-eli", "elichen", "Eli Chen", null)
    val june = UserSummary("user-june", "junepark", "June Park", null)
    val maya = UserSummary("user-maya", "mayainstereo", "Maya Solis", null)
    val noah = UserSummary("user-noah", "noahw", "Noah Williams", null)
    private val meSummary = UserSummary(me.id, me.username, me.displayName, null)

    // ---- Catalogue ------------------------------------------------------------------------

    private val theMidnight = Artist("artist-the-midnight", "The Midnight", null)
    private val arloParks = Artist("artist-arlo-parks", "Arlo Parks", null)
    private val phoebe = Artist("artist-phoebe", "Phoebe Bridgers", null)
    private val djo = Artist("artist-djo", "Djo", null)
    private val menITrust = Artist("artist-men-i-trust", "Men I Trust", null)

    val artists: List<Artist> = listOf(arloParks, menITrust, theMidnight, phoebe, djo)

    val nightDrive = song("song-night-drive", "Night Drive", theMidnight, 222_000)
    val softLanding = song("song-soft-landing", "Soft Landing", arloParks, 198_000)
    val moonSong = song("song-moon-song", "Moon Song", phoebe, 276_000)
    val endOfBeginning = song("song-end-of-beginning", "End of Beginning", djo, 159_000)
    val showMeHowYouDeal = song("song-show-me", "Show Me How", menITrust, 214_000)

    val midnightMix = Playlist("playlist-midnight-mix", me.id, "Your midnight mix", "Dream pop, late-night drives, you", null, false, 24)
    val cityIsAsleep = Playlist("playlist-city-asleep", "user-community", "the city is asleep", null, null, true, 42)
    val longWayHome = Playlist("playlist-long-way-home", me.id, "songs for the long way home", null, null, true, 28)

    // ---- Home -------------------------------------------------------------------------------

    val recentlyPlayed: List<Song> = listOf(nightDrive, softLanding, moonSong)

    val recommendedAlbums: List<SampleAlbum> = listOf(
        SampleAlbum("album-softer-loud", "A softer kind of loud", arloParks.name),
        SampleAlbum("album-blue-hour", "Blue hour", menITrust.name),
    )

    val trending: List<TrendingSong> = listOf(
        TrendingSong(endOfBeginning, listeners = 2_400),
        TrendingSong(nightDrive, listeners = 1_800),
    )

    val friendsListening: List<FriendListening> = listOf(
        FriendListening(eli, nightDrive),
        FriendListening(june, moonSong),
    )

    // ---- Discover ---------------------------------------------------------------------------

    val artistGenres: Map<String, String> = mapOf(
        menITrust.id to "Dream pop",
        arloParks.id to "Indie soul",
        djo.id to "Psych pop",
        theMidnight.id to "Synthwave",
        phoebe.id to "Indie folk",
    )

    val listenerMatches: List<ListenerMatch> = listOf(
        ListenerMatch(june, tasteMatchPercent = 94, sharedArtists = 12),
        ListenerMatch(eli, tasteMatchPercent = 91, sharedArtists = 8),
    )

    val feelings: List<String> = listOf("Dream pop", "Indie soul", "After dark", "Electronic", "Lo-fi")

    // ---- Player -----------------------------------------------------------------------------

    val nowPlaying = NowPlaying(song = nightDrive, fromPlaylist = midnightMix, positionMs = 84_000, sharePrompt = eli)

    // ---- Chats ------------------------------------------------------------------------------

    val onlineUserIds: Set<String> = setOf(eli.id, june.id)

    val conversations: List<Conversation> = listOf(
        direct("conv-eli", eli, MessageContent.SharedSong(nightDrive.id, null), eli.id, "2026-11-09T21:39:00Z", unread = 2),
        direct("conv-june", june, MessageContent.Text("ok but track 4 changed my brain"), june.id, "2026-11-09T21:32:00Z", unread = 1),
        group("conv-after-hours", "the after hours club", listOf(eli, june, maya), MessageContent.Text("who's still awake? \uD83C\uDF19"), eli.id, "2026-11-09T21:28:00Z"),
        direct("conv-maya", maya, MessageContent.Text("this whole album is a mood"), me.id, "2026-11-09T20:54:00Z"),
        group("conv-sunday", "Sunday selectors", listOf(june, noah), MessageContent.SharedPlaylist(longWayHome.id, "added 3 songs"), june.id, "2026-11-08T18:10:00Z"),
        direct("conv-noah", noah, MessageContent.Text("See you at the listening party"), noah.id, "2026-11-08T15:02:00Z"),
    )

    /** Display names for ids referenced by messages (senders and shared songs). */
    val songsById: Map<String, Song> = listOf(nightDrive, softLanding, moonSong, endOfBeginning, showMeHowYouDeal).associateBy { it.id }
    val peopleById: Map<String, UserSummary> = listOf(meSummary, eli, june, maya, noah).associateBy { it.id }

    // ---- Profile ----------------------------------------------------------------------------

    val myTopArtists: List<Artist> = listOf(arloParks, menITrust, theMidnight, phoebe)
    val myFavoriteSongs: List<Song> = listOf(nightDrive, moonSong)
    val myPublicMixes: List<Playlist> = listOf(longWayHome)
    val myLastPlayed = LastPlayed(softLanding, minutesAgo = 18)

    // ---- Helpers ----------------------------------------------------------------------------

    private fun song(id: String, title: String, artist: Artist, durationMs: Long) = Song(
        id = id, title = title, artists = listOf(artist), albumTitle = null, artworkUrl = null,
        durationMs = durationMs, previewUrl = null, externalUrl = null, catalogRef = null,
    )

    private fun direct(id: String, other: UserSummary, content: MessageContent, senderId: String, at: String, unread: Int = 0) =
        Conversation(
            id = id, type = ConversationType.DIRECT, title = null, avatarUrl = null,
            members = listOf(meSummary, other), lastMessage = message(id, senderId, content, at),
            unreadCount = unread, updatedAt = Instant.parse(at),
        )

    private fun group(id: String, title: String, others: List<UserSummary>, content: MessageContent, senderId: String, at: String) =
        Conversation(
            id = id, type = ConversationType.GROUP, title = title, avatarUrl = null,
            members = listOf(meSummary) + others, lastMessage = message(id, senderId, content, at),
            unreadCount = 0, updatedAt = Instant.parse(at),
        )

    private fun message(conversationId: String, senderId: String, content: MessageContent, at: String) = Message(
        id = "$conversationId-last", conversationId = conversationId, senderId = senderId, content = content,
        sentAt = Instant.parse(at), editedAt = null, status = DeliveryStatus.READ,
    )
}

/** Sample-only shapes for content the domain does not model yet (feeds, recommendations). */
data class SampleAlbum(val id: String, val title: String, val artistName: String)
data class TrendingSong(val song: Song, val listeners: Int)
data class FriendListening(val friend: UserSummary, val song: Song)
data class ListenerMatch(val person: UserSummary, val tasteMatchPercent: Int, val sharedArtists: Int)
data class NowPlaying(val song: Song, val fromPlaylist: Playlist, val positionMs: Long, val sharePrompt: UserSummary)
data class LastPlayed(val song: Song, val minutesAgo: Int)
