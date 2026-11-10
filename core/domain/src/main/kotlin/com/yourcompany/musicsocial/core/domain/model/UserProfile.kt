package com.yourcompany.musicsocial.core.domain.model

/** Public profile of a user, as shown on profile screens and in follower lists. */
data class UserProfile(
    val id: String,
    val username: String,
    val displayName: String,
    val bio: String?,
    val avatarUrl: String?,
    val followerCount: Int,
    val followingCount: Int,
)

/** Compact user reference used inside chat members, message senders and song shares. */
data class UserSummary(
    val id: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String?,
)

/** Fields the signed-in user may change on their own profile. Null means "leave unchanged". */
data class ProfileUpdate(
    val username: String? = null,
    val displayName: String? = null,
    val bio: String? = null,
    val avatarUrl: String? = null,
)
