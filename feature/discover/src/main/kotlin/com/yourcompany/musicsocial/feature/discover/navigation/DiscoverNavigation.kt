package com.yourcompany.musicsocial.feature.discover.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.yourcompany.musicsocial.core.navigation.DiscoverGraph
import com.yourcompany.musicsocial.core.navigation.DiscoverRoute
import com.yourcompany.musicsocial.feature.discover.presentation.screen.DiscoverRoute as DiscoverRouteScreen

fun NavGraphBuilder.discoverSection(contentPadding: PaddingValues) {
    navigation<DiscoverGraph>(startDestination = DiscoverRoute) {
        composable<DiscoverRoute> { DiscoverRouteScreen(contentPadding = contentPadding) }
    }
}
