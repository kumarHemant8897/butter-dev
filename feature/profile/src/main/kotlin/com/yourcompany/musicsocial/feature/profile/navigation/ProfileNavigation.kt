package com.yourcompany.musicsocial.feature.profile.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.yourcompany.musicsocial.core.domain.model.Playlist
import com.yourcompany.musicsocial.core.navigation.ConversationRoute
import com.yourcompany.musicsocial.core.navigation.SongListKind
import com.yourcompany.musicsocial.core.navigation.SongListRoute
import com.yourcompany.musicsocial.core.navigation.EditProfileRoute
import com.yourcompany.musicsocial.core.navigation.FollowRequestsRoute
import com.yourcompany.musicsocial.core.navigation.PeopleSearchRoute
import com.yourcompany.musicsocial.core.navigation.ProfileGraph
import com.yourcompany.musicsocial.core.navigation.ProfileRoute
import com.yourcompany.musicsocial.core.navigation.UserProfileRoute
import com.yourcompany.musicsocial.feature.profile.presentation.screen.EditProfileRoute as EditProfileRouteScreen
import com.yourcompany.musicsocial.feature.profile.presentation.screen.FollowRequestsRoute as FollowRequestsRouteScreen
import com.yourcompany.musicsocial.feature.profile.presentation.screen.PeopleSearchRoute as PeopleSearchRouteScreen
import com.yourcompany.musicsocial.feature.profile.presentation.screen.ProfileRoute as ProfileRouteScreen
import com.yourcompany.musicsocial.feature.profile.presentation.screen.UserProfileRoute as UserProfileRouteScreen

/** Playlists open on the shared song-list screen (registered by the app shell). */
private fun Playlist.toRoute() = SongListRoute(kind = SongListKind.PLAYLIST, title = title, id = id)

/**
 * The tab opens the signed-in user's own profile (`ProfileRoute(userId = null)`); Edit profile is a
 * second screen inside the same graph, so the Profile tab stays selected while editing.
 */
fun NavGraphBuilder.profileSection(navController: NavController, contentPadding: PaddingValues) {
    navigation<ProfileGraph>(startDestination = ProfileRoute()) {
        composable<ProfileRoute> {
            ProfileRouteScreen(
                contentPadding = contentPadding,
                onEditProfile = { navController.navigate(EditProfileRoute) },
                onOpenRequests = { navController.navigate(FollowRequestsRoute) },
                onFindPeople = { navController.navigate(PeopleSearchRoute) },
                onOpenPlaylist = { navController.navigate(it.toRoute()) },
            )
        }
        composable<EditProfileRoute> {
            EditProfileRouteScreen(
                contentPadding = contentPadding,
                onBack = { navController.popBackStack() },
            )
        }
    }
}

/**
 * People screens opened from several tabs (Chats, Discover, Profile), so they sit outside the tab
 * graphs. [onOpenConversation] is owned by the app shell because the chat screen lives in another feature.
 */
fun NavGraphBuilder.peopleDestinations(
    navController: NavController,
    contentPadding: PaddingValues,
    onOpenConversation: (ConversationRoute) -> Unit,
) {
    composable<UserProfileRoute> {
        UserProfileRouteScreen(
            contentPadding = contentPadding,
            onBack = { navController.popBackStack() },
            onOpenConversation = onOpenConversation,
            onOpenPlaylist = { navController.navigate(it.toRoute()) },
        )
    }
    composable<PeopleSearchRoute> {
        PeopleSearchRouteScreen(
            contentPadding = contentPadding,
            onBack = { navController.popBackStack() },
            onOpenProfile = { userId -> navController.navigate(UserProfileRoute(userId)) },
        )
    }
    composable<FollowRequestsRoute> {
        FollowRequestsRouteScreen(
            contentPadding = contentPadding,
            onBack = { navController.popBackStack() },
            onOpenProfile = { userId -> navController.navigate(UserProfileRoute(userId)) },
            onFindPeople = { navController.navigate(PeopleSearchRoute) },
        )
    }
}
