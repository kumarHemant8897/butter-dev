package com.yourcompany.musicsocial.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.yourcompany.musicsocial.core.navigation.HomeGraph
import androidx.navigation.NavDestination.Companion.hasRoute
import com.yourcompany.musicsocial.core.navigation.ConversationRoute
import com.yourcompany.musicsocial.core.navigation.PeopleSearchRoute
import com.yourcompany.musicsocial.core.navigation.SongListRoute
import com.yourcompany.musicsocial.core.navigation.UserProfileRoute
import com.yourcompany.musicsocial.feature.chat.navigation.chatDestinations
import com.yourcompany.musicsocial.feature.music.navigation.musicDestinations
import com.yourcompany.musicsocial.feature.profile.navigation.peopleDestinations
import com.yourcompany.musicsocial.core.ui.component.MiniPlayer
import com.yourcompany.musicsocial.feature.chat.navigation.chatsSection
import com.yourcompany.musicsocial.feature.discover.navigation.discoverSection
import com.yourcompany.musicsocial.feature.home.navigation.homeSection
import com.yourcompany.musicsocial.feature.player.navigation.playerSection
import com.yourcompany.musicsocial.feature.profile.navigation.profileSection

/** Root UI: scaffold with the mini player, the bottom bar and the app's navigation graph. */
@Composable
fun MusicSocialAppShell(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    shellViewModel: ShellViewModel = hiltViewModel(),
) {
    val playback by shellViewModel.playback.collectAsStateWithLifecycle()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val onPlayerTab = backStackEntry?.destination.currentScreen() == Screen.PLAYER
    // A conversation needs the whole height for the thread and the keyboard.
    val inConversation = backStackEntry?.destination?.hasRoute(ConversationRoute::class) == true
    val song = playback.song

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (!inConversation) Column {
                // Shown on every tab except Player (which already is the full player).
                AnimatedVisibility(
                    visible = song != null && !onPlayerTab,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    if (song != null) {
                        MiniPlayer(
                            title = song.title,
                            artist = song.artistNames,
                            artworkUrl = song.artworkUrl,
                            tintKey = song.id,
                            isPlaying = playback.isPlaying,
                            isLoading = playback.isLoading,
                            progress = playback.progress,
                            hasNext = playback.hasNext,
                            onTogglePlay = shellViewModel::onTogglePlay,
                            onPrevious = shellViewModel::onPrevious,
                            onNext = shellViewModel::onNext,
                            onSeek = shellViewModel::onSeek,
                            onOpenPlayer = { navController.navigateToTopLevel(Screen.PLAYER) },
                        )
                    }
                }
                AppBottomNavigation(navController)
            }
        },
    ) { innerPadding ->
        // The top inset becomes real padding (content is cut off below the status bar) instead of
        // scroll padding; otherwise scrolled content slides under the transparent status bar and
        // overlaps the clock. Bottom stays scroll padding so lists can scroll above the bar.
        val layoutDirection = LocalLayoutDirection.current
        AppNavigation(
            navController = navController,
            contentPadding = PaddingValues(
                start = innerPadding.calculateStartPadding(layoutDirection),
                end = innerPadding.calculateEndPadding(layoutDirection),
                bottom = innerPadding.calculateBottomPadding(),
            ),
            modifier = Modifier.padding(top = innerPadding.calculateTopPadding()),
        )
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
        val openSongList: (SongListRoute) -> Unit = { route -> navController.navigate(route) }
        val openProfile: (String) -> Unit = { userId -> navController.navigate(UserProfileRoute(userId)) }
        val findPeople: () -> Unit = { navController.navigate(PeopleSearchRoute) }
        homeSection(contentPadding, onOpenSearch = { navController.navigateToTopLevel(Screen.DISCOVER) }, onOpenSongList = openSongList)
        discoverSection(contentPadding, onOpenSongList = openSongList, onOpenProfile = openProfile, onFindPeople = findPeople)
        playerSection(contentPadding)
        chatsSection(navController, contentPadding, onFindPeople = findPeople)
        profileSection(navController, contentPadding)
        // Shared screens opened from several tabs sit outside the tab graphs.
        musicDestinations(navController, contentPadding)
        peopleDestinations(navController, contentPadding, onOpenConversation = { route -> navController.navigate(route) })
        chatDestinations(navController, contentPadding, onOpenProfile = openProfile)
    }
}
