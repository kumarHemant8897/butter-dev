package com.yourcompany.musicsocial.data.chat.datasource

import com.yourcompany.musicsocial.core.common.logging.AppLogger
import com.yourcompany.musicsocial.core.common.logging.LogCategory
import com.yourcompany.musicsocial.core.domain.result.AppResult
import com.yourcompany.musicsocial.core.domain.result.map
import com.yourcompany.musicsocial.core.network.SupabaseClientProvider
import com.yourcompany.musicsocial.core.network.call
import com.yourcompany.musicsocial.core.network.requireUserId
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.UUID
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

internal class SupabaseChatRemoteDataSource @Inject constructor(
    private val supabase: SupabaseClientProvider,
    private val logger: AppLogger,
) : ChatRemoteDataSource {

    override suspend fun getConversations(): AppResult<List<ConversationRowDto>> =
        supabase.call(logger, "chat.conversations") { client ->
            client.postgrest.rpc("my_conversations").decodeList<ConversationRowDto>()
        }

    override suspend fun createDirectConversation(otherUserId: String): AppResult<String> =
        supabase.call(logger, "chat.openDirect") { client ->
            client.postgrest.rpc("create_direct_conversation", buildJsonObject { put("other_user", otherUserId) })
                .decodeAs<String>()
        }

    override suspend fun getMessages(conversationId: String, before: MessageCursor?, limit: Int): AppResult<List<MessageDto>> =
        supabase.call(logger, "chat.messages") { client ->
            client.from(MESSAGES).select(MESSAGE_COLUMNS) {
                filter {
                    eq("conversation_id", conversationId)
                    // Keyset: strictly older than the cursor. created_at ties are broken by id.
                    if (before != null) {
                        or {
                            lt("created_at", before.createdAt)
                            and {
                                eq("created_at", before.createdAt)
                                lt("id", before.id)
                            }
                        }
                    }
                }
                order("created_at", Order.DESCENDING)
                order("id", Order.DESCENDING)
                limit(limit.toLong())
            }.decodeList<MessageDto>()
        }

    override suspend fun getOtherMembersLastReadAt(conversationId: String): AppResult<List<String?>> =
        supabase.call(logger, "chat.readCursors") { client ->
            val me = client.requireUserId()
            client.from(MEMBERS).select(Columns.list("user_id", "last_read_at")) {
                filter {
                    eq("conversation_id", conversationId)
                    neq("user_id", me)
                }
            }.decodeList<ReadCursorRow>().map { it.lastReadAt }
        }

    /**
     * One Realtime channel per open conversation. The channel is removed when the collector stops
     * (the screen closes), so subscriptions never pile up.
     */
    override fun observeInsertedMessages(conversationId: String): Flow<MessageDto> = channelFlow {
        val client = supabase.clientOrNull() ?: return@channelFlow
        // A unique topic per subscription: reopening a chat quickly must not reuse a closing channel.
        val channel = client.channel("messages-$conversationId-${UUID.randomUUID()}")
        val inserts = channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
            table = MESSAGES
            filter("conversation_id", FilterOperator.EQ, conversationId)
        }
        launch {
            inserts.collect { action ->
                try {
                    send(action.decodeRecord<MessageDto>())
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    logger.warn(LogCategory.Network, "chat.realtime: skipped a message that could not be read", e)
                }
            }
        }
        try {
            channel.subscribe()
            awaitCancellation()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Live updates are a bonus over the history the screen already loaded; never crash for them.
            logger.warn(LogCategory.Network, "chat.realtime: subscription failed", e)
            awaitCancellation()
        } finally {
            withContext(NonCancellable) { runCatching { client.realtime.removeChannel(channel) } }
        }
    }

    override suspend fun insertMessage(message: NewMessageDto): AppResult<MessageDto> =
        supabase.call(logger, "chat.send") { client ->
            client.from(MESSAGES).insert(message.copy(senderId = client.requireUserId())) {
                select(MESSAGE_COLUMNS)
            }.decodeSingle<MessageDto>()
        }

    override suspend fun markRead(conversationId: String, upToCreatedAt: String): AppResult<Unit> =
        supabase.call(logger, "chat.markRead") { client ->
            val me = client.requireUserId()
            client.from(MEMBERS).update(buildJsonObject { put("last_read_at", upToCreatedAt) }) {
                filter {
                    eq("conversation_id", conversationId)
                    eq("user_id", me)
                }
            }
        }.map { }

    override suspend fun sendSongMessage(conversationId: String, clientId: String, comment: String?, song: MessageSongDto): AppResult<String> =
        supabase.call(logger, "chat.sendSong") { client ->
            val params = buildJsonObject {
                put("target_conversation", conversationId)
                put("client", clientId)
                put("comment", comment)
                put("song_id", song.songId)
                put("title", song.title)
                put("artist_names", song.artistNames)
                put("album_title", song.albumTitle)
                put("artwork_url", song.artworkUrl)
                put("external_url", song.externalUrl)
                put("duration_ms", song.durationMs)
            }
            client.postgrest.rpc("send_song_message", params).decodeAs<String>()
        }

    override suspend fun getMessage(messageId: String): AppResult<MessageDto?> =
        supabase.call(logger, "chat.message") { client ->
            client.from(MESSAGES).select(MESSAGE_COLUMNS) { filter { eq("id", messageId) } }.decodeSingleOrNull<MessageDto>()
        }

    override fun currentUserId(): String? = supabase.clientOrNull()?.auth?.currentUserOrNull()?.id

    override suspend fun createGroup(title: String, memberIds: List<String>): AppResult<String> =
        supabase.call(logger, "chat.createGroup") { client ->
            val params = buildJsonObject {
                put("group_title", title)
                put("member_ids", JsonArray(memberIds.map { JsonPrimitive(it) }))
            }
            client.postgrest.rpc("create_group_conversation", params).decodeAs<String>()
        }

    override suspend fun getMembers(conversationId: String): AppResult<List<MemberDto>> =
        supabase.call(logger, "chat.members") { client ->
            client.from(MEMBERS).select(Columns.raw("user_id,person:profiles(id,username,display_name,avatar_url)")) {
                filter { eq("conversation_id", conversationId) }
                order("joined_at", Order.ASCENDING)
            }.decodeList<MemberDto>()
        }

    override suspend fun leave(conversationId: String): AppResult<Unit> =
        supabase.call(logger, "chat.leave") { client ->
            val me = client.requireUserId()
            client.from(MEMBERS).delete {
                filter {
                    eq("conversation_id", conversationId)
                    eq("user_id", me)
                }
            }
        }.map { }

    @Serializable
    private data class ReadCursorRow(
        @SerialName("user_id") val userId: String,
        @SerialName("last_read_at") val lastReadAt: String? = null,
    )

    private companion object {
        const val MESSAGES = "messages"
        const val MEMBERS = "conversation_members"
        val MESSAGE_COLUMNS = Columns.raw(
            "id,conversation_id,sender_id,client_id,kind,body,created_at,edited_at,deleted_at," +
                "song:message_songs(song_id,title,artist_names,album_title,artwork_url,external_url,duration_ms)",
        )
    }
}
