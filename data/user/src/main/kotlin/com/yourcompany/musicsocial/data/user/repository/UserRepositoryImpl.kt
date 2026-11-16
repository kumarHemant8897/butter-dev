package com.yourcompany.musicsocial.data.user.repository

import com.yourcompany.musicsocial.core.domain.model.FollowRequest
import com.yourcompany.musicsocial.core.domain.model.FollowStatus
import com.yourcompany.musicsocial.core.domain.model.Page
import com.yourcompany.musicsocial.core.domain.model.PageRequest
import com.yourcompany.musicsocial.core.domain.model.ProfileUpdate
import com.yourcompany.musicsocial.core.domain.model.Relationship
import com.yourcompany.musicsocial.core.domain.model.ReportReason
import com.yourcompany.musicsocial.core.domain.result.getOrNull
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
        val blocked = remote.getBlockedIds().getOrNull().orEmpty()
        return remote.searchProfiles(trimmed, afterUsername = page.cursor, limit = page.limit).map { rows ->
            // A full page means there may be more; the last username is the cursor for the next one.
            Page(
                // People search is for finding others: the signed-in user and people they blocked are left out.
                items = rows.filter { it.id != me && it.id !in blocked }.map { it.toDomain() },
                nextCursor = if (rows.size == page.limit) rows.last().username else null,
            )
        }
    }

    override suspend fun getSuggestedProfiles(limit: Int): AppResult<List<UserProfile>> {
        val blocked = remote.getBlockedIds().getOrNull().orEmpty()
        return remote.getSuggestedProfiles(limit).map { rows -> rows.filter { it.id !in blocked }.map { it.toDomain() } }
    }

    override suspend fun getFollowingIds(): AppResult<Set<String>> = remote.getFollowingIds()

    override suspend fun getRequestedIds(): AppResult<Set<String>> = remote.getRequestedIds()

    override suspend fun getFollowers(userId: String): AppResult<List<UserProfile>> =
        remote.getFollowers(userId, FOLLOW_LIST_LIMIT).map { rows -> rows.map { it.toDomain() } }

    override suspend fun getFollowing(userId: String): AppResult<List<UserProfile>> =
        remote.getFollowing(userId, FOLLOW_LIST_LIMIT).map { rows -> rows.map { it.toDomain() } }

    override suspend fun getRelationship(userId: String): AppResult<Relationship> {
        val blocked = when (val result = remote.getBlockedIds()) {
            is AppResult.Failure -> return result
            is AppResult.Success -> userId in result.data
        }
        // A block removes every follow and request between the two, so nothing else to read.
        if (blocked) return AppResult.Success(Relationship(FollowStatus.NONE, followsMe = false, blockedByMe = true))
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

    /** Blocking twice is what the user asked for, so a duplicate is not an error. */
    override suspend fun block(userId: String): AppResult<Unit> =
        when (val result = remote.insertBlock(userId)) {
            is AppResult.Failure -> if (result.error == AppError.Conflict) AppResult.Success(Unit) else result
            is AppResult.Success -> result
        }

    override suspend fun unblock(userId: String): AppResult<Unit> = remote.deleteBlock(userId)

    override suspend fun getBlockedIds(): AppResult<Set<String>> = remote.getBlockedIds()

    /**
     * A new file name for every upload: the photo's address changes, so other phones and the image
     * cache show the new photo instead of a cached old one. Old files stay until a cleanup job exists.
     */
    override suspend fun uploadAvatar(jpeg: ByteArray): AppResult<UserProfile> {
        if (jpeg.isEmpty()) return AppResult.Failure(AppError.Validation("avatar", "empty"))
        if (jpeg.size > AVATAR_MAX_BYTES) return AppResult.Failure(AppError.Validation("avatar", "too_large"))
        val url = when (val result = remote.uploadAvatar(jpeg, "avatar-${System.currentTimeMillis()}.jpg")) {
            is AppResult.Failure -> return result
            is AppResult.Success -> result.data
        }
        return remote.updateMyProfile(ProfileUpdate(avatarUrl = url)).map { it.toDomain() }
    }

    override suspend fun report(userId: String, reason: ReportReason, details: String?): AppResult<Unit> {
        val text = details?.trim()?.takeIf { it.isNotEmpty() }
        if (text != null && text.length > REPORT_DETAILS_MAX) return AppResult.Failure(AppError.Validation("details", "too_long"))
        return remote.insertReport(userId, reason.code, text)
    }

    private companion object {
        const val MIN_QUERY_LENGTH = 2
        const val FOLLOW_LIST_LIMIT = 200

        /** Matches the avatars bucket's file size limit. */
        const val AVATAR_MAX_BYTES = 2 * 1024 * 1024

        /** Matches the reports.details CHECK constraint. */
        const val REPORT_DETAILS_MAX = 1000
    }
}
