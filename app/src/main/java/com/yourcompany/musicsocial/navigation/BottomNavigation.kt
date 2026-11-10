package com.yourcompany.musicsocial.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
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
    val selected = backStackEntry?.destination.currentScreen()

    val labels = Screen.entries.map { stringResource(it.label) }
    val items = remember(labels) {
        Screen.entries.mapIndexed { index, screen ->
            BottomNavItem(label = labels[index], icon = screen.icon)
        }.toImmutableList()
    }

    BottomNavigationBar(
        items = items,
        selectedIndex = selected?.ordinal ?: -1,
        onItemSelected = { index -> navController.navigateToTopLevel(Screen.entries[index]) },
        modifier = modifier,
    )
}

internal fun NavDestination?.currentScreen(): Screen? =
    this?.hierarchy?.let { hierarchy ->
        Screen.entries.firstOrNull { screen -> hierarchy.any { it.hasRoute(screen.graphRouteClass) } }
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
