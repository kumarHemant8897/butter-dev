package com.yourcompany.musicsocial.data.user.datasource

import com.yourcompany.musicsocial.core.domain.result.AppResult
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Remote contract for `public.profiles` and `public.follows`.
 * Not implemented yet (Phase 2). Implementations must rely on RLS for authorisation:
 * the client never sends a user id it wants to act as; the server uses auth.uid().
 */
interface UserRemoteDataSource {
    suspend fun getProfile(userId: String): AppResult<ProfileDto>

    suspend fun getProfileByUsername(username: String): AppResult<ProfileDto>

    suspend fun updateMyProfile(update: ProfileUpdateDto): AppResult<ProfileDto>

    /** Keyset pagination on (username, id). [afterUsername] is null for the first page. */
    suspend fun searchProfiles(query: String, afterUsername: String?, limit: Int): AppResult<List<ProfileDto>>

    suspend fun follow(followeeId: String): AppResult<Unit>

    suspend fun unfollow(followeeId: String): AppResult<Unit>
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

/** PATCH body for the caller's own profile. Null fields are omitted (explicitNulls = false). */
@Serializable
data class ProfileUpdateDto(
    val username: String? = null,
    @SerialName("display_name") val displayName: String? = null,
    val bio: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
)
