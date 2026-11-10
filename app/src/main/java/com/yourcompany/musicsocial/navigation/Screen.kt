package com.yourcompany.musicsocial.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.yourcompany.musicsocial.R
import com.yourcompany.musicsocial.core.designsystem.icon.AppIcons
import com.yourcompany.musicsocial.core.navigation.ChatsGraph
import com.yourcompany.musicsocial.core.navigation.DiscoverGraph
import com.yourcompany.musicsocial.core.navigation.HomeGraph
import com.yourcompany.musicsocial.core.navigation.PlayerGraph
import com.yourcompany.musicsocial.core.navigation.ProfileGraph
import kotlin.reflect.KClass

/**
 * The five bottom-bar sections. Each points at its feature's nested graph route, so selecting a
 * tab returns to that section's own back stack.
 */
enum class Screen(
    val graphRoute: Any,
    @StringRes val label: Int,
    @DrawableRes val icon: Int,
) {
    HOME(HomeGraph, R.string.nav_home, AppIcons.Home),
    DISCOVER(DiscoverGraph, R.string.nav_discover, AppIcons.Discover),
    PLAYER(PlayerGraph, R.string.nav_player, AppIcons.Player),
    CHATS(ChatsGraph, R.string.nav_chats, AppIcons.Chats),
    PROFILE(ProfileGraph, R.string.nav_profile, AppIcons.Profile),
    ;

    val graphRouteClass: KClass<*> get() = graphRoute::class
}
