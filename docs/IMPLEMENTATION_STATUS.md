# Implementation status

Last updated after the social release: follow requests, other people's profiles, liked/saved songs and one-to-one chat.
Backend features were tested against a **local** Supabase stack only (reached over the home Wi-Fi, see "Testing on two phones").
Only things that were actually done and checked are marked as done.

## Verified

| Check | Result |
|---|---|
| `assembleDevDebug` | Builds, no compiler warnings |
| `assembleProdRelease` (R8 shrinking on) | Built and smoke-tested at the end of Phase 1; **not rebuilt after Phase 2** |
| JVM unit tests (`testDevDebugUnitTest testDebugUnitTest :core:domain:test`) | 165 tests, 0 failures |
| Database rules (`rls_smoke_test.sql`, `social_rules_test.sql`, each in a rolled-back transaction) | Both pass: follow only by request, library visible only to approved followers, chat only between connected people, strangers see nothing |
| Social flow on the phone, over Wi-Fi only (no USB tunnel) | New user's Chats shows "Find your people"; people search finds Riya; her profile shows counts and a locked music card; Follow becomes "Requested" and creates a request row; after she accepts, her liked songs and a Message button appear; a sent message is stored and her reply appears in the open chat without refreshing; the heart and save buttons on the Player store the song |
| Song search on the phone | Typing "kesariya" in Discover returns real Bollywood results from the iTunes India store, with album art |
| Preview playback on the phone | Tapping play starts the 30-second preview; Android reports the app playing music; the Player shows real art, title, artists and a moving progress bar; the mini player appears on other tabs |
| Compose UI tests on a physical device (moto g34 5G, Android 15) | 5 tests, 0 failures (bottom bar, profile screen) |
| Local Supabase (`supabase start`) | 6 migrations applied; `rls_smoke_test.sql` passed; security advisor shows only 3 intentional warnings |
| Sign-in on the phone against the local stack | Works: correct password signs in, wrong password shows "That email and password don't match", sign-out returns to the sign-in screen |
| Profile on the phone | Real profile loads; Edit profile saves name/username/bio to the database; a taken username shows "That username is taken"; the profile refreshes after saving |

| Home greeting | Uses the signed-in user's real first name |
| Secret scan (end of Phase 1) | No keys or tokens in source; `local.properties` is git-ignored. **Not re-run after Phase 2** |

## Implemented

- Phase 1 foundation: Gradle multi-module project, Afterglow design system (colours, Inter font, Lucide icons, components), navigation shell, logging with redaction, `AppResult`/`AppError`, Room cache, DataStore, build-time secrets (build rejects service_role keys).
- **Sign-in / sign-up screen** (`feature:auth`) with field validation (email, password of at least 8 characters, username rules, display name), backend error messages, and a gate in the app shell: restoring session -> sign-in -> main app. Without backend credentials the main app runs on sample data.
- **Real profile** (`feature:profile`): own profile, Edit profile (only changed fields are sent), sign-out.
- **Real "People to follow"** in Discover with optimistic follow/unfollow that rolls back on failure.
- `data:auth` and `data:user` implementations (Supabase), `UserRepository` with get/update/search/suggested/follow/unfollow.
- Database: 6 migrations (foundation, profiles+follows, catalogue, playlists+likes, chat, hardening), seed, config, RLS test script.

## Phase 3 so far

- Catalogue: iTunes Search API, India storefront (`ItunesMusicRemoteDataSource`). Response URLs are accepted only when HTTPS on Apple hosts; song ids are validated before lookups.
- Playback: one app-wide `PlaybackController` (interface in `core:domain`, ExoPlayer in the new `core:playback` module) with queue, shuffle and repeat rules in a unit-tested `PlaybackQueue`.
- Discover search with a 450 ms typing delay, loading/empty/error states, play/pause per row, and an "Open in Apple Music" link plus the iTunes credit (required by Apple's terms).
- Player tab and mini player use the real playback state; the sample now-playing data was removed.
- Home is fully real: "Trending in India" is Apple Music's India top-songs chart (25 songs, cached 30 min, shared with Discover's top songs), with a fallback to Apple's older iTunes chart feed when the main feed fails or hangs (8 s limit). "Back in your orbit" lists songs played this session (not saved across restarts). The Home search box opens Discover. Verified on the phone: chart loads, tapping a row plays it, the row turns into a pause button, and the mini player and history row appear.
- Discover music sections are real: "Voices you'll fall for" is a swipeable row of the India chart's lead artists (up to 12, names and genres from an iTunes lookup, image = album art of their chart song because the catalogue has no artist photos). "Stay a little longer" is Apple's live Bollywood chart (genre 1263), labelled "Listeners' choice". It reflects what Apple Music listeners in India play, not Butter users: a real Butter community playlist needs play/like tracking in the backend.
- New `feature:music` module with one shared song-list screen (`SongListRoute`, top level in the NavHost): the full chart ("See all" on Home's "Trending in India" and Discover's "The night's on repeat"), the Bollywood chart, and artist pages (up to 50 songs, most popular first). Each has Play all, Shuffle, per-row play/pause, Apple Music links and the iTunes credit. The bottom bar keeps the tab it was opened from highlighted. Verified on the phone: artists row, Bollywood card, artist page (Karan Aujla, A. R. Rahman) with artwork, Play all plays audio.
- Mini player has previous / next and a draggable seek bar; the content no longer slides under the status bar.
- Concurrent chart requests (Home, Discover, artists) share one download; the main chart feed gets 4 s before the fallback feed is tried.
- Home's sample social sections (friends listening, shared with you, sample mix) were removed; they return with chat and activity in Phase 4.
- Preview audio is streamed only: preview URLs are never written to the local cache, and playback pauses when the app goes to the background.

## Social (follow requests, profiles, library, chat)

- Migration 7 (`20261111000007_follow_requests_library_chat_rules.sql`):
  - every account approves its followers. Follow inserts into `follow_requests`; only the target can turn it into a follow (`accept_follow_request`). Clients can no longer insert into `follows`.
  - `library_songs` holds liked and saved songs (display data only; preview audio is never stored). Readable by the owner and approved followers only.
  - `create_direct_conversation` refuses unless one person follows the other. `my_conversations()` returns the chat list with the other person, last message and unread count in one call.
- Other people's profiles (`UserProfileRoute`): name, photo, bio and counts for everyone; Follow / Follow back / Requested / Following; liked and saved songs once approved; Message once either follows the other. A waiting request is re-checked every few seconds.
- Follow requests screen (from your Profile): accept or decline.
- Find your people (`PeopleSearchRoute`): search by name or username, newest members as suggestions. Opened from the Chats empty state, the Chats compose button, Discover's "See all" and Profile.
- Your own Profile shows your real liked and saved songs and how many requests are waiting.
- Player: heart (like) and + (save) write to your library.
- Chats: real conversations only. A new user sees "Find your people". The list refreshes every 8 s while open.
- Conversation: history, live messages through Supabase Realtime plus a 5 s refresh as a safety net, optimistic send with "Not sent. Tap to try again", "Seen" on your latest read message, read cursor updated on open.

## Playlists and background playback (end of Phase 3)

- Migration 8 (`20261112000008_playlists_v2.sql`): `playlist_songs` now stores catalogue songs (like `library_songs`), with song count and cover kept by a trigger, at most 100 playlists per user and 500 songs per playlist. A playlist is visible to its owner, and to approved followers when "Followers can see it" is on. `playlists_test.sql` checks this.
- Player: playlist button opens "Add to playlist" (your playlists plus "New playlist", which creates it with the playing song in it).
- Profile: "Playlists" section with "New"; other people's public playlists show to approved followers. A playlist opens on the shared song-list screen; the owner can remove songs and delete the playlist (both ask first).
- Background playback: `PlaybackService` (Media3 `MediaSessionService`) keeps music playing after leaving the app, with the media notification, lock-screen and Quick Settings controls; next/previous there use the app's queue. Verified on the phone: audio still playing 75 s after pressing Home, service in the foreground, Next and Pause in Quick Settings work, the queue advances in the background.
- Verified on the phone: creating "Road trip" from the Player stored it with the playing song. **Not yet checked on the phone:** the Playlists section on the profile, opening a playlist, removing a song, deleting a playlist (all covered by unit tests).

## Testing on two phones (same Wi-Fi)

1. On the PC: `supabase start` in `backend/`. The PC must stay on, on the same Wi-Fi as both phones. Windows Firewall has an inbound rule "Butter local Supabase (54321)" limited to the local subnet.
2. Set `SUPABASE_URL=http://<PC Wi-Fi address>:54321` in `local.properties` and rebuild. Debug builds allow plain HTTP to any address (`app/src/debug/res/xml/network_security_config.xml`); release builds are HTTPS only. When the router gives the PC a new address, update the URL and rebuild.
3. APK: `dist/Butter-dev-wifi-test.apk` (debug-signed, git-ignored). Send it to the other phone, allow "install unknown apps", install.
4. Accounts: sign up on each phone, or use the local test accounts (kept out of the repository).

## Still sample data (clearly marked in the UI and code)

`core:sample` feeds: Discover's feelings, and (only when the build has no backend) the sample profile and sample people. Feeling chips change local screen state only.

## Not implemented

- Password reset, email-confirmation handling beyond a "check your inbox" message (untested), "show password", social logins, onboarding
- Avatar upload, followers/following lists, a "private account" switch (every account approves followers today)
- Full-length songs (needs a licence; see Known limitations), renaming playlists and reordering their songs, catalogue rows in Supabase
- Sharing songs into chat, group chats in the UI, push notifications for messages and requests, offline outbox, older-message paging in a thread (the latest 50 are shown)
- `notifications` table, group-member management, blocking/reporting, Storage buckets
- Crash reporting vendor (a no-op `CrashReporter` is bound), analytics, release signing
- Light theme design, tablet/landscape layouts, localisation beyond English

## Known limitations and risks

- **The hosted `butter-dev` project has none of this applied.** Everything above ran against the local stack. The hosted Supabase MCP server was never authenticated.
- The sign-up form was exercised through unit tests and the sign-up API, but **not tapped through on the phone**. Email confirmation (on by default in hosted projects) is untested end to end.
- Package and application id are placeholders (`com.yourcompany.musicsocial`). The id is permanent once published: change it first.
- Hosted projects default to a 6-character minimum password; the app requires 8. Set the hosted Auth minimum to 8 to match.
- `profiles` are readable by every signed-in user (needed for people search); add privacy controls before launch if private accounts are wanted.
- Usernames taken at sign-up silently get a placeholder (`user_xxxx`); the user can change it in Edit profile, but the sign-up screen cannot warn in advance because anonymous users cannot read profiles.
- Retrofit is in the version catalog but unused; the Supabase Kotlin client is a community library (pin and review upgrades).
- `fallbackToDestructiveMigration` is on for Room because every table is a cache; replace with real migrations before storing device-only data.
- Accessibility: content descriptions, headings and 44 dp targets were applied, but no TalkBack testing or formal audit was done.
- Not tested: tablets, small screens, process death, slow networks, the light theme.
- Local-only test accounts exist in the local Supabase database; they are not in the repository.

- **Previews play back to back through a whole search list.** Apple's terms say previews must promote the song and not be used for independent entertainment. Auto-advancing through 25 previews may cross that line; a decision is needed (see next steps).
- The iTunes Search API allows about 20 requests per minute per IP and is free with no SLA; it is fine for development but not a production catalogue.
- The Wi-Fi test setup sends traffic over plain HTTP on the home network (debug builds only). Never ship it; production must use the hosted project over HTTPS.
- The second phone was simulated through the API from the PC (accept, like, reply). A real second phone has not been tested yet.
- Changing the backend address signs everyone out once (sessions are stored per server).

## Blockers

None for the Wi-Fi test. For testing away from home: authenticate the hosted MCP server or paste the 7 migrations into the hosted SQL editor.

## Next steps

1. Install `dist/Butter-dev-wifi-test.apk` on a second phone and run the two-phone test above.
2. Apply the 7 migrations to the hosted `butter-dev` project, set its Auth minimum password length to 8, and point the build at it (HTTPS, works anywhere).
3. Decide whether previews should stop after one song or keep auto-advancing.
4. Song sharing in chat, push notifications, followers/following lists.
