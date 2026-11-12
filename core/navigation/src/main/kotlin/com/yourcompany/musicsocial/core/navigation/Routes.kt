package com.yourcompany.musicsocial.core.navigation

import kotlinx.serialization.Serializable

/*
 * Navigation Compose type-safe routes.
 *
 * Each bottom-bar section is a nested graph (`*Graph`) with a start destination (`*Route`).
 * Nesting from day one means later screens (song detail, conversation, edit profile) are added
 * inside their section without touching the app shell, and each tab keeps its own back stack.
 *
 * Routes carry IDs only, never whole objects: the destination loads data through its ViewModel,
 * which keeps the back stack small and survives process death.
 */

@Serializable data object HomeGraph
@Serializable data object HomeRoute

@Serializable data object DiscoverGraph
@Serializable data object DiscoverRoute

@Serializable data object PlayerGraph
@Serializable data object PlayerRoute

@Serializable data object ChatsGraph
@Serializable data object ChatListRoute

@Serializable data object ProfileGraph

/** Profile screen. A null [userId] means the signed-in user's own profile. */
@Serializable data class ProfileRoute(val userId: String? = null)

/** Edit the signed-in user's own profile. */
@Serializable data object EditProfileRoute

/**
 * A full list of songs: a chart ("See all"), a genre chart or an artist's songs. Top level (not in a
 * tab's graph) because Home and Discover both open it; [title] shows while the songs load.
 * [kind] is one of [SongListKind]; [id] is the genre or artist id (null for the main chart).
 */
@Serializable data class SongListRoute(val kind: String, val title: String, val id: String? = null)

object SongListKind {
    const val CHART = "chart"
    const val GENRE = "genre"
    const val ARTIST = "artist"

    /** A user playlist; [SongListRoute.id] is the playlist id. */
    const val PLAYLIST = "playlist"
}

/** Someone else's profile. Top level: opened from Discover, people search, chats and requests. */
@Serializable data class UserProfileRoute(val userId: String)

/** Find people to follow ("Find your people"). */
@Serializable data object PeopleSearchRoute

/** Follow requests waiting for the signed-in user. */
@Serializable data object FollowRequestsRoute

/**
 * One conversation. [title] shows while messages load; [otherUserId] (direct chats) lets the header
 * open that person's profile.
 */
@Serializable data class ConversationRoute(val conversationId: String, val title: String, val otherUserId: String? = null)
