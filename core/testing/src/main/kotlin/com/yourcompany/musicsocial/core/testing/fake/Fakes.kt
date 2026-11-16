package com.yourcompany.musicsocial.core.testing.fake

import com.yourcompany.musicsocial.core.domain.model.ArtistDetail
import com.yourcompany.musicsocial.core.domain.model.ArtistProfile
import com.yourcompany.musicsocial.core.domain.model.AuthState
import com.yourcompany.musicsocial.core.domain.model.ChartGenre
import com.yourcompany.musicsocial.core.domain.model.Page
import com.yourcompany.musicsocial.core.domain.model.PageRequest
import com.yourcompany.musicsocial.core.domain.model.ProfileUpdate
import com.yourcompany.musicsocial.core.domain.model.Song
import com.yourcompany.musicsocial.core.domain.playback.PlaybackController
import com.yourcompany.musicsocial.core.domain.playback.PlaybackState
import kotlinx.coroutines.flow.StateFlow
import com.yourcompany.musicsocial.core.domain.model.SignUpOutcome
import com.yourcompany.musicsocial.core.domain.model.UserProfile
import com.yourcompany.musicsocial.core.domain.repository.AuthRepository
import com.yourcompany.musicsocial.core.domain.repository.MusicRepository
import com.yourcompany.musicsocial.core.domain.repository.UserRepository
import com.yourcompany.musicsocial.core.domain.result.AppError
import com.yourcompany.musicsocial.core.domain.result.AppResult
import com.yourcompany.musicsocial.core.domain.system.BackendAvailability
import com.yourcompany.musicsocial.core.domain.system.NetworkMonitor
import com.yourcompany.musicsocial.core.domain.model.ConversationSummary
import com.yourcompany.musicsocial.core.domain.model.UserSummary
import com.yourcompany.musicsocial.core.domain.model.DeliveryStatus
import com.yourcompany.musicsocial.core.domain.model.FollowRequest
import com.yourcompany.musicsocial.core.domain.model.FollowStatus
import com.yourcompany.musicsocial.core.domain.model.LibraryKind
import com.yourcompany.musicsocial.core.domain.model.Message
import com.yourcompany.musicsocial.core.domain.model.MessageContent
import com.yourcompany.musicsocial.core.domain.model.OutgoingMessage
import com.yourcompany.musicsocial.core.domain.model.Relationship
import com.yourcompany.musicsocial.core.domain.model.ReportReason
import com.yourcompany.musicsocial.core.domain.repository.ChatRepository
import com.yourcompany.musicsocial.core.domain.repository.LibraryRepository
import com.yourcompany.musicsocial.core.domain.repository.PlaylistRepository
import com.yourcompany.musicsocial.core.domain.model.Playlist
import com.yourcompany.musicsocial.core.domain.model.PlaylistDetail
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter

/** Network state that a test can flip with [setOnline]. */
class FakeNetworkMonitor(initiallyOnline: Boolean = true) : NetworkMonitor {
    private val state = MutableStateFlow(initiallyOnline)
    override val isOnline: Flow<Boolean> = state

    fun setOnline(online: Boolean) {
        state.value = online
    }
}

class FakeBackendAvailability(override val isConfigured: Boolean) : BackendAvailability

/**
 * In-memory [AuthRepository] for ViewModel tests. Tests drive [authState] directly with [emit];
 * the sign-in methods record their arguments and return the configured results.
 */
class FakeAuthRepository(initial: AuthState = AuthState.Initializing) : AuthRepository {
    private val state = MutableStateFlow(initial)
    override val authState: Flow<AuthState> = state

    var signInResult: AppResult<Unit> = AppResult.Success(Unit)
    var signUpResult: AppResult<SignUpOutcome> = AppResult.Success(SignUpOutcome.SIGNED_IN)

    val signInAttempts = mutableListOf<Pair<String, String>>()
    val signUpAttempts = mutableListOf<List<String>>()
    var signOutCount = 0
        private set

    fun emit(newState: AuthState) {
        state.value = newState
    }

    override suspend fun signInWithEmail(email: String, password: String): AppResult<Unit> {
        signInAttempts += email to password
        return signInResult
    }

    override suspend fun signUpWithEmail(
        email: String,
        password: String,
        username: String,
        displayName: String,
    ): AppResult<SignUpOutcome> {
        signUpAttempts += listOf(email, password, username, displayName)
        return signUpResult
    }

    override suspend fun signOut(): AppResult<Unit> {
        signOutCount++
        state.value = AuthState.SignedOut
        return AppResult.Success(Unit)
    }
}

/** [MusicRepository] with canned results; records what was searched. Set [failWith] to make calls fail. */
class FakeMusicRepository(var searchResults: List<Song> = emptyList()) : MusicRepository {
    val searchedQueries = mutableListOf<String>()
    var failWith: AppError? = null

    override suspend fun getSong(songId: String): AppResult<Song> =
        failWith?.let { AppResult.Failure(it) }
            ?: searchResults.firstOrNull { it.id == songId }?.let { AppResult.Success(it) }
            ?: AppResult.Failure(AppError.NotFound)

    override suspend fun searchSongs(query: String, page: PageRequest): AppResult<Page<Song>> {
        searchedQueries += query
        return failWith?.let { AppResult.Failure(it) } ?: AppResult.Success(Page(searchResults, nextCursor = null))
    }

    override suspend fun getPreviewUrl(songId: String): AppResult<String> =
        failWith?.let { AppResult.Failure(it) } ?: AppResult.Success("https://audio.example/$songId.m4a")

    var topSongs: List<Song> = emptyList()
    var topSongsRequests = 0
        private set

    override suspend fun getTopSongs(limit: Int): AppResult<List<Song>> {
        topSongsRequests++
        return failWith?.let { AppResult.Failure(it) } ?: AppResult.Success(topSongs.take(limit))
    }

    var genreCharts: Map<ChartGenre, List<Song>> = emptyMap()
    val genreChartRequests = mutableListOf<ChartGenre>()

    override suspend fun getGenreChart(genre: ChartGenre, limit: Int): AppResult<List<Song>> {
        genreChartRequests += genre
        return failWith?.let { AppResult.Failure(it) } ?: AppResult.Success(genreCharts[genre].orEmpty().take(limit))
    }

    var topArtists: List<ArtistProfile> = emptyList()

    override suspend fun getTopArtists(limit: Int): AppResult<List<ArtistProfile>> =
        failWith?.let { AppResult.Failure(it) } ?: AppResult.Success(topArtists.take(limit))

    var artists: Map<String, ArtistDetail> = emptyMap()
    val artistRequests = mutableListOf<String>()

    override suspend fun getArtist(artistId: String): AppResult<ArtistDetail> {
        artistRequests += artistId
        return failWith?.let { AppResult.Failure(it) }
            ?: artists[artistId]?.let { AppResult.Success(it) }
            ?: AppResult.Failure(AppError.NotFound)
    }
}

/**
 * [PlaybackController] that records requests instead of playing. Tests set [state] to simulate the
 * player (for example a song starting) and read the lists to check what the screen asked for.
 */
class FakePlaybackController : PlaybackController {
    private val _state = MutableStateFlow(PlaybackState())
    override val state: StateFlow<PlaybackState> = _state

    private val _recent = MutableStateFlow<List<Song>>(emptyList())
    override val recentlyPlayed: StateFlow<List<Song>> = _recent

    val playRequests = mutableListOf<Pair<List<Song>, Int>>()
    var toggleCount = 0
        private set
    var nextCount = 0
        private set
    var previousCount = 0
        private set
    var shuffleRequests = mutableListOf<Boolean>()
    var repeatCycles = 0
        private set
    var stopCount = 0
        private set

    fun setState(newState: PlaybackState) {
        _state.value = newState
    }

    override fun play(songs: List<Song>, startIndex: Int) {
        playRequests += songs to startIndex
        _recent.value = (listOf(songs[startIndex]) + _recent.value.filterNot { it.id == songs[startIndex].id })
        _state.value = _state.value.copy(song = songs[startIndex], status = PlaybackState.Status.PLAYING)
    }

    override fun togglePlayPause() {
        toggleCount++
    }

    override fun pause() = Unit

    override fun next() {
        nextCount++
    }

    override fun previous() {
        previousCount++
    }

    val seekRequests = mutableListOf<Long>()

    override fun seekTo(positionMs: Long) {
        seekRequests += positionMs
    }

    override fun setShuffle(enabled: Boolean) {
        shuffleRequests += enabled
    }

    override fun cycleRepeatMode() {
        repeatCycles++
    }

    override fun stop() {
        stopCount++
    }
}

/**
 * In-memory [UserRepository]. Profiles live in [profiles]; the signed-in user's id is [meId].
 * Set [failWith] to make every call fail with that error.
 */
class FakeUserRepository(
    val meId: String = "me",
    initialProfiles: List<UserProfile> = emptyList(),
) : UserRepository {
    val profiles: MutableMap<String, UserProfile> = initialProfiles.associateBy { it.id }.toMutableMap()
    val following: MutableSet<String> = mutableSetOf()
    var failWith: AppError? = null
    val updates = mutableListOf<ProfileUpdate>()

    private fun <T> result(block: () -> AppResult<T>): AppResult<T> = failWith?.let { AppResult.Failure(it) } ?: block()

    override suspend fun getProfile(userId: String): AppResult<UserProfile> = result {
        profiles[userId]?.let { AppResult.Success(it) } ?: AppResult.Failure(AppError.NotFound)
    }

    override suspend fun getProfileByUsername(username: String): AppResult<UserProfile> = result {
        profiles.values.firstOrNull { it.username == username }?.let { AppResult.Success(it) }
            ?: AppResult.Failure(AppError.NotFound)
    }

    override suspend fun updateMyProfile(update: ProfileUpdate): AppResult<UserProfile> = result {
        updates += update
        val current = profiles[meId] ?: return@result AppResult.Failure(AppError.NotFound)
        if (update.username != null && profiles.values.any { it.id != meId && it.username == update.username }) {
            return@result AppResult.Failure(AppError.Conflict)
        }
        val updated = current.copy(
            username = update.username ?: current.username,
            displayName = update.displayName ?: current.displayName,
            bio = update.bio ?: current.bio,
            avatarUrl = update.avatarUrl ?: current.avatarUrl,
        )
        profiles[meId] = updated
        AppResult.Success(updated)
    }

    override suspend fun searchProfiles(query: String, page: PageRequest): AppResult<Page<UserProfile>> = result {
        val matches = profiles.values.filter { it.username.contains(query, true) || it.displayName.contains(query, true) }
        AppResult.Success(Page(matches.take(page.limit), nextCursor = null))
    }

    override suspend fun getSuggestedProfiles(limit: Int): AppResult<List<UserProfile>> = result {
        AppResult.Success(profiles.values.filter { it.id != meId }.take(limit))
    }

    override suspend fun getFollowingIds(): AppResult<Set<String>> = result { AppResult.Success(following.toSet()) }

    /** People the signed-in user asked to follow (outgoing, waiting). */
    val requested: MutableSet<String> = mutableSetOf()

    /** People who follow the signed-in user. */
    val followers: MutableSet<String> = mutableSetOf()

    /** Requests waiting for the signed-in user. */
    val incoming: MutableList<FollowRequest> = mutableListOf()

    override suspend fun getRequestedIds(): AppResult<Set<String>> = result { AppResult.Success(requested.toSet()) }

    /** Follower and following lists by user id, for list screens. */
    val followerLists: MutableMap<String, List<UserProfile>> = mutableMapOf()
    val followingLists: MutableMap<String, List<UserProfile>> = mutableMapOf()

    override suspend fun getFollowers(userId: String): AppResult<List<UserProfile>> =
        result { AppResult.Success(followerLists[userId].orEmpty()) }

    override suspend fun getFollowing(userId: String): AppResult<List<UserProfile>> =
        result { AppResult.Success(followingLists[userId].orEmpty()) }

    override suspend fun getRelationship(userId: String): AppResult<Relationship> = result {
        if (userId in blocked) return@result AppResult.Success(Relationship(FollowStatus.NONE, followsMe = false, blockedByMe = true))
        val status = when (userId) {
            in following -> FollowStatus.FOLLOWING
            in requested -> FollowStatus.REQUESTED
            else -> FollowStatus.NONE
        }
        AppResult.Success(Relationship(status, followsMe = userId in followers))
    }

    override suspend fun requestFollow(userId: String): AppResult<Unit> = result {
        requested += userId
        AppResult.Success(Unit)
    }

    override suspend fun cancelFollowRequest(userId: String): AppResult<Unit> = result {
        requested -= userId
        AppResult.Success(Unit)
    }

    override suspend fun unfollow(userId: String): AppResult<Unit> = result {
        following -= userId
        AppResult.Success(Unit)
    }

    override suspend fun getIncomingRequests(): AppResult<List<FollowRequest>> = result { AppResult.Success(incoming.toList()) }

    override suspend fun acceptFollowRequest(requesterId: String): AppResult<Unit> = result {
        if (incoming.removeAll { it.requester.id == requesterId }) followers += requesterId
        AppResult.Success(Unit)
    }

    override suspend fun declineFollowRequest(requesterId: String): AppResult<Unit> = result {
        incoming.removeAll { it.requester.id == requesterId }
        AppResult.Success(Unit)
    }

    val blocked: MutableSet<String> = mutableSetOf()
    val reports = mutableListOf<Triple<String, ReportReason, String?>>()

    /** Like the database: a block ends follows and requests both ways. */
    override suspend fun block(userId: String): AppResult<Unit> = result {
        blocked += userId
        following -= userId
        followers -= userId
        requested -= userId
        incoming.removeAll { it.requester.id == userId }
        AppResult.Success(Unit)
    }

    override suspend fun unblock(userId: String): AppResult<Unit> = result {
        blocked -= userId
        AppResult.Success(Unit)
    }

    override suspend fun getBlockedIds(): AppResult<Set<String>> = result { AppResult.Success(blocked.toSet()) }

    val uploadedAvatars = mutableListOf<ByteArray>()

    override suspend fun uploadAvatar(jpeg: ByteArray): AppResult<UserProfile> = result {
        uploadedAvatars += jpeg
        val current = profiles[meId] ?: return@result AppResult.Failure(AppError.NotFound)
        val updated = current.copy(avatarUrl = "https://storage.example/avatars/$meId/avatar-${uploadedAvatars.size}.jpg")
        profiles[meId] = updated
        AppResult.Success(updated)
    }

    override suspend fun report(userId: String, reason: ReportReason, details: String?): AppResult<Unit> = result {
        reports += Triple(userId, reason, details)
        AppResult.Success(Unit)
    }
}

/** In-memory [LibraryRepository]. [songs] holds every user's lists; the signed-in user is [meId]. */
class FakeLibraryRepository(private val meId: String = "me") : LibraryRepository {
    val songs: MutableMap<Pair<String, LibraryKind>, MutableList<Song>> = mutableMapOf()
    var failWith: AppError? = null
    val requests = mutableListOf<Pair<String, LibraryKind>>()

    private val _mine = MutableStateFlow<Map<LibraryKind, Set<String>>>(emptyMap())
    override val mySongIds: StateFlow<Map<LibraryKind, Set<String>>> = _mine

    override suspend fun refreshMine(): AppResult<Unit> {
        failWith?.let { return AppResult.Failure(it) }
        _mine.value = LibraryKind.entries.associateWith { kind -> songs[meId to kind].orEmpty().map { it.id }.toSet() }
        return AppResult.Success(Unit)
    }

    override suspend fun getSongs(userId: String, kind: LibraryKind, limit: Int): AppResult<List<Song>> {
        requests += userId to kind
        return failWith?.let { AppResult.Failure(it) } ?: AppResult.Success(songs[userId to kind].orEmpty().take(limit))
    }

    override suspend fun add(kind: LibraryKind, song: Song): AppResult<Unit> {
        failWith?.let { return AppResult.Failure(it) }
        val list = songs.getOrPut(meId to kind) { mutableListOf() }
        if (list.none { it.id == song.id }) list.add(0, song)
        return refreshMine()
    }

    override suspend fun remove(kind: LibraryKind, songId: String): AppResult<Unit> {
        failWith?.let { return AppResult.Failure(it) }
        songs[meId to kind]?.removeAll { it.id == songId }
        return refreshMine()
    }
}

/** In-memory [ChatRepository]. Push live messages with [emitLive]. */
class FakeChatRepository(private val meId: String = "me") : ChatRepository {
    var conversations: List<ConversationSummary> = emptyList()
    val messages: MutableMap<String, MutableList<Message>> = mutableMapOf()
    var failWith: AppError? = null

    /** Users a direct chat may be opened with; anyone else gets Forbidden, like the database rule. */
    val allowedPeers: MutableSet<String> = mutableSetOf()
    val sent = mutableListOf<Pair<String, OutgoingMessage>>()
    val markedRead = mutableListOf<String>()
    private val live = MutableSharedFlow<Message>(extraBufferCapacity = 16)

    override suspend fun getConversations(): AppResult<List<ConversationSummary>> =
        failWith?.let { AppResult.Failure(it) } ?: AppResult.Success(conversations)

    override suspend fun openDirectConversation(userId: String): AppResult<String> = when {
        failWith != null -> AppResult.Failure(failWith!!)
        userId !in allowedPeers -> AppResult.Failure(AppError.Auth.Forbidden)
        else -> AppResult.Success("dm-$userId")
    }

    override suspend fun getMessages(conversationId: String, page: PageRequest): AppResult<Page<Message>> =
        failWith?.let { AppResult.Failure(it) }
            ?: AppResult.Success(Page(messages[conversationId].orEmpty().sortedByDescending { it.sentAt }.take(page.limit), nextCursor = null))

    override fun observeNewMessages(conversationId: String): Flow<Message> = live.filter { it.conversationId == conversationId }

    suspend fun emitLive(message: Message) = live.emit(message)

    override suspend fun sendMessage(conversationId: String, message: OutgoingMessage, clientMessageId: String): AppResult<Message> {
        failWith?.let { return AppResult.Failure(it) }
        sent += conversationId to message
        val content = when (message) {
            is OutgoingMessage.Text -> MessageContent.Text(message.body)
            is OutgoingMessage.SongShare -> MessageContent.SharedSong(message.song, message.comment)
            is OutgoingMessage.PlaylistShare -> return AppResult.Failure(AppError.Validation("kind", "unsupported"))
        }
        val stored = Message(
            id = "m-$clientMessageId",
            conversationId = conversationId,
            senderId = meId,
            content = content,
            sentAt = java.time.Instant.parse("2026-11-11T10:00:00Z").plusSeconds(sent.size.toLong()),
            editedAt = null,
            status = DeliveryStatus.SENT,
        )
        messages.getOrPut(conversationId) { mutableListOf() } += stored
        return AppResult.Success(stored)
    }

    override suspend fun markRead(conversationId: String): AppResult<Unit> {
        markedRead += conversationId
        return AppResult.Success(Unit)
    }

    val createdGroups = mutableListOf<Pair<String, List<String>>>()
    val members: MutableMap<String, List<UserSummary>> = mutableMapOf()
    val left = mutableListOf<String>()

    override suspend fun createGroup(title: String, memberIds: List<String>): AppResult<String> {
        failWith?.let { return AppResult.Failure(it) }
        if (title.isBlank() || memberIds.isEmpty()) return AppResult.Failure(AppError.Validation("group", "invalid"))
        createdGroups += title to memberIds
        return AppResult.Success("group-${createdGroups.size}")
    }

    override suspend fun getMembers(conversationId: String): AppResult<List<UserSummary>> =
        failWith?.let { AppResult.Failure(it) } ?: AppResult.Success(members[conversationId].orEmpty())

    override suspend fun leave(conversationId: String): AppResult<Unit> {
        failWith?.let { return AppResult.Failure(it) }
        left += conversationId
        return AppResult.Success(Unit)
    }
}

/** In-memory [PlaylistRepository]. Playlists created here belong to [meId]. */
class FakePlaylistRepository(private val meId: String = "me") : PlaylistRepository {
    val playlists: MutableList<Playlist> = mutableListOf()
    val songs: MutableMap<String, MutableList<Song>> = mutableMapOf()
    var failWith: AppError? = null
    private var nextId = 1

    private fun refreshCount(id: String) {
        val index = playlists.indexOfFirst { it.id == id }
        if (index >= 0) {
            val list = songs[id].orEmpty()
            playlists[index] = playlists[index].copy(songCount = list.size, coverUrl = list.firstNotNullOfOrNull { it.artworkUrl })
        }
    }

    override suspend fun getPlaylists(userId: String): AppResult<List<Playlist>> =
        failWith?.let { AppResult.Failure(it) } ?: AppResult.Success(playlists.filter { it.ownerId == userId })

    override suspend fun getMyPlaylists(): AppResult<List<Playlist>> = getPlaylists(meId)

    override suspend fun getPlaylist(playlistId: String): AppResult<PlaylistDetail> {
        failWith?.let { return AppResult.Failure(it) }
        val playlist = playlists.firstOrNull { it.id == playlistId } ?: return AppResult.Failure(AppError.NotFound)
        return AppResult.Success(PlaylistDetail(playlist, songs[playlistId].orEmpty().toList()))
    }

    override suspend fun createPlaylist(title: String, isPublic: Boolean): AppResult<Playlist> {
        failWith?.let { return AppResult.Failure(it) }
        if (title.isBlank()) return AppResult.Failure(AppError.Validation("title", "blank"))
        val playlist = Playlist("pl-${nextId++}", meId, title.trim(), null, null, isPublic, 0)
        playlists.add(0, playlist)
        return AppResult.Success(playlist)
    }

    override suspend fun deletePlaylist(playlistId: String): AppResult<Unit> {
        failWith?.let { return AppResult.Failure(it) }
        playlists.removeAll { it.id == playlistId }
        songs.remove(playlistId)
        return AppResult.Success(Unit)
    }

    override suspend fun addSong(playlistId: String, song: Song): AppResult<Unit> {
        failWith?.let { return AppResult.Failure(it) }
        val list = songs.getOrPut(playlistId) { mutableListOf() }
        if (list.none { it.id == song.id }) list += song
        refreshCount(playlistId)
        return AppResult.Success(Unit)
    }

    override suspend fun removeSong(playlistId: String, songId: String): AppResult<Unit> {
        failWith?.let { return AppResult.Failure(it) }
        songs[playlistId]?.removeAll { it.id == songId }
        refreshCount(playlistId)
        return AppResult.Success(Unit)
    }
}
