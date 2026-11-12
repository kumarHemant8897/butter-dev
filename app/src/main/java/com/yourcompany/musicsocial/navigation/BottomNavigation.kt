package com.yourcompany.musicsocial.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.yourcompany.musicsocial.core.designsystem.component.BottomNavItem
import com.yourcompany.musicsocial.core.designsystem.component.BottomNavigationBar
import kotlinx.collections.immutable.toImmutableList

/** Connects the stateless design-system bar to the NavController. */
@Composable
fun AppBottomNavigation(navController: NavHostController, modifier: Modifier = Modifier) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val backStack by navController.currentBackStack.collectAsState()
    val current = backStackEntry?.destination.currentScreen()
    // Shared screens outside the tab graphs (a chart, a profile, a chat) keep the tab they were opened
    // from highlighted: the nearest tab screen below them in the back stack.
    val selected = current ?: backStack.asReversed().firstNotNullOfOrNull { it.destination.currentScreen() }

    val labels = Screen.entries.map { stringResource(it.label) }
    val items = remember(labels) {
        Screen.entries.mapIndexed { index, screen ->
            BottomNavItem(label = labels[index], icon = screen.icon)
        }.toImmutableList()
    }

    BottomNavigationBar(
        items = items,
        selectedIndex = selected?.ordinal ?: -1,
        onItemSelected = { index ->
            val screen = Screen.entries[index]
            // On a shared screen, tapping the highlighted tab goes back to that tab's first screen.
            if (current == null && screen == selected) navController.popToTabStart(screen) else navController.navigateToTopLevel(screen)
        },
        modifier = modifier,
    )
}

internal fun NavDestination?.currentScreen(): Screen? =
    this?.hierarchy?.let { hierarchy ->
        Screen.entries.firstOrNull { screen -> hierarchy.any { it.hasRoute(screen.graphRouteClass) } }
    }

/** Pops shared screens off the stack until [screen]'s start destination is on top. */
private fun NavController.popToTabStart(screen: Screen) {
    val tabGraph = graph.findNode(screen.graphRoute) as? NavGraph
    if (tabGraph == null || !popBackStack(tabGraph.findStartDestination().id, inclusive = false)) navigateToTopLevel(screen)
}

/**
 * Standard bottom-navigation behaviour:
 * - pop back to the start destination so the back stack does not grow with every tab switch,
 * - save and restore each tab's state (scroll position, nested screens),
 * - avoid duplicate copies when the current tab is selected again.
 */
fun NavController.navigateToTopLevel(screen: Screen) {
    navigate(screen.graphRoute) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
