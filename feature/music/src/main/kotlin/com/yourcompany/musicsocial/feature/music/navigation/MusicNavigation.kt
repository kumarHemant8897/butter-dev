package com.yourcompany.musicsocial.feature.music.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.yourcompany.musicsocial.core.navigation.SongListRoute
import com.yourcompany.musicsocial.feature.music.presentation.screen.SongListRoute as SongListRouteScreen

/** Registers the shared song list (charts, genre charts, artist pages). Open it with `navigate(SongListRoute(...))`. */
fun NavGraphBuilder.musicDestinations(navController: NavController, contentPadding: PaddingValues) {
    composable<SongListRoute> {
        SongListRouteScreen(contentPadding = contentPadding, onBack = { navController.popBackStack() })
    }
}
