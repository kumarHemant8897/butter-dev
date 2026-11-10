package com.yourcompany.musicsocial.feature.profile.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.yourcompany.musicsocial.core.navigation.ProfileGraph
import com.yourcompany.musicsocial.core.navigation.ProfileRoute
import com.yourcompany.musicsocial.feature.profile.presentation.screen.ProfileRoute as ProfileRouteScreen

/**
 * The tab opens the signed-in user's own profile (`ProfileRoute(userId = null)`). Other features
 * open someone else's profile with `navController.navigate(ProfileRoute(userId))`.
 */
fun NavGraphBuilder.profileSection(contentPadding: PaddingValues) {
    navigation<ProfileGraph>(startDestination = ProfileRoute()) {
        composable<ProfileRoute> { ProfileRouteScreen(contentPadding = contentPadding) }
    }
}
