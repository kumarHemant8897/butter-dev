package com.yourcompany.musicsocial.core.domain.model

import java.time.Instant

/** Where the signed-in user stands with another person. Every account approves its followers. */
enum class FollowStatus {
    NONE,

    /** A follow request is waiting for the other person to accept. */
    REQUESTED,
    FOLLOWING,
}

/** The signed-in user's relationship with one other person, as seen on their profile. */
data class Relationship(
    val status: FollowStatus,
    /** The other person follows the signed-in user. */
    val followsMe: Boolean,
) {
    /** Liked and saved songs are visible to approved followers only (enforced by the database). */
    val canSeeLibrary: Boolean get() = status == FollowStatus.FOLLOWING

    /** A chat may start once a follow request was accepted in either direction (enforced by the database). */
    val canMessage: Boolean get() = status == FollowStatus.FOLLOWING || followsMe
}

/** Someone asked to follow the signed-in user. */
data class FollowRequest(val requester: UserProfile, val requestedAt: Instant)

/** The two song lists every user has. Shown on profiles to the owner and approved followers. */
enum class LibraryKind { LIKED, SAVED }

/** One row of the chat list. */
data class ConversationSummary(
    val id: String,
    val type: ConversationType,
    /** Group title; null for direct chats, where [otherUser] names the conversation. */
    val title: String?,
    val otherUser: UserSummary?,
    val lastMessage: LastMessagePreview?,
    val unreadCount: Int,
    val updatedAt: Instant,
)

data class LastMessagePreview(val senderId: String, val content: MessageContent)
