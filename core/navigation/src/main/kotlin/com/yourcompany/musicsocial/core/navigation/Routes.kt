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
