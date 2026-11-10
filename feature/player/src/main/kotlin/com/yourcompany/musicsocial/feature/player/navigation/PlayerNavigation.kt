package com.yourcompany.musicsocial.feature.player.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.yourcompany.musicsocial.core.navigation.PlayerGraph
import com.yourcompany.musicsocial.core.navigation.PlayerRoute
import com.yourcompany.musicsocial.feature.player.presentation.screen.PlayerRoute as PlayerRouteScreen

fun NavGraphBuilder.playerSection(contentPadding: PaddingValues) {
    navigation<PlayerGraph>(startDestination = PlayerRoute) {
        composable<PlayerRoute> { PlayerRouteScreen(contentPadding = contentPadding) }
    }
}
