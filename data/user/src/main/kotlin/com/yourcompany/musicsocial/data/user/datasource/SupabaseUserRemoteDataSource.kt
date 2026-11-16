package com.yourcompany.musicsocial.data.user.datasource

import com.yourcompany.musicsocial.core.common.logging.AppLogger
import com.yourcompany.musicsocial.core.domain.model.ProfileUpdate
import com.yourcompany.musicsocial.core.domain.result.AppError
import com.yourcompany.musicsocial.core.domain.result.AppResult
import com.yourcompany.musicsocial.core.domain.result.flatMap
import com.yourcompany.musicsocial.core.domain.result.map
import com.yourcompany.musicsocial.core.network.SupabaseClientProvider
import com.yourcompany.musicsocial.core.network.call
import com.yourcompany.musicsocial.core.network.requireUserId
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject

internal class SupabaseUserRemoteDataSource @Inject constructor(
    private val supabase: SupabaseClientProvider,
    private val logger: AppLogger,
) : UserRemoteDataSource {

    override suspend fun getProfile(userId: String): AppResult<ProfileDto> =
        supabase.call(logger, "profiles.get") { client ->
            client.from(PROFILES).select(PROFILE_COLUMNS) { filter { eq("id", userId) } }
                .decodeSingleOrNull<ProfileDto>()
        }.orNotFound()

    override suspend fun getProfileByUsername(username: String): AppResult<ProfileDto> =
        supabase.call(logger, "profiles.getByUsername") { client ->
            client.from(PROFILES).select(PROFILE_COLUMNS) { filter { eq("username", username.lowercase()) } }
                .decodeSingleOrNull<ProfileDto>()
        }.orNotFound()

    override suspend fun updateMyProfile(update: ProfileUpdate): AppResult<ProfileDto> =
        supabase.call(logger, "profiles.update") { client ->
            val me = client.requireUserId()
            // Only the fields being changed are sent, so nothing else is overwritten with null.
            val body = buildJsonObject {
                update.username?.let { put("username", it) }
                update.displayName?.let { put("display_name", it) }
                update.bio?.let { put("bio", it) }
                update.avatarUrl?.let { put("avatar_url", it) }
            }
            client.from(PROFILES).update(body) {
                select(PROFILE_COLUMNS)
                filter { eq("id", me) }
            }.decodeSingleOrNull<ProfileDto>()
        }.orNotFound()

    override suspend fun searchProfiles(query: String, afterUsername: String?, limit: Int): AppResult<List<ProfileDto>> =
        supabase.call(logger, "profiles.search") { client ->
            val pattern = "%${sanitizeForFilter(query)}%"
            client.from(PROFILES).select(PROFILE_COLUMNS) {
                filter {
                    // Both patterns are escaped and stripped of filter-grammar characters first.
                    or {
                        ilike("username", pattern)
                        ilike("display_name", pattern)
                    }
                    if (afterUsername != null) gt("username", afterUsername)
                }
                order("username", Order.ASCENDING)
                limit(limit.toLong())
            }.decodeList<ProfileDto>()
        }

    override suspend fun getSuggestedProfiles(limit: Int): AppResult<List<ProfileDto>> =
        supabase.call(logger, "profiles.suggested") { client ->
            val me = client.requireUserId()
            client.from(PROFILES).select(PROFILE_COLUMNS) {
                filter { neq("id", me) }
                order("created_at", Order.DESCENDING)
                limit(limit.toLong())
            }.decodeList<ProfileDto>()
        }

    override suspend fun getFollowingIds(): AppResult<Set<String>> =
        supabase.call(logger, "follows.list") { client ->
            val me = client.requireUserId()
            client.from(FOLLOWS).select(Columns.list("followee_id")) { filter { eq("follower_id", me) } }
                .decodeList<FollowRow>()
                .mapTo(HashSet()) { it.followeeId }
        }

    override suspend fun getRequestedIds(): AppResult<Set<String>> =
        supabase.call(logger, "follow_requests.outgoing") { client ->
            val me = client.requireUserId()
            client.from(REQUESTS).select(Columns.list("target_id")) { filter { eq("requester_id", me) } }
                .decodeList<RequestTargetRow>()
                .mapTo(HashSet()) { it.targetId }
        }

    override suspend fun getFollowers(userId: String, limit: Int): AppResult<List<ProfileDto>> =
        supabase.call(logger, "follows.followers") { client ->
            client.from(FOLLOWS).select(Columns.raw("created_at,person:profiles!follows_follower_id_fkey($PROFILE_FIELDS)")) {
                filter { eq("followee_id", userId) }
                order("created_at", Order.DESCENDING)
                limit(limit.toLong())
            }.decodeList<FollowPersonRow>().map { it.person }
        }

    override suspend fun getFollowing(userId: String, limit: Int): AppResult<List<ProfileDto>> =
        supabase.call(logger, "follows.following") { client ->
            client.from(FOLLOWS).select(Columns.raw("created_at,person:profiles!follows_followee_id_fkey($PROFILE_FIELDS)")) {
                filter { eq("follower_id", userId) }
                order("created_at", Order.DESCENDING)
                limit(limit.toLong())
            }.decodeList<FollowPersonRow>().map { it.person }
        }

    override suspend fun getFollowsBetween(userId: String): AppResult<List<FollowEdgeDto>> =
        supabase.call(logger, "follows.between") { client ->
            val me = client.requireUserId()
            client.from(FOLLOWS).select(Columns.list("follower_id", "followee_id")) {
                filter {
                    or {
                        and {
                            eq("follower_id", me)
                            eq("followee_id", userId)
                        }
                        and {
                            eq("follower_id", userId)
                            eq("followee_id", me)
                        }
                    }
                }
            }.decodeList<FollowEdgeDto>()
        }

    override suspend fun isRequestPending(targetId: String): AppResult<Boolean> =
        supabase.call(logger, "follow_requests.pending") { client ->
            val me = client.requireUserId()
            client.from(REQUESTS).select(Columns.list("target_id")) {
                filter {
                    eq("requester_id", me)
                    eq("target_id", targetId)
                }
            }.decodeList<RequestTargetRow>().isNotEmpty()
        }

    override suspend fun insertFollowRequest(targetId: String): AppResult<Unit> =
        supabase.call(logger, "follow_requests.insert") { client ->
            client.from(REQUESTS).insert(RequestInsert(requesterId = client.requireUserId(), targetId = targetId))
        }.map { }

    override suspend fun deleteOutgoingRequest(targetId: String): AppResult<Unit> =
        supabase.call(logger, "follow_requests.cancel") { client ->
            val me = client.requireUserId()
            client.from(REQUESTS).delete {
                filter {
                    eq("requester_id", me)
                    eq("target_id", targetId)
                }
            }
        }.map { }

    override suspend fun deleteIncomingRequest(requesterId: String): AppResult<Unit> =
        supabase.call(logger, "follow_requests.decline") { client ->
            val me = client.requireUserId()
            client.from(REQUESTS).delete {
                filter {
                    eq("requester_id", requesterId)
                    eq("target_id", me)
                }
            }
        }.map { }

    override suspend fun getIncomingRequests(): AppResult<List<IncomingRequestDto>> =
        supabase.call(logger, "follow_requests.incoming") { client ->
            val me = client.requireUserId()
            client.from(REQUESTS).select(INCOMING_REQUEST_COLUMNS) {
                filter { eq("target_id", me) }
                order("created_at", Order.DESCENDING)
                limit(MAX_INCOMING_REQUESTS)
            }.decodeList<IncomingRequestDto>()
        }

    override suspend fun acceptFollowRequest(requesterId: String): AppResult<Boolean> =
        supabase.call(logger, "follow_requests.accept") { client ->
            client.postgrest.rpc("accept_follow_request", buildJsonObject { put("requester", requesterId) })
                .decodeAs<Boolean>()
        }

    override fun currentUserId(): String? = supabase.clientOrNull()?.auth?.currentUserOrNull()?.id

    override suspend fun insertBlock(blockedId: String): AppResult<Unit> =
        supabase.call(logger, "blocks.insert") { client ->
            client.from(BLOCKS).insert(BlockRow(blockerId = client.requireUserId(), blockedId = blockedId))
        }.map { }

    override suspend fun deleteBlock(blockedId: String): AppResult<Unit> =
        supabase.call(logger, "blocks.delete") { client ->
            val me = client.requireUserId()
            client.from(BLOCKS).delete {
                filter {
                    eq("blocker_id", me)
                    eq("blocked_id", blockedId)
                }
            }
        }.map { }

    override suspend fun getBlockedIds(): AppResult<Set<String>> =
        supabase.call(logger, "blocks.list") { client ->
            val me = client.requireUserId()
            client.from(BLOCKS).select(Columns.list("blocker_id", "blocked_id")) { filter { eq("blocker_id", me) } }
                .decodeList<BlockRow>()
                .mapTo(HashSet()) { it.blockedId }
        }

    override suspend fun insertReport(targetUserId: String, reason: String, details: String?): AppResult<Unit> =
        supabase.call(logger, "reports.insert") { client ->
            client.from(REPORTS).insert(ReportRow(reporterId = client.requireUserId(), targetUserId = targetUserId, reason = reason, details = details))
        }.map { }

    override suspend fun uploadAvatar(jpeg: ByteArray, fileName: String): AppResult<String> =
        supabase.call(logger, "avatars.upload") { client ->
            // The folder must be the user's own id: the storage policy rejects anything else.
            val path = "${client.requireUserId()}/$fileName"
            val bucket = client.storage.from(AVATARS)
            // Every upload has a fresh file name, so no upsert: an upsert needs a SELECT
            // policy on storage.objects, which would let users list everyone's files.
            bucket.upload(path, jpeg) {
                upsert = false
                contentType = ContentType.Image.JPEG
            }
            bucket.publicUrl(path)
        }

    override suspend fun unfollow(followeeId: String): AppResult<Unit> =
        supabase.call(logger, "follows.delete") { client ->
            val me = client.requireUserId()
            client.from(FOLLOWS).delete {
                filter {
                    eq("follower_id", me)
                    eq("followee_id", followeeId)
                }
            }
        }.map { }

    private fun <T> AppResult<T?>.orNotFound(): AppResult<T> =
        flatMap { value -> value?.let { AppResult.Success(it) } ?: AppResult.Failure(AppError.NotFound) }

    @Serializable
    private data class FollowRow(@SerialName("followee_id") val followeeId: String)

    @Serializable
    private data class FollowPersonRow(val person: ProfileDto)

    @Serializable
    private data class BlockRow(
        @SerialName("blocker_id") val blockerId: String,
        @SerialName("blocked_id") val blockedId: String,
    )

    @Serializable
    private data class ReportRow(
        @SerialName("reporter_id") val reporterId: String,
        @SerialName("target_user_id") val targetUserId: String,
        val reason: String,
        val details: String? = null,
    )

    @Serializable
    private data class RequestTargetRow(@SerialName("target_id") val targetId: String)

    @Serializable
    private data class RequestInsert(
        @SerialName("requester_id") val requesterId: String,
        @SerialName("target_id") val targetId: String,
    )

    internal companion object {
        const val PROFILES = "profiles"
        const val FOLLOWS = "follows"
        const val REQUESTS = "follow_requests"
        const val BLOCKS = "blocks"
        const val AVATARS = "avatars"
        const val REPORTS = "reports"
        private const val PROFILE_FIELDS = "id,username,display_name,bio,avatar_url,follower_count,following_count"
        val PROFILE_COLUMNS: Columns = Columns.raw(PROFILE_FIELDS)

        /** The requester's profile is embedded through the requester_id foreign key. */
        val INCOMING_REQUEST_COLUMNS: Columns = Columns.raw("created_at,requester:profiles!follow_requests_requester_id_fkey($PROFILE_FIELDS)")
        private const val MAX_INCOMING_REQUESTS = 100L

        /**
         * User text goes into a PostgREST `or(...)` expression, whose grammar treats , ( ) " as
         * syntax. Keep only letters, digits, space and . _ ' - so input cannot add conditions, and
         * escape LIKE wildcards so what remains matches literally.
         */
        fun sanitizeForFilter(query: String): String =
            query.trim()
                .replace(Regex("[^\\p{L}\\p{N} ._'-]"), "")
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_")
    }
}
