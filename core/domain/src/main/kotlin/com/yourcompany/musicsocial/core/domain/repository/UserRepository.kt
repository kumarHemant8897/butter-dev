package com.yourcompany.musicsocial.core.domain.repository

import com.yourcompany.musicsocial.core.domain.model.FollowRequest
import com.yourcompany.musicsocial.core.domain.model.Page
import com.yourcompany.musicsocial.core.domain.model.PageRequest
import com.yourcompany.musicsocial.core.domain.model.ProfileUpdate
import com.yourcompany.musicsocial.core.domain.model.Relationship
import com.yourcompany.musicsocial.core.domain.model.UserProfile
import com.yourcompany.musicsocial.core.domain.result.AppResult

/**
 * Profiles and the follow graph. Every account approves its followers: [requestFollow] creates a
 * request, and only the other person can turn it into a follow with [acceptFollowRequest].
 */
interface UserRepository {
    suspend fun getProfile(userId: String): AppResult<UserProfile>

    suspend fun getProfileByUsername(username: String): AppResult<UserProfile>

    /** Updates the signed-in user's own profile. RLS rejects updates to anyone else's row. */
    suspend fun updateMyProfile(update: ProfileUpdate): AppResult<UserProfile>

    /** Cursor is the last username of the previous page (usernames are unique and sortable). */
    suspend fun searchProfiles(query: String, page: PageRequest): AppResult<Page<UserProfile>>

    /** People other than the signed-in user, newest first. A simple stand-in until recommendations exist. */
    suspend fun getSuggestedProfiles(limit: Int): AppResult<List<UserProfile>>

    /** Ids of everyone the signed-in user follows. */
    suspend fun getFollowingIds(): AppResult<Set<String>>

    /** Ids of everyone the signed-in user has asked to follow and who has not answered yet. */
    suspend fun getRequestedIds(): AppResult<Set<String>>

    suspend fun getRelationship(userId: String): AppResult<Relationship>

    /** Idempotent: asking again while a request is waiting succeeds. */
    suspend fun requestFollow(userId: String): AppResult<Unit>

    suspend fun cancelFollowRequest(userId: String): AppResult<Unit>

    suspend fun unfollow(userId: String): AppResult<Unit>

    /** Requests waiting for the signed-in user, newest first. */
    suspend fun getIncomingRequests(): AppResult<List<FollowRequest>>

    /** Idempotent: a request that was already handled or cancelled is not an error. */
    suspend fun acceptFollowRequest(requesterId: String): AppResult<Unit>

    suspend fun declineFollowRequest(requesterId: String): AppResult<Unit>
}
