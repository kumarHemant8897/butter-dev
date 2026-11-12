package com.yourcompany.musicsocial.feature.home.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.yourcompany.musicsocial.core.navigation.HomeGraph
import com.yourcompany.musicsocial.core.navigation.HomeRoute
import com.yourcompany.musicsocial.core.navigation.SongListRoute
import com.yourcompany.musicsocial.feature.home.presentation.screen.HomeRoute as HomeRouteScreen

/**
 * Registers the Home section. Future Home screens are added inside this nested graph.
 * [onOpenSongList] opens the full chart (the app shell owns that destination).
 */
fun NavGraphBuilder.homeSection(
    contentPadding: PaddingValues,
    onOpenSearch: () -> Unit,
    onOpenSongList: (SongListRoute) -> Unit,
) {
    navigation<HomeGraph>(startDestination = HomeRoute) {
        composable<HomeRoute> {
            HomeRouteScreen(contentPadding = contentPadding, onOpenSearch = onOpenSearch, onOpenSongList = onOpenSongList)
        }
    }
}
