package com.yourcompany.musicsocial.data.user.repository

import com.yourcompany.musicsocial.core.domain.model.FollowRequest
import com.yourcompany.musicsocial.core.domain.model.FollowStatus
import com.yourcompany.musicsocial.core.domain.model.Page
import com.yourcompany.musicsocial.core.domain.model.PageRequest
import com.yourcompany.musicsocial.core.domain.model.ProfileUpdate
import com.yourcompany.musicsocial.core.domain.model.Relationship
import com.yourcompany.musicsocial.core.domain.model.UserProfile
import com.yourcompany.musicsocial.core.domain.repository.UserRepository
import com.yourcompany.musicsocial.core.domain.result.AppError
import com.yourcompany.musicsocial.core.domain.result.AppResult
import com.yourcompany.musicsocial.core.domain.result.map
import com.yourcompany.musicsocial.data.user.datasource.UserRemoteDataSource
import com.yourcompany.musicsocial.core.network.parseSupabaseTimestamp
import com.yourcompany.musicsocial.data.user.mapper.toDomain
import java.time.Instant
import javax.inject.Inject

internal class UserRepositoryImpl @Inject constructor(
    private val remote: UserRemoteDataSource,
) : UserRepository {

    override suspend fun getProfile(userId: String): AppResult<UserProfile> =
        remote.getProfile(userId).map { it.toDomain() }

    override suspend fun getProfileByUsername(username: String): AppResult<UserProfile> =
        remote.getProfileByUsername(username.trim()).map { it.toDomain() }

    override suspend fun updateMyProfile(update: ProfileUpdate): AppResult<UserProfile> =
        remote.updateMyProfile(update).map { it.toDomain() }

    override suspend fun searchProfiles(query: String, page: PageRequest): AppResult<Page<UserProfile>> {
        val trimmed = query.trim()
        if (trimmed.length < MIN_QUERY_LENGTH) return AppResult.Success(Page(emptyList(), nextCursor = null))

        val me = remote.currentUserId()
        return remote.searchProfiles(trimmed, afterUsername = page.cursor, limit = page.limit).map { rows ->
            // A full page means there may be more; the last username is the cursor for the next one.
            Page(
                // People search is for finding others; the signed-in user is left out.
                items = rows.filter { it.id != me }.map { it.toDomain() },
                nextCursor = if (rows.size == page.limit) rows.last().username else null,
            )
        }
    }

    override suspend fun getSuggestedProfiles(limit: Int): AppResult<List<UserProfile>> =
        remote.getSuggestedProfiles(limit).map { rows -> rows.map { it.toDomain() } }

    override suspend fun getFollowingIds(): AppResult<Set<String>> = remote.getFollowingIds()

    override suspend fun getRequestedIds(): AppResult<Set<String>> = remote.getRequestedIds()

    override suspend fun getRelationship(userId: String): AppResult<Relationship> {
        val edges = when (val result = remote.getFollowsBetween(userId)) {
            is AppResult.Failure -> return result
            is AppResult.Success -> result.data
        }
        val iFollow = edges.any { it.followeeId == userId }
        val followsMe = edges.any { it.followerId == userId }
        if (iFollow) return AppResult.Success(Relationship(FollowStatus.FOLLOWING, followsMe))
        return remote.isRequestPending(userId).map { pending ->
            Relationship(if (pending) FollowStatus.REQUESTED else FollowStatus.NONE, followsMe)
        }
    }

    /** A request that is already waiting is what the user asked for, so a duplicate is not an error. */
    override suspend fun requestFollow(userId: String): AppResult<Unit> =
        when (val result = remote.insertFollowRequest(userId)) {
            is AppResult.Failure -> if (result.error == AppError.Conflict) AppResult.Success(Unit) else result
            is AppResult.Success -> result
        }

    override suspend fun cancelFollowRequest(userId: String): AppResult<Unit> = remote.deleteOutgoingRequest(userId)

    override suspend fun unfollow(userId: String): AppResult<Unit> = remote.unfollow(userId)

    override suspend fun getIncomingRequests(): AppResult<List<FollowRequest>> =
        remote.getIncomingRequests().map { rows ->
            rows.map { FollowRequest(it.requester.toDomain(), requestedAt = parseSupabaseTimestamp(it.createdAt) ?: Instant.EPOCH) }
        }

    /** False from the backend means the request is gone already (cancelled or handled on another device). */
    override suspend fun acceptFollowRequest(requesterId: String): AppResult<Unit> =
        remote.acceptFollowRequest(requesterId).map { }

    override suspend fun declineFollowRequest(requesterId: String): AppResult<Unit> = remote.deleteIncomingRequest(requesterId)

    private companion object {
        const val MIN_QUERY_LENGTH = 2
    }
}
