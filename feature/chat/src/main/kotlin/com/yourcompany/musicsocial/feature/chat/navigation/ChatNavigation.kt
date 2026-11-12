package com.yourcompany.musicsocial.feature.chat.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.yourcompany.musicsocial.core.navigation.ChatListRoute
import com.yourcompany.musicsocial.core.navigation.ChatsGraph
import com.yourcompany.musicsocial.core.navigation.ConversationRoute
import com.yourcompany.musicsocial.feature.chat.presentation.screen.ChatListRoute as ChatListRouteScreen
import com.yourcompany.musicsocial.feature.chat.presentation.screen.ConversationRoute as ConversationRouteScreen

/** The Chats tab. [onFindPeople] opens people search, which lives in the profile feature. */
fun NavGraphBuilder.chatsSection(navController: NavController, contentPadding: PaddingValues, onFindPeople: () -> Unit) {
    navigation<ChatsGraph>(startDestination = ChatListRoute) {
        composable<ChatListRoute> {
            ChatListRouteScreen(
                contentPadding = contentPadding,
                onFindPeople = onFindPeople,
                onOpenConversation = { route -> navController.navigate(route) },
            )
        }
    }
}

/** A conversation, opened from the chat list or from a profile's Message button. */
fun NavGraphBuilder.chatDestinations(navController: NavController, contentPadding: PaddingValues, onOpenProfile: (String) -> Unit) {
    composable<ConversationRoute> {
        ConversationRouteScreen(
            contentPadding = contentPadding,
            onBack = { navController.popBackStack() },
            onOpenProfile = onOpenProfile,
        )
    }
}
