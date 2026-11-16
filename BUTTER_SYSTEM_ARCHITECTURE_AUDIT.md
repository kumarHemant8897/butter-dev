# Butter: System Architecture Audit

Audit date: 15 November 2026. Method: read-only review of source, build files, SQL migrations, tests and docs in `c:\Users\hemen\Downloads\Butter`. No code was run, no dependency installed, no configuration changed. The only file written is this report.

**Status legend**
- IMPLEMENTED: code exists and does what the label says.
- PARTIALLY IMPLEMENTED: code exists but is incomplete, or only one layer exists (for example database only).
- NOT IMPLEMENTED: no code found.
- UNVERIFIED: cannot be confirmed from the repository (needs a device, a hosted project, or an external check).

**What was not verified.** I did not build the app, run tests, run it on a device, or touch the hosted Supabase project. Statements about runtime behavior come from reading code, or from `docs/IMPLEMENTATION_STATUS.md`, which is marked as the author's own claim and is partly stale (see section 12).

---

## 1. Executive Summary

Butter is a well-structured, multi-module Android app (Kotlin, Jetpack Compose, MVVM with a clean-architecture layering, Hilt, Supabase). The code quality and the Row Level Security design are better than typical for this stage.

**The most important finding for your next decision: there is no Apple Music integration.** The app uses the free, public **iTunes Search API** (`https://itunes.apple.com`) and Apple's public chart feeds. It has no Apple Music API client, no MusicKit, no Developer Token, no Music User Token, no subscription check and no DRM code. Every song plays as a **30-second preview clip** through ExoPlayer. Full-length playback is **NOT IMPLEMENTED**, and the current architecture cannot reach it without new work (see section 4).

| Area | Overall status |
|---|---|
| Architecture and module layout | IMPLEMENTED, consistent |
| Apple Music API / MusicKit | NOT IMPLEMENTED |
| Preview playback (30 s) + background service | IMPLEMENTED |
| Full-length playback | NOT IMPLEMENTED |
| Auth, profiles, follow requests, library, playlists | IMPLEMENTED (backend tested locally only) |
| 1:1 chat with realtime, song sharing | IMPLEMENTED (uncommitted work) |
| Group chat | PARTIALLY IMPLEMENTED (database only, no client code) |
| Presence, listening rooms, push, in-app notifications | NOT IMPLEMENTED |
| Avatar upload | PARTIALLY IMPLEMENTED (bucket and policies only) |
| Crash reporting, analytics | NOT IMPLEMENTED (no-op seam) |
| Release signing, Play Store readiness | NOT IMPLEMENTED |
| Hosted Supabase project | UNVERIFIED (docs say migrations were never applied there) |

**Source control state:** 2 commits. Migrations 9 and 10, `safety_groups_test.sql`, `ShareSheet.kt`, `FollowListViewModel.kt` are untracked, and about 15 tracked files are modified and uncommitted. The last `build-log.txt` ends in a failing unit test (see section 11).

---

## 2. Existing Technology Stack

Source: `gradle/libs.versions.toml`, `app/build.gradle.kts`, `build-logic/`, `README.md`.

| Layer | Technology | Evidence |
|---|---|---|
| Language | Kotlin 2.4.21 (AGP 9.5.1 built-in Kotlin), SQL (PostgreSQL 17) | `libs.versions.toml`, `backend/supabase/config.toml` (`major_version = 17`) |
| UI | Jetpack Compose (BOM 2026.09.00), Material 3, Navigation Compose 2.10.2 (type-safe routes) | `libs.versions.toml`, `core/navigation/.../Routes.kt` |
| DI | Hilt 2.60.1 with KSP | `build-logic/.../HiltConventionPlugin.kt` |
| Async | Kotlin Coroutines 1.11.0, Flow | `libs.versions.toml` |
| Backend client | `supabase-kt` 3.9.0 (community library): Auth, Postgrest, Realtime, Storage; Ktor 3.6.1 OkHttp engine | `core/network/build.gradle.kts` |
| HTTP | OkHttp 5.5.1, one shared singleton client | `core/network/.../di/NetworkModule.kt` |
| Playback | AndroidX Media3 1.11.1 (ExoPlayer + MediaSession) | `core/playback/build.gradle.kts` |
| Local data | Room 2.8.5 (song cache), Preferences DataStore | `core/database`, `core/datastore` |
| Images | Coil 3.6.3 using the shared OkHttp client | `MusicSocialApplication.kt` |
| Logging | Timber 5.0.1 with a redaction layer | `core/common/.../logging/` |
| Tests | JUnit 4, MockK, Turbine, Compose UI test | `libs.versions.toml` |
| Build | Gradle 9.8.2, convention plugins in `build-logic/`, version catalog | `gradle/wrapper/gradle-wrapper.properties` |
| Unused | Retrofit 3.0.0 is declared but not used | `libs.versions.toml`; `docs/IMPLEMENTATION_STATUS.md` confirms |

SDK levels: `minSdk 26`, `targetSdk 37`, `compileSdk 37`. These version numbers are newer than my reference knowledge. The build log shows the project resolved them, so I treat them as valid but did not independently confirm availability.

**Module map** (`settings.gradle.kts`): 1 app module, 11 `core:*`, 4 `data:*`, 7 `feature:*`.

---

## 3. Complete System Architecture

### 3.1 Pattern: MVVM + Clean Architecture (IMPLEMENTED)

```
app  ── wires everything (Hilt, NavHost, BuildConfig → BackendConfig)
 ├─ feature:*  (screens + ViewModels + UiState; depend on core:domain interfaces only)
 ├─ data:*     (repository impls + remote data sources; Supabase / iTunes / Room)
 └─ core:*     domain (models, repository interfaces, PlaybackController, use cases)
               network, database, datastore, playback, designsystem, ui, common, navigation, testing, sample
```

Evidence: `app/build.gradle.kts` comment "Data modules are wired here and only here: features see repository interfaces, never these". `core/domain` contains only interfaces and pure Kotlin (`repository/*.kt`, `playback/PlaybackController.kt`, `usecase/*`).

### 3.2 Frontend
- Single `MainActivity` (`app/.../MainActivity.kt`), Compose root `ButterApp.kt`.
- `ButterApp` derives an `AppGate` (RESTORING_SESSION, SIGN_IN, MAIN) from `AuthState`. Status: IMPLEMENTED.
- `MusicSocialAppShell` (`navigation/AppNavigation.kt`) holds a Scaffold with a MiniPlayer and a bottom bar with five tabs (Home, Discover, Player, Chats, Profile).
- Each feature module exposes a navigation extension (`homeSection`, `discoverSection`, `chatsSection`, ...). Features do not depend on each other.

### 3.3 State management (IMPLEMENTED)
- ViewModel + `StateFlow<UiState>`, collected with `collectAsStateWithLifecycle`. Example: `ConversationViewModel`, `ChatListViewModel`.
- Repositories hold in-memory singletons for shared state: `LibraryRepositoryImpl.mySongIds`, `ExoPlaybackController.state`.
- Optimistic updates with rollback: `LibraryRepositoryImpl.add/remove`, `ConversationViewModel.onSend`.

### 3.4 Navigation (IMPLEMENTED)
Type-safe routes in `core/navigation/.../Routes.kt` (for example `ConversationRoute`, `UserProfileRoute`, `SongListRoute`, `PeopleSearchRoute`). Fade transitions only. No deep links were found (UNVERIFIED for the manifest: only a MAIN/LAUNCHER intent filter exists in `AndroidManifest.xml`).

### 3.5 Backend integration
- `core/network/SupabaseClientProvider.kt`: lazy singleton client, returns `null` when not configured. Callers map `null` to `AppError.BackendNotConfigured`.
- `core/network/RemoteCall.kt` `SupabaseClientProvider.call(...)`: one wrapper that converts exceptions to `AppResult.Failure(AppError)`, rethrows cancellation, logs once. IMPLEMENTED, good practice.
- Data sources: `SupabaseAuthRemoteDataSource`, `SupabaseUserRemoteDataSource`, `SupabaseChatRemoteDataSource`, `SupabaseLibraryRemoteDataSource`, plus `PlaylistRemoteDataSource`.
- Catalogue source: `ItunesMusicRemoteDataSource` (REST via OkHttp, not Supabase).
- There is no custom backend server and no Edge Function. The app talks to PostgREST, GoTrue, Realtime and Storage directly, with Row Level Security as the only server-side authorization.

### 3.6 Dependency injection (IMPLEMENTED)
Hilt. Modules: `AppModule` (BackendConfig from BuildConfig), `NetworkModule`, `CommonModule` (logger, crash reporter, dispatchers, `Clock`), `PlaybackModule`, `*DataModule`.

### 3.7 Other architecture notes
- `core:sample` still exists ("TESTING ONLY") and feeds Discover feelings, plus sample profile/people when the build has no backend (`ButterApp.kt` comment, `docs/IMPLEMENTATION_STATUS.md`). PARTIALLY IMPLEMENTED content.
- Room: `fallbackToDestructiveMigration` is on (stated in `docs/IMPLEMENTATION_STATUS.md`; not re-verified in `MusicSocialDatabase.kt`). Acceptable while Room is cache-only.

---

## 4. Apple Music Integration Analysis (highest priority)

### 4.1 Direct answers

| Question | Answer | Status | Evidence |
|---|---|---|---|
| Is Apple Music API integrated? | No | NOT IMPLEMENTED | `grep` for `musickit`, `developer token`, `music user token`, `amp-api`, `api.music.apple`, `widevine`, `DefaultDrm` returned zero matches in the whole repo (excluding build output). |
| Is MusicKit for Android integrated? | No | NOT IMPLEMENTED | No MusicKit AAR in `libs.versions.toml`, no `musicsdk://` handling, no auth intent code. |
| What *is* integrated? | iTunes Search/Lookup API + Apple RSS chart feeds, India storefront | IMPLEMENTED | `data/music/.../ItunesMusicRemoteDataSource.kt`: `BASE = https://itunes.apple.com`, `CHART_BASE = https://rss.marketingtools.apple.com`, `STOREFRONT = "IN"`. |
| Authentication? | None. Requests are anonymous. | NOT IMPLEMENTED | `fetch()` sends only `Accept: application/json`. |
| Music User Token? | No | NOT IMPLEMENTED | No match anywhere. |
| Developer Token (JWT) generated/managed? | No | NOT IMPLEMENTED | No ES256 signing, no MusicKit private key, no token endpoint. The iTunes Search API does not need one. |
| Subscription authorization checked? | No | NOT IMPLEMENTED | No call to a storefront or subscription endpoint. |
| How are songs searched? | `GET /search?term=…&media=music&entity=song&country=IN&limit=N`; "offset" is faked by requesting `offset+limit` rows and dropping the start | IMPLEMENTED | `ItunesMusicRemoteDataSource.searchSongs`. |
| Where does the 30-second preview come from? | The `previewUrl` field of the iTunes response, kept only if HTTPS on `*.apple.com` | IMPLEMENTED | `ItunesTrack.toSongDto`, `trustedHttpsUrl("apple.com")`. |
| Full-length playback? | No | NOT IMPLEMENTED | `ExoPlaybackController.loadCurrent()` only ever plays `song.previewUrl` or the result of `musicRepository.getPreviewUrl()`. `PlaybackState.durationMs` documents "a 30-second preview is 30000, not the song's full length". |
| Actual audio playback engine? | Yes, ExoPlayer, for preview URLs | IMPLEMENTED | `core/playback/.../ExoPlaybackController.kt`. |
| Uses Apple's authorized playback? | No. It streams Apple's public preview MP4/M4A files directly. | NOT IMPLEMENTED | No MusicKit player, no DRM configuration on `MediaItem`. |
| Android restrictions? | See 4.3 | UNVERIFIED (external) | Needs confirmation against Apple documentation. |
| Can users hear complete songs in Butter? | **No.** Only 30-second clips. | NOT IMPLEMENTED | As above. |
| Requires an Apple Music subscription? | **No.** The current feature set works with no Apple account at all. | n/a | No auth path exists. |

### 4.2 How the current catalogue flow works (all IMPLEMENTED)

1. **Search**: `DiscoverViewModel` waits 450 ms after typing (`docs/IMPLEMENTATION_STATUS.md`), calls `MusicRepository.searchSongs` → `MusicRepositoryImpl.searchSongs` (minimum 2 characters, pagination via faked offset) → `ItunesMusicRemoteDataSource.searchSongs`.
2. **Charts**: `getTopSongs` first fetches Apple's marketing "most-played" feed for ids (4 s timeout), falls back to the legacy `rss/topsongs` feed (8 s), then performs one batch `/lookup?id=…` to get full details and preview URLs. A 30-minute in-memory cache and a `Mutex` stop concurrent callers from repeating the download (`MusicRepositoryImpl.getTopSongs`).
3. **Genre chart**: only Bollywood (`genre 1263`) via the legacy feed (`ChartGenre.BOLLYWOOD`).
4. **Security of responses**: every URL is validated for HTTPS and host suffix before use (`trustedHttpsUrl`). Song ids must match `^itunes:\d{1,20}$` before a lookup (`parseTrackId`). IMPLEMENTED, good.
5. **Preview URL handling**: `MusicRepositoryImpl.toEntity` and library/chat mappers set `previewUrl = null` so preview URLs are never persisted (`SongMappers.kt` line 42, `SongRows.kt`, `MessageMappers.kt`). A song loaded from a library, playlist or chat message therefore needs an extra `/lookup` call at play time (`ExoPlaybackController.loadCurrent` → `musicRepository.getPreviewUrl`). This is deliberate but has rate-limit cost (section 10).

### 4.3 Why searching Apple's catalogue does not give full playback

- The iTunes Search API is a marketing/affiliate API. It returns metadata plus a short preview clip. It does not return full-length audio.
- Full-length Apple Music playback is DRM-protected and tied to a signed-in subscriber. Apple offers this through MusicKit. A Music User Token alone does not give you audio files; playback has to go through Apple's player.
- I found Apple's **MusicKit for Android** SDK documentation online ([developer.apple.com/musickit/android](https://developer.apple.com/musickit/android/com/apple/android/sdk/authentication/TokenResult.html)). It exposes an authentication flow that returns a Music User Token. Third-party reports describe authentication that hands off to the installed Apple Music app via a deep link ([Stack Overflow](https://stackoverflow.com/questions/72824625/apple-musickit-sdk-in-android)) and recurring token problems ([Apple Developer Forums](https://developer.apple.com/forums/thread/838633)).
- I could **not** verify from those pages: whether the Android SDK includes a playback engine usable inside a third-party app, current maintenance state, the Apple Music app dependency, pricing, or license terms for apps that also stream previews. Treat all of this as **UNVERIFIED** and confirm in Apple's current documentation and the Apple Developer Program terms before committing to a design.
- Content was rephrased from the sources above for compliance with licensing restrictions.

### 4.4 Classification

| Capability | Class |
|---|---|
| Catalogue search, charts, artist lookup (iTunes API) | **1. Implemented** |
| 30-second preview playback, queue, background service | **1. Implemented** |
| "Open in Apple Music" link and iTunes credit | **1. Implemented** (documented in `docs/IMPLEMENTATION_STATUS.md`; UI strings not re-read) |
| Apple Developer Token, Music User Token, subscription check | **3. Missing** |
| Full-length playback of Apple Music tracks | **3. Missing** |
| Apple Music playback inside ExoPlayer using raw preview or catalogue URLs | **4. Technically unsupported.** There is no full-audio URL to give ExoPlayer, and Apple's audio is DRM-protected. Do not plan on this. |
| Any use of unofficial or scraped streams to get full audio | **4. Unsupported and not permitted.** Do not build this. |
| Android MusicKit SDK suitability for in-app playback | **UNVERIFIED** |

### 4.5 Terms and policy risks in the existing preview feature
- `docs/IMPLEMENTATION_STATUS.md` itself flags that auto-advancing through 25 previews may cross Apple's terms (previews are meant to promote a song). The queue does auto-advance: `ExoPlaybackController.onEnded()` calls `q.next(auto = true)`. Status: policy risk, unresolved.
- The same document says the iTunes API is limited to about 20 requests per minute per IP and has no SLA. UNVERIFIED externally, but consistent with the code's 403/429 handling (`fetch`: 403 and 429 map to `AppError.Server(429)`).
- Apple credits: the document states the UI shows a link to Apple Music and a "provided courtesy of iTunes" credit. I did not read the UI strings, so this is UNVERIFIED in code.

---

## 5. Music Playback Architecture

| Item | Status | File / class | Evidence |
|---|---|---|---|
| Audio engine | IMPLEMENTED | `core/playback/.../ExoPlaybackController.kt` | Builds `ExoPlayer` with `AudioAttributes(USAGE_MEDIA, CONTENT_TYPE_MUSIC)`, `handleAudioFocus = true`, `setHandleAudioBecomingNoisy(true)`. |
| Controller abstraction | IMPLEMENTED | `core/domain/.../PlaybackController.kt` | Interface with `state`, `recentlyPlayed`, `play/togglePlayPause/pause/next/previous/seekTo/setShuffle/cycleRepeatMode/stop`. UI never touches ExoPlayer. |
| Queue, shuffle, repeat | IMPLEMENTED | `core/domain/.../PlaybackQueue.kt` + `PlaybackQueueTest.kt` | Pure Kotlin, shuffle keeps current song first, repeat OFF/ALL/ONE, tested. |
| Play / pause / seek | IMPLEMENTED | `ExoPlaybackController` | `exo.play()/pause()/seekTo()`; 250 ms progress ticker (`TICK_MS`). |
| Background playback | IMPLEMENTED | `PlaybackService.kt` (`MediaSessionService`), `core/playback/AndroidManifest.xml` | `foregroundServiceType="mediaPlayback"`, permissions `FOREGROUND_SERVICE` and `FOREGROUND_SERVICE_MEDIA_PLAYBACK`. `onTaskRemoved` stops the service if not playing. |
| Notification / lock-screen controls | IMPLEMENTED (device-verified per author only) | `PlaybackService.addSession(...)` | Next/previous routed to the app queue through `sessionPlayer()` (`ForwardingPlayer`). The author reports it was verified on a moto g34 with the service in the foreground 75 s after pressing Home. UNVERIFIED by me. |
| Audio session / focus | IMPLEMENTED | `ExoPlaybackController.player()` | Focus handled by ExoPlayer. No custom ducking policy. |
| Playback state sync | IMPLEMENTED | `ExoPlaybackController.syncStatus/updateProgress`, `PlaybackState` | Single `StateFlow`. Mini player, Player tab and chat bubbles all observe it. |
| Error handling | PARTIALLY IMPLEMENTED | `Player.Listener.onPlayerError` | Only two codes are mapped (`IO_NETWORK_CONNECTION_FAILED`, `..._TIMEOUT`); everything else becomes `AppError.Unknown`. There is no automatic retry. A failed load leaves status PAUSED with `error` set; pressing play retries `loadCurrent()`. |
| Preview vs full-track | Preview only | `loadCurrent()` | See section 4. |
| Disk cache of audio | Not configured, by design | `ExoPlaybackController` KDoc | "no disk cache is configured". |
| Media3 `MediaController` client | NOT IMPLEMENTED | n/a | The app starts the service directly and shares one in-process `ExoPlayer`. This works but means the session is bound to the app process. |
| Android Auto / Wear | NOT IMPLEMENTED | n/a | Manifest comment mentions Android Auto but there is no `MediaLibraryService` or automotive descriptor. |

**Design issues worth knowing**
1. `ExoPlaybackController` plays **one `MediaItem` at a time**, and the queue lives in `PlaybackQueue`. There is no gapless transition and each song start has a resolve step. Fine for 30-second clips; not suitable for full-length songs with gapless or crossfade.
2. `ensureServiceStarted()` calls `context.startService(...)` from a coroutine. On Android 12+ background-start limits apply; the code catches `IllegalStateException` and degrades to playback without system controls (comment in code). Acceptable.
3. `play(...)` is dispatched with `scope.launch` on the main dispatcher, so rapid taps can interleave, but `loadJob?.cancel()` bounds that.
4. `recentlyPlayed` is in memory only (not saved across restarts) per the interface doc.
5. A "play" for a library, playlist or chat song needs an iTunes `/lookup` first (`previewUrl == null`). The 4 s / 8 s chart timeouts do not apply here; it uses the default 15/30/30 s OkHttp timeouts.

---

## 6. Chat and Social Architecture

| Feature | Status | Evidence |
|---|---|---|
| Profiles (view, edit name/username/bio) | IMPLEMENTED | `SupabaseUserRemoteDataSource.getProfile/updateMyProfile`, `EditProfileViewModel`, table `profiles` (migration 2). |
| Avatar upload | PARTIALLY IMPLEMENTED | Bucket `avatars` and policies exist (migration 10). No code calls Storage upload (no `.upload(` match in any `.kt`). `updateMyProfile` can set `avatar_url` but no UI picks an image. |
| Follow requests (follow = request, target accepts) | IMPLEMENTED | Migration 7 `follow_requests`, RPC `accept_follow_request`; client `insertFollowRequest`, `acceptFollowRequest`. Clients can no longer insert into `follows`. |
| Followers / following lists | PARTIALLY IMPLEMENTED | `getFollowers/getFollowing` exist in the data source; `FollowListViewModel.kt` is **untracked** and I did not confirm it is wired in navigation. |
| People search / suggestions | IMPLEMENTED | `searchProfiles` (ILIKE with input sanitised against PostgREST filter grammar, `sanitizeForFilter`). Suggestions are just "newest members". |
| Block / report | PARTIALLY IMPLEMENTED | Migration 10 tables and policies; `insertBlock/deleteBlock/getBlockedIds/insertReport` in the data source; dialogs in `UserProfileScreen.kt`. Uncommitted. No moderator tool or back office (reports are readable only with the service role). |
| Library (liked / saved songs) | IMPLEMENTED | Migration 7 `library_songs`, `LibraryRepositoryImpl`. |
| Playlists (create, add, remove, delete, visibility) | IMPLEMENTED | Migration 8, `PlaylistRepositoryImpl`. Rename and reorder are NOT IMPLEMENTED (per docs). |
| Private 1:1 messaging (text) | IMPLEMENTED | `messages` table, `insertMessage`, optimistic send with client id, retry on failure (`ConversationViewModel`). |
| Realtime message delivery | IMPLEMENTED | `SupabaseChatRemoteDataSource.observeInsertedMessages` uses `postgresChangeFlow<Insert>` filtered by `conversation_id`, one channel per open conversation, removed on cancel. Safety net: the screen also polls every **5 s** (`ConversationScreen.kt` `REFRESH_EVERY_MS = 5_000L`). |
| Read receipts | PARTIALLY IMPLEMENTED | Read cursor `last_read_at` per member. The app writes the **device clock** (`ChatRepositoryImpl.markRead` → `clock.instant()`), while unread counts compare it against server `created_at`. Clock skew makes counts wrong. |
| Song sharing in chat | IMPLEMENTED (uncommitted) | Migration 9 `message_songs`, RPC `send_song_message` (idempotent), `ShareSheet.kt` (untracked), `ChatRepositoryImpl.sendMessage(SongShare)`. Tapping a shared song plays a 30 s preview. |
| Playlist sharing in chat | NOT IMPLEMENTED | `ChatRepositoryImpl.sendMessage` returns `Validation("kind","unsupported")` for non-text/non-song. The `kind` check allows `'playlist'` but the table that held targets was dropped (migration 9). |
| Group messaging | PARTIALLY IMPLEMENTED | RPC `create_group_conversation` (1 to 49 connected people), `my_conversations()` returns groups, Chat list has a Groups filter and group avatar shape. There is **no client method** to create or manage a group (`ChatRepository` has no such function) and no member management. Group chat is effectively a database feature only. |
| Chat list | IMPLEMENTED | RPC `my_conversations()` (SECURITY INVOKER), polled every **8 s** (`ChatListScreen.kt`). |
| Message edit / delete | PARTIALLY IMPLEMENTED | Update is allowed on `body, edited_at, deleted_at` by the sender (migration 5). No client UI or data source method was found. |
| User presence / online dot | NOT IMPLEMENTED | `ConversationItem.isOnline` defaults to `false` and nothing sets it. No Realtime presence channel. UI placeholder only. |
| Typing indicators | NOT IMPLEMENTED | No match. |
| Listening rooms / shared listening | NOT IMPLEMENTED | No code, table or UI. Docs: "returns in Phase 4". |
| Push notifications | NOT IMPLEMENTED | No FCM/Firebase dependency, no `POST_NOTIFICATIONS` permission, no service. |
| In-app notifications | NOT IMPLEMENTED | `ic_notifications` icon and a `CircleIconButton(badge)` component exist; no handler or `notifications` table (`docs/IMPLEMENTATION_STATUS.md`). UI placeholder only. |
| Activity feed ("friends listening") | NOT IMPLEMENTED | Removed from Home per docs. |

---

## 7. Backend and Database Architecture

### 7.1 Technology
Supabase (PostgreSQL 17, GoTrue Auth, PostgREST, Realtime, Storage). No Edge Functions. No custom API server. Local stack configured in `backend/supabase/config.toml`.

### 7.2 Authentication
- Email and password only (`SupabaseAuthRemoteDataSource.signInWithEmail/signUpWithEmail`). IMPLEMENTED.
- Session persistence: Supabase SDK default (SharedPreferences, per `AuthRemoteDataSource` KDoc). `allowBackup=false` and `data_extraction_rules.xml` exclude everything from backup and transfer. IMPLEMENTED.
- Profile creation: DB trigger `handle_new_user()` on `auth.users`, with a safe fallback username. IMPLEMENTED.
- NOT IMPLEMENTED: password reset, social login, email-confirmation handling beyond a message, account deletion, MFA.
- Local `config.toml` sets `enable_confirmations = false`, `minimum_password_length = 8`. Hosted defaults differ (6) per `docs/IMPLEMENTATION_STATUS.md`. UNVERIFIED for the hosted project.

### 7.3 Tables (public schema) and relationships

| Table | Key columns / relationships | Migration | Used by the app? |
|---|---|---|---|
| `profiles` | `id → auth.users`, unique citext `username`, counters | 2 | Yes |
| `follows` | `(follower_id, followee_id)` → profiles | 2 | Yes (insert only via RPC) |
| `follow_requests` | `(requester_id, target_id)` → profiles | 7 | Yes |
| `blocks` | `(blocker_id, blocked_id)` | 10 | Yes |
| `reports` | reporter, target, optional `message_id`, `reason` enum check | 10 | Insert only |
| `artists`, `songs`, `song_artists` | uuid catalogue, `catalog_provider/external_id`, `isrc` | 3 | **No.** The app never reads them (songs come from iTunes). Only `seed.sql` fills them. Dead schema. |
| `user_likes` | `(user_id, song_id uuid → songs)` | 4 | **No.** Superseded by `library_songs`. Dead schema. |
| `library_songs` | `(user_id, kind, song_id text 'itunes:123')` + display fields | 7 | Yes |
| `playlists` | owner, `is_public`, `song_count`, `cover_url` | 4, 8 | Yes |
| `playlist_songs` | `(playlist_id, song_id text)` + display fields (rebuilt in 8) | 8 | Yes |
| `conversations` | `type direct/group`, `direct_key` unique | 5 | Yes |
| `conversation_members` | role, `last_read_at`, `last_delivered_at` | 5 | Yes |
| `messages` | `client_id` unique per sender, `kind text/song/playlist`, soft delete | 5 | Yes |
| `message_songs` | song display data per song message (replaces `message_shares`) | 9 | Yes |

### 7.4 RPCs (API endpoints beyond table REST)
`accept_follow_request`, `create_direct_conversation`, `create_group_conversation`, `my_conversations`, `send_song_message`, and helper functions `is_conversation_member`, `can_view_playlist`, `is_blocked_between`, `is_direct_blocked`. Trigger functions have `EXECUTE` revoked from API roles (migration 6, 8, 10). `max_rows = 200` in `config.toml`.

### 7.5 Realtime
Tables `messages` and `conversation_members` are added to the `supabase_realtime` publication (migration 5). Only `messages` inserts are subscribed to by the client (`observeInsertedMessages`). `conversation_members` changes (read cursors) are published but not consumed. No broadcast or presence channels.

### 7.6 Storage
Bucket `avatars` (public, 2 MB, jpeg/png/webp) with per-user folder policies (migration 10). Not used by the client yet.

### 7.7 Row Level Security: summary
RLS is enabled on every table above. Policies use `(select auth.uid())` (good for planner caching). Highlights:
- `profiles`: readable by all signed-in users; update limited to `username, display_name, bio, avatar_url` columns (column-level grant).
- `library_songs`, `playlists`, `playlist_songs`: owner, or approved follower when public.
- `messages`: members read; members send as themselves and not when a direct chat is blocked.
- `reports`: insert only.
- Counters (`follower_count`, `song_count`) are trigger-maintained and not writable by clients.

### 7.8 Environment variables / configuration
Names only, never values. `local.properties` (git-ignored) contains `sdk.dir`, `SUPABASE_URL`, `SUPABASE_ANON_KEY`. `local.properties.example` also lists `PROD_SUPABASE_URL`, `PROD_SUPABASE_ANON_KEY`. The same names can come from CI environment variables (`app/build.gradle.kts` `secret()`). Values are injected into `BuildConfig` per flavor (`dev`, `prod`). The build **rejects** keys that look like `sb_secret_*` or JWTs with `role: service_role` (`requirePublishableKey`). IMPLEMENTED, a good guard.

---

## 8. Third-Party APIs

| Service | Purpose | Where | Functional? | Cost / usage notes |
|---|---|---|---|---|
| iTunes Search / Lookup API (`itunes.apple.com`) | Song search, lookup, artist data, preview URLs | `ItunesMusicRemoteDataSource` | Yes, per author's device test | Free, no SLA. About 20 requests per minute per IP according to the code comments and docs (UNVERIFIED externally). Terms restrict previews to promotion. |
| Apple marketing chart feed (`rss.marketingtools.apple.com`) | India "most played" chart ids | `getTopSongs` | Yes, with a fallback | Undocumented stability; the code notes it "hangs often". |
| Apple legacy iTunes RSS charts (`itunes.apple.com/in/rss/...`) | Fallback chart and genre charts | `getTopSongs`, `getGenreChart` | Yes | Legacy feed; long-term availability UNVERIFIED. |
| Apple image CDN (`*.mzstatic.com`) | Artwork | `artworkUrl.trustedHttpsUrl("mzstatic.com")`, Coil | Yes | Free. |
| Supabase (hosted or local) | Auth, DB, Realtime, Storage | `core/network`, `data/*` | Local: yes. **Hosted: UNVERIFIED** (docs say the hosted `butter-dev` project has none of the migrations applied and MCP was never authenticated). | Pricing depends on plan; UNVERIFIED. Realtime connection and message limits depend on the plan. |
| `supabase-kt` (community client) | Kotlin SDK for Supabase | `libs.versions.toml` | Yes | Open source; pin and review upgrades (noted in docs). |
| Firebase / Crashlytics / Sentry | Crash reporting | n/a | **NOT IMPLEMENTED** (`NoOpCrashReporter` bound) | n/a |
| FCM | Push | n/a | NOT IMPLEMENTED | n/a |
| Analytics | n/a | n/a | NOT IMPLEMENTED | n/a |

Fonts and icons: Inter (OFL) and Lucide (ISC) are bundled with licenses in `docs/licenses/`.

---

## 9. Security Assessment

### 9.1 Secrets and source control
- **No secrets found in tracked files.** A regex scan of tracked files (`sb_secret_`, `service_role`, JWT-shaped strings, `AIza…`, private key headers) matched only comments, docs and the build guard. IMPLEMENTED.
- `local.properties`, keystores, `.env*`, `google-services.json`, `dist/` APKs and `*-log.txt` are in `.gitignore`. `local.properties` is not tracked (`git ls-files` shows only `local.properties.example`).
- I printed only the *key names* inside `local.properties`, never values. The file holds a Supabase URL and anon key that point at a local stack per the author's notes. I did not check history of the 2 commits for earlier leaks beyond the pattern scan on tracked files (UNVERIFIED for full history; run a history scan such as `gitleaks` before making the repo public).
- `build-log.txt` exists in the working directory. It is git-ignored; I did not inspect it for secrets beyond the tail.

### 9.2 Findings

| # | Finding | Severity | Status | Evidence |
|---|---|---|---|---|
| S1 | Anon key is in the APK by design. Safe only if RLS is right everywhere. RLS looks consistent in migrations 2 to 10, and the repo has SQL tests (`rls_smoke_test.sql`, `social_rules_test.sql`, `playlists_test.sql`, `safety_groups_test.sql`). | Info | IMPLEMENTED | `BackendConfig` KDoc; tests run by author, not by me |
| S2 | **Debug builds allow cleartext HTTP to any host**; release builds do not. | Medium if a debug build is ever distributed | IMPLEMENTED correctly for release | `app/src/debug/res/xml/network_security_config.xml` vs `app/src/main/res/xml/network_security_config.xml` |
| S3 | `profiles` are readable by every signed-in user, and `follows` rows are readable by every signed-in user (`using (true)`). Anyone signed in can enumerate users and the entire follow graph. The "every account approves followers" rule protects *libraries and playlists*, not the graph. | Medium (privacy) | PARTIALLY IMPLEMENTED | Migration 2 policies `profiles are visible to signed-in users`, `follows are visible to signed-in users`; the author lists this as a limitation |
| S4 | `profiles_avatar_url_format` allows `http://` URLs (`'^https?://'`) while the comment says https or this project's storage. | Low | IMPLEMENTED with a gap | Migration 10 |
| S5 | No server-side rate limiting on sign-up, message sending, follow requests, reports or blocks. | High before launch | NOT IMPLEMENTED | `backend/supabase/README.md` "Before launch checklist" lists it as to-do |
| S6 | Messages are stored in plaintext; operators with database access can read all chats. No end-to-end encryption. | Info (product decision) | n/a | `messages.body` text |
| S7 | `markRead` uses the client clock. A user can set any `last_read_at`, including a future one, which hides unread counts. Low impact, only affects that user. | Low | IMPLEMENTED with a gap | `ChatRepositoryImpl.markRead` |
| S8 | A removed or blocked member of a **group** can still send. `is_direct_blocked` only covers direct chats, and there is no member-removal flow. | Low now (no group UI) | PARTIALLY IMPLEMENTED | Migration 10 comment "Group chats are not affected" |
| S9 | Report abuse: any user can insert unlimited reports against one target (no uniqueness or throttle). | Medium | NOT IMPLEMENTED | `reports` policy has no limit |
| S10 | Realtime `postgres_changes` is RLS-filtered, but the filter used is only `conversation_id`. Authorization relies on RLS on `messages` (membership). Correct, but see the scaling note in section 10. | Info | IMPLEMENTED | `observeInsertedMessages` |
| S11 | Log redaction removes JWTs, bearer tokens, `password/token/...=` pairs and emails. HTTP logging is `Level.BASIC` and enabled only in debug (`enableHttpLogging = BuildConfig.DEBUG`). | Good | IMPLEMENTED | `LogRedactor.kt`, `SupabaseClientProvider.supabaseHttpClient` |
| S12 | Exported `PlaybackService` (`android:exported="true"`). Media3 only accepts controllers allowed in `onConnect`. Default `onConnect` accepts all controllers; no custom `MediaSession.Callback` was found. So any app on the device could connect and control playback. | Low to Medium | PARTIALLY IMPLEMENTED | `core/playback/AndroidManifest.xml`; `PlaybackService.kt` builds the session without a callback. The Media3 default behavior is from my knowledge, not verified here. |
| S13 | R8 release rules: minimal `proguard-rules.pro`; the author last built `assembleProdRelease` at the end of Phase 1 and **not after Phase 2 or 3** (`docs/IMPLEMENTATION_STATUS.md`). Release behavior with Media3, Supabase and serialization is UNVERIFIED. | Medium | UNVERIFIED | `docs/IMPLEMENTATION_STATUS.md` |
| S14 | Untrusted external content: all iTunes URLs are validated for scheme and host; user strings placed in PostgREST `or(...)` filters are sanitised. | Good | IMPLEMENTED | `trustedHttpsUrl`, `sanitizeForFilter` |
| S15 | `SECURITY DEFINER` functions all set `search_path = ''` and revoke `EXECUTE` from `public, anon`. | Good | IMPLEMENTED | Migrations 5 to 10 |
| S16 | Supabase advisor "3 intentional warnings" reported by the author for migrations 1 to 6. Advisors were **not re-run** after migrations 7 to 10 (`docs/IMPLEMENTATION_STATUS.md` is stale on that). | Medium | UNVERIFIED | `docs/IMPLEMENTATION_STATUS.md` |

---

## 10. Performance and Scalability

Nothing was load tested. The statements below are reasoning from code, not measurements. I make no claim of capacity.

### 10.1 Bottlenecks found

| Area | Finding | Evidence |
|---|---|---|
| Catalogue (biggest risk) | The catalogue API is called **from each phone** and is rate limited per IP (about 20 requests per minute per the code comments). That scales with the number of distinct IPs, not with your server. On shared networks (offices, mobile carrier NAT), many users share one IP and will hit 403/429. There is no server-side proxy, cache or retry/backoff (only a user-facing error). | `ItunesMusicRemoteDataSource` KDoc; `fetch` maps 403/429 to `Server(429)` |
| Play start | Every play of a library, playlist or chat song costs one extra `/lookup` request because previews are never stored. Playing a 25-song list one by one costs up to 25 lookups. | `ExoPlaybackController.loadCurrent`; mappers set `previewUrl = null` |
| Chat polling | Open conversation calls `getMessages` every **5 s**; chat list calls `my_conversations()` every **8 s**; a pending follow request is re-checked every 6 s. Each message fetch also reads other members' read cursors (2 queries per refresh: `getOtherMembersLastReadAt`, `getMessages`). Realtime is already present, so polling duplicates traffic. | `ConversationScreen.kt`, `ChatListScreen.kt`, `UserProfileScreen.kt`, `ChatRepositoryImpl.getMessages` |
| `my_conversations()` | Per conversation it runs two lateral subqueries plus a correlated `count(*)` for unread over `messages`, executed every 8 s per active user. Index `messages_history_idx (conversation_id, created_at desc, id desc)` helps the latest-message lookup; the unread count scans messages newer than the cursor. Unread counts grow with long unread backlogs. | Migration 10 `my_conversations()` |
| Realtime | `postgres_changes` with RLS performs an authorization check per subscriber per change. RLS here calls `is_conversation_member()`, one indexed lookup. This is a known scaling concern in Supabase's own documentation for high fan-out, and I did not measure it. | Migration 5 (publication), `SupabaseChatRemoteDataSource` |
| Realtime connection count | One Realtime channel per open conversation, one WebSocket per client (SDK). Concurrent connection limits depend on the Supabase plan. UNVERIFIED. | `observeInsertedMessages` |
| Profile search | `ILIKE '%term%'` on `username` and `display_name`. Trigram GIN indexes exist (`profiles_username_trgm`, `profiles_display_name_trgm`), which fit `ILIKE` with `%`. Keyset pagination by `username`. | Migration 2, `searchProfiles` |
| Follow counters | Trigger-maintained counters avoid `COUNT(*)` on read. Contention on hot rows (a very popular profile) is possible at high follow rates. | Migration 2 `update_follow_counts` |
| Room / memory | Song metadata is cached for 24 h; charts for 30 min in memory. Fine. | `MusicRepositoryImpl` |
| Chat history | Latest 50 messages only; **older-message paging is NOT IMPLEMENTED** in the client (`PAGE_SIZE = 50`, `ChatRepository` supports cursors but the VM loads one page). | `ConversationViewModel.load` |

### 10.2 Capacity assessment (reasoned, not measured)

| Users | Assessment |
|---|---|
| **100** | Plausible on a single Supabase project. The main risk is the iTunes per-IP limit if many testers share one network, and the polling load, which is small at this size. No evidence from a test either way. |
| **1,000** | Polling alone would issue on the order of several requests per second at peak concurrency (for example 300 concurrent users in a chat each polling every 5 s is about 60 req/s plus list refresh). That is a guess, not a measurement. Supabase plan limits (connections, Realtime messages, egress) are UNVERIFIED. Catalogue limits will bite users on shared networks. Needs load testing before claiming support. |
| **10,000** | **No evidence the current architecture supports this.** Direct-from-phone catalogue calls, 5 s/8 s polling, RLS-checked realtime fan-out and plaintext single-database chat would all need rework and measurement: a catalogue proxy or cache, removal of polling, rate limits, connection pooling review, and a load test. |

---

## 11. Production Readiness

| Item | Status | Evidence |
|---|---|---|
| Android compatibility | PARTIALLY IMPLEMENTED | `minSdk 26`, `targetSdk 37`. Tested only on one physical device (moto g34 5G, Android 15) per docs. Tablets, small screens, landscape, process death, slow networks, light theme: not tested (`docs/IMPLEMENTATION_STATUS.md`). |
| Error handling | IMPLEMENTED (data layer), PARTIALLY IMPLEMENTED (player) | Central `AppResult/AppError` and `call()` wrapper; player errors mostly collapse to `Unknown`. |
| Crash reporting | **NOT IMPLEMENTED** | `CrashReporter` bound to `NoOpCrashReporter` (`CommonModule.kt`). Release Timber tree forwards to it, so release crashes are invisible. |
| Analytics | NOT IMPLEMENTED | None. |
| Logging | IMPLEMENTED | Timber, categories, `LogRedactor`, debug-only HTTP logging. |
| Unit tests | PARTIALLY IMPLEMENTED | 22 JVM test files, 2 instrumented test files, about 188 `@Test` annotations (static count). Covers ViewModels, repositories, queue rules, parsing, validation. **No tests for `ExoPlaybackController`, `PlaybackService`, `ItunesMusicRemoteDataSource` network paths (only parsing), Realtime, or navigation.** |
| Test health | **UNVERIFIED / FAILING at last log** | `build-log.txt` ends with `UserRepositoryImplTest > someone I blocked is reported as blocked and left out of search FAILED` (`UserRepositoryImplTest.kt:34`) and `BUILD FAILED`. This log may predate later edits; I did not re-run. The test file is also uncommitted-modified. |
| Backend tests | IMPLEMENTED | Four SQL behavior scripts plus a shim for plain Postgres. Run by the author in rolled-back transactions. Not run by me. |
| CI | NOT IMPLEMENTED | No `.github`, no pipeline files were found. |
| Build configuration | PARTIALLY IMPLEMENTED | Flavors `dev`/`prod`; release has `isMinifyEnabled = true`, `isShrinkResources = true`. **Release signing is intentionally not configured** (`app/build.gradle.kts` comment). No lint config or baseline found. |
| Application id | **Placeholder** `com.yourcompany.musicsocial`, `versionCode = 1`. It cannot change after the first Play release. | `app/build.gradle.kts` |
| Permissions | IMPLEMENTED, minimal | `INTERNET`, `ACCESS_NETWORK_STATE`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`. No `POST_NOTIFICATIONS` (needed once push exists). |
| Backup and data extraction | IMPLEMENTED | `allowBackup=false`, extraction rules exclude all domains. |
| Google Play readiness | **NOT READY** | See list below. |

**Play Store blockers found**
1. Placeholder application id, no release signing, no upload key.
2. No privacy policy, no Data safety form content, no account-deletion flow. Google Play requires an in-app and web way to delete an account for apps that create accounts. Account deletion is NOT IMPLEMENTED (no code).
3. User-generated content (chat, profiles): Play policy expects reporting and blocking, which now exist in code (uncommitted). A moderation process for reports does not exist.
4. Media provider terms: auto-advancing previews and the Apple attribution requirement must be resolved (section 4.5).
5. Release build not rebuilt or smoke-tested since Phase 1 (`docs/IMPLEMENTATION_STATUS.md`).
6. No crash reporting.
7. Hosted backend is unverified, with email confirmation and password length defaults not aligned.

---

## 12. Missing Features

NOT IMPLEMENTED unless noted:
- Apple Music API, MusicKit, Developer/Music User Token, subscription check, full-length playback.
- Group chat creation and management UI and client code (database RPC exists).
- Presence, typing indicators, listening rooms, push notifications, in-app notifications and `notifications` table.
- Avatar upload (bucket exists), playlist rename and reorder, playlist sharing in chat, message edit/delete UI, older-message paging.
- Password reset, social login, onboarding, account deletion, light theme, tablet and landscape layouts, localisation.
- Crash reporting vendor, analytics, release signing, CI.
- Offline outbox for messages. Downloaded or offline music (not allowed for previews).

**Documentation drift** (`docs/IMPLEMENTATION_STATUS.md`): it says "6 migrations", "7 migrations" and lists song sharing, group chat UI, blocking/reporting and Storage buckets as not implemented. The repository now has **10 migrations** and the song-share, block, report, avatar-bucket and group-RPC work exists (uncommitted). `README.md` still says "Phase 2 ... playback and chat are not implemented yet". Treat both docs as out of date. `backend/supabase/README.md` also lists only migrations up to hardening.

**Dead schema:** `artists`, `songs`, `song_artists`, `user_likes` (and seed data) are unused by the app. The message `kind = 'playlist'` has no backing table.

---

## 13. Critical Technical Risks

1. **No full-length playback path exists, and the roadmap assumes one.** The player, queue, domain model and storage are all built around 30-second preview URLs. Apple Music full playback is a different integration (authorisation, DRM-protected player) and may impose requirements (installed app, subscriber account) that change the product. Needs a spike before design (sections 4, 5).
2. **Preview use may violate Apple's terms.** Auto-advancing queues of previews, previews as "independent entertainment", and sharing songs that play only as previews. The author flagged this himself and it is undecided.
3. **Catalogue dependence on a free, rate-limited, unofficial-chart API called from the client.** Per-IP limits, no SLA, two undocumented chart feeds, and no server-side cache. A user-visible failure mode as soon as many users share networks.
4. **The hosted backend has never been verified, and the client cannot be trusted to enforce security.** Migrations were only run against a local stack. Advisors were not re-run after migrations 7 to 10. No rate limiting on any write path.
5. **Release pipeline and observability are absent.** No crash reporting, no signing, no CI, a placeholder app id, last release build predates two phases of work, and the last recorded test run failed. Large uncommitted changes exist with only 2 commits of history.

Additional risks: full social graph readable by all signed-in users (S3); polling plus realtime duplication (section 10); chat read cursors based on the device clock (S7).

---

## 14. Recommended Architecture Improvements

No code was changed. These are recommendations only.

**Playback and catalogue**
1. Run a time-boxed spike on Apple's current MusicKit for Android documentation and terms. Decide product rules (subscriber-only full playback, previews for everyone else) **before** more UI is built on previews.
2. Keep `PlaybackController` as the seam. Add a second implementation or a `PlaybackSource` abstraction so preview (ExoPlayer) and licensed full playback can coexist behind the same interface. Today `ExoPlaybackController` assumes one URL per song.
3. Move the catalogue behind a thin server-side layer (Supabase Edge Function or a small service) that caches charts and lookups centrally and handles Apple's rate limits and any future Developer Token signing. A Developer Token private key must never ship in the APK.
4. Stop auto-advancing previews, or limit it, until the terms question is settled.
5. Add a `previewUrl` resolve cache in memory (not disk) with a short TTL to avoid one lookup per play, if the terms allow memory-only caching. Confirm first.

**Backend**
6. Apply and test all 10 migrations on the hosted dev project; re-run the security and performance advisors; align Auth settings (password minimum 8, email confirmation on).
7. Add rate limits (per-user insert quotas via triggers, or an Edge Function front) for messages, follow requests, reports and sign-up.
8. Decide the privacy model for `profiles` and `follows` visibility (S3) before launch. Add a unique constraint or throttle on reports.
9. Use server time for read cursors (an RPC `mark_read(conversation)` that sets `now()`).
10. Drop or document unused tables (`artists`, `songs`, `song_artists`, `user_likes`) to cut confusion.
11. Fix `profiles_avatar_url_format` to https only.

**Client**
12. Replace the 5 s/8 s polling with Realtime-driven updates and a refresh on app resume or reconnect. Subscribe to `conversation_members` changes for read receipts.
13. Add paging for older messages, group creation/management client code, avatar upload and account deletion.
14. Add a `MediaSession.Callback.onConnect` that allows only your own app and the system (S12). Verify the default behavior on your Media3 version.
15. Map more `PlaybackException` codes (HTTP status, format, decoder) and add bounded retry for transient errors.

**Quality and release**
16. Wire a crash reporter behind the existing `CrashReporter` seam.
17. Add CI: unit tests, lint, `assembleProdRelease`, SQL tests against a Supabase container, secret scanning.
18. Add tests for `ExoPlaybackController` (using Media3 test utilities), `ItunesMusicRemoteDataSource` network paths (MockWebServer), and a Realtime integration test.
19. Commit the current work in logical steps. Keep `docs/IMPLEMENTATION_STATUS.md` and both READMEs in step with it.

---

## 15. Prioritized Action Plan

| Priority | Action | Why |
|---|---|---|
| P0 | Fix the failing test (`UserRepositoryImplTest.kt:34`) or confirm it already passes; commit the uncommitted work | Establishes a known-good baseline; right now the working tree is the only copy of migrations 9 and 10 |
| P0 | Research spike: Apple MusicKit for Android (auth, playback, requirements, terms, fees). Written go/no-go and product rules | Gates the whole full-playback goal |
| P0 | Decide preview policy (auto-advance, attribution, sharing) | Terms risk affects what you can safely ship |
| P1 | Apply all migrations to hosted dev; run advisors; align Auth settings | Removes the largest unknown in the backend |
| P1 | Server-side catalogue cache/proxy and failure handling for 403/429 | Prevents catalogue outages from rate limits |
| P1 | Rate limits and abuse controls on writes; throttle reports | Needed before any public test |
| P1 | Crash reporting, CI, release signing, real application id | Needed to ship and to see failures |
| P2 | Replace polling with realtime; add older-message paging; server-time read cursors | Cost and correctness |
| P2 | Account deletion, privacy policy, Data safety content, moderation process | Play Store requirements |
| P2 | Group chat client, avatar upload, notifications (FCM), presence | Feature completion |
| P3 | Load test at 100, 1,000, 10,000 simulated users; revisit capacity | Evidence for scale claims |
| P3 | Clean dead schema; update docs | Maintainability |

---

## Final Summary

**Five biggest technical risks**
1. There is no Apple Music integration and no full-length playback. The app plays 30-second iTunes previews only, and the playback layer is built for that.
2. The preview feature, especially auto-advancing queues, may conflict with Apple's terms, and the decision is open.
3. The catalogue relies on a free, rate-limited, unofficial API called directly from every phone with no server-side cache.
4. The hosted Supabase backend has never been tested, advisors have not been re-run since migration 6, and no write path has rate limiting.
5. There is no release pipeline or observability: no crash reporting, signing, CI or real app id. The last test run in the log failed, and the newest work is uncommitted.

**Five most important next steps**
1. Get the build and tests green and commit the current work.
2. Run the Apple MusicKit for Android spike and write down the product rules for full playback.
3. Settle the preview policy and attribution.
4. Apply and verify all migrations on the hosted dev project, run the advisors, and add rate limits.
5. Add crash reporting, CI, a real application id and release signing, then rebuild and smoke-test the prod release.

---

### Sources
- Apple MusicKit for Android reference, token result: https://developer.apple.com/musickit/android/com/apple/android/sdk/authentication/TokenResult.html
- Apple MusicKit overview: https://developer.apple.com/musickit/
- Apple Developer Forums, MusicKit Android token problems: https://developer.apple.com/forums/thread/838633
- Stack Overflow, Apple MusicKit SDK in Android: https://stackoverflow.com/questions/72824625/apple-musickit-sdk-in-android

Content from these sources was rephrased for compliance with licensing restrictions.
