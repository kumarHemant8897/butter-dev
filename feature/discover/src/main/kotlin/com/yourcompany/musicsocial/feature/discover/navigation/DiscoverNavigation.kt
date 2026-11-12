package com.yourcompany.musicsocial.feature.discover.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.yourcompany.musicsocial.core.navigation.DiscoverGraph
import com.yourcompany.musicsocial.core.navigation.DiscoverRoute
import com.yourcompany.musicsocial.core.navigation.SongListRoute
import com.yourcompany.musicsocial.feature.discover.presentation.screen.DiscoverRoute as DiscoverRouteScreen

/**
 * The app shell owns the shared destinations: [onOpenSongList] (charts, artist pages),
 * [onOpenProfile] (someone's profile) and [onFindPeople] (people search).
 */
fun NavGraphBuilder.discoverSection(
    contentPadding: PaddingValues,
    onOpenSongList: (SongListRoute) -> Unit,
    onOpenProfile: (String) -> Unit,
    onFindPeople: () -> Unit,
) {
    navigation<DiscoverGraph>(startDestination = DiscoverRoute) {
        composable<DiscoverRoute> {
            DiscoverRouteScreen(
                contentPadding = contentPadding,
                onOpenSongList = onOpenSongList,
                onOpenProfile = onOpenProfile,
                onFindPeople = onFindPeople,
            )
        }
    }
}
