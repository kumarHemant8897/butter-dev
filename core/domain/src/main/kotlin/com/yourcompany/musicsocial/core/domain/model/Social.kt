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
    /** The signed-in user blocked this person. (Being blocked by them is never revealed.) */
    val blockedByMe: Boolean = false,
) {
    /** Liked and saved songs are visible to approved followers only (enforced by the database). */
    val canSeeLibrary: Boolean get() = !blockedByMe && status == FollowStatus.FOLLOWING

    /** A chat may start once a follow request was accepted in either direction and nobody blocked the other (enforced by the database). */
    val canMessage: Boolean get() = !blockedByMe && (status == FollowStatus.FOLLOWING || followsMe)
}

/** Why someone is reported. [code] matches the reports.reason CHECK constraint. */
enum class ReportReason(val code: String) {
    SPAM("spam"),
    HARASSMENT("harassment"),
    INAPPROPRIATE("inappropriate"),
    IMPERSONATION("impersonation"),
    OTHER("other"),
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
    /** Everyone in the conversation, including the signed-in user. */
    val memberCount: Int = 2,
)

data class LastMessagePreview(val senderId: String, val content: MessageContent, val senderName: String? = null)
