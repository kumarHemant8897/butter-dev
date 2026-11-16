package com.yourcompany.musicsocial.data.user.datasource

import com.yourcompany.musicsocial.core.domain.model.ProfileUpdate
import com.yourcompany.musicsocial.core.domain.result.AppResult
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Remote contract for `public.profiles`, `public.follows` and `public.follow_requests`.
 * Authorisation is Row Level Security: the client never decides who it acts as. Writes name the
 * signed-in user only because RLS requires the column to equal auth.uid() and rejects anything else.
 */
interface UserRemoteDataSource {
    suspend fun getProfile(userId: String): AppResult<ProfileDto>

    suspend fun getProfileByUsername(username: String): AppResult<ProfileDto>

    suspend fun updateMyProfile(update: ProfileUpdate): AppResult<ProfileDto>

    /** Keyset pagination on username: pass the last username of the previous page, or null for the first. */
    suspend fun searchProfiles(query: String, afterUsername: String?, limit: Int): AppResult<List<ProfileDto>>

    suspend fun getSuggestedProfiles(limit: Int): AppResult<List<ProfileDto>>

    suspend fun getFollowingIds(): AppResult<Set<String>>

    suspend fun getRequestedIds(): AppResult<Set<String>>

    suspend fun getFollowers(userId: String, limit: Int): AppResult<List<ProfileDto>>

    suspend fun getFollowing(userId: String, limit: Int): AppResult<List<ProfileDto>>

    /** Follow rows between the signed-in user and [userId], in either direction. */
    suspend fun getFollowsBetween(userId: String): AppResult<List<FollowEdgeDto>>

    suspend fun isRequestPending(targetId: String): AppResult<Boolean>

    suspend fun insertFollowRequest(targetId: String): AppResult<Unit>

    /** Deletes the request from the signed-in user to [targetId] (cancel). */
    suspend fun deleteOutgoingRequest(targetId: String): AppResult<Unit>

    /** Deletes the request from [requesterId] to the signed-in user (decline). */
    suspend fun deleteIncomingRequest(requesterId: String): AppResult<Unit>

    suspend fun getIncomingRequests(): AppResult<List<IncomingRequestDto>>

    /** True when a waiting request was turned into a follow; false when there was none. */
    suspend fun acceptFollowRequest(requesterId: String): AppResult<Boolean>

    suspend fun unfollow(followeeId: String): AppResult<Unit>

    suspend fun insertBlock(blockedId: String): AppResult<Unit>

    suspend fun deleteBlock(blockedId: String): AppResult<Unit>

    suspend fun getBlockedIds(): AppResult<Set<String>>

    suspend fun insertReport(targetUserId: String, reason: String, details: String?): AppResult<Unit>

    /** Stores [jpeg] under avatars/<my id>/ and returns its public address. */
    suspend fun uploadAvatar(jpeg: ByteArray, fileName: String): AppResult<String>

    /** The signed-in user's id, or null when signed out. */
    fun currentUserId(): String?
}

/** Row of `public.profiles`. Field names mirror backend/supabase/migrations. */
@Serializable
data class ProfileDto(
    val id: String,
    val username: String,
    @SerialName("display_name") val displayName: String,
    val bio: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("follower_count") val followerCount: Int = 0,
    @SerialName("following_count") val followingCount: Int = 0,
)

@Serializable
data class FollowEdgeDto(
    @SerialName("follower_id") val followerId: String,
    @SerialName("followee_id") val followeeId: String,
)

/** A request to the signed-in user with the requester's profile embedded. */
@Serializable
data class IncomingRequestDto(
    @SerialName("created_at") val createdAt: String,
    val requester: ProfileDto,
)
