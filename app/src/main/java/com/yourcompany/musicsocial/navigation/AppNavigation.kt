package com.yourcompany.musicsocial.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.yourcompany.musicsocial.core.navigation.HomeGraph
import com.yourcompany.musicsocial.feature.chat.navigation.chatsSection
import com.yourcompany.musicsocial.feature.discover.navigation.discoverSection
import com.yourcompany.musicsocial.feature.home.navigation.homeSection
import com.yourcompany.musicsocial.feature.player.navigation.playerSection
import com.yourcompany.musicsocial.feature.profile.navigation.profileSection

/** Root UI: scaffold with the floating bottom bar and the app's navigation graph. */
@Composable
fun MusicSocialAppShell(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = { AppBottomNavigation(navController) },
    ) { innerPadding ->
        AppNavigation(navController = navController, contentPadding = innerPadding)
    }
}

/**
 * The app graph is assembled from one `*Section` per feature. Each feature owns its nested graph;
 * the app only decides the order and the start destination.
 */
@Composable
fun AppNavigation(
    navController: NavHostController,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = HomeGraph,
        modifier = modifier,
        enterTransition = { fadeIn(tween(durationMillis = 220)) },
        exitTransition = { fadeOut(tween(durationMillis = 160)) },
        popEnterTransition = { fadeIn(tween(durationMillis = 220)) },
        popExitTransition = { fadeOut(tween(durationMillis = 160)) },
    ) {
        homeSection(contentPadding)
        discoverSection(contentPadding)
        playerSection(contentPadding)
        chatsSection(contentPadding)
        profileSection(contentPadding)
    }
}
