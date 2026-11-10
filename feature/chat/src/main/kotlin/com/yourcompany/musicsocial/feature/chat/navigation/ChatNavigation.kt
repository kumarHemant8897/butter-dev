package com.yourcompany.musicsocial.feature.chat.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.yourcompany.musicsocial.core.navigation.ChatListRoute
import com.yourcompany.musicsocial.core.navigation.ChatsGraph
import com.yourcompany.musicsocial.feature.chat.presentation.screen.ChatListRoute as ChatListRouteScreen

/** Conversation detail (`ConversationRoute(conversationId)`) will be added inside this graph. */
fun NavGraphBuilder.chatsSection(contentPadding: PaddingValues) {
    navigation<ChatsGraph>(startDestination = ChatListRoute) {
        composable<ChatListRoute> { ChatListRouteScreen(contentPadding = contentPadding) }
    }
}
