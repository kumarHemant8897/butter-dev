# Implementation status

Last updated at the end of Phase 1. Only things that were actually done and checked are marked as done.

## Verified

| Check | Result |
|---|---|
| `assembleDevDebug` (dev flavour) | Builds |
| `assembleProdRelease` (R8 shrinking on) | Builds; 2.8 MB unsigned APK. Signed with the debug key for a smoke test it installed, launched and showed no crash in the crash log |
| JVM unit tests (`testDevDebugUnitTest testDebugUnitTest :core:domain:test`) | 47 tests, 0 failures |
| Compose UI tests on a physical device (moto g34 5G, Android 15) | 4 tests, 0 failures (bottom bar, profile screen) |
| App on device | Installed and launched; all five tabs were opened and screenshots compared with Figma; no crashes |
| SQL migrations + `rls_smoke_test.sql` | Applied cleanly on PostgreSQL 17 with a stand-in `auth` schema; all checks passed |
| Secret scan | No keys or tokens in source; `local.properties` is git-ignored; only a fake key exists in a unit-test fixture |
| Module dependency rules | Checked from the build files: features do not depend on data or other features; domain/navigation depend on nothing |

## Implemented

- Gradle multi-module project (Gradle 9.8, AGP 9.5, Kotlin 2.4), version catalog, convention plugins, dev/prod flavours, secrets read from `local.properties` / environment, build rejects secret/service_role keys.
- Design system from the Figma "Afterglow" file: colours, Inter font, Lucide icons, shapes, spacing, buttons, search field, text field, music card/tile, song row, person row, filter chip, profile header, avatar, bottom navigation, loading/error/empty views, offline banner. Light theme tokens exist but light theme is not designed.
- Five screens built from the Figma frames: Home, Discover, Player, Chats (list), Profile (own profile, Hemant Kumar).
- Navigation shell with nested graphs per tab and type-safe routes.
- Core services: logging with redaction, `AppResult`/`AppError`, connectivity monitor, coroutine dispatchers, Supabase client provider (lazy, never created without credentials), error mapping, Room database + song cache, DataStore theme preference.
- Auth data layer (Supabase email auth) and music data layer (remote + cache), both unit-tested or compile-checked but **not run against a real Supabase project**.
- Database migrations for profiles, follows, catalogue, playlists, likes, conversations, members, messages, shares (RLS, triggers, Realtime publication).
- Docs, `backend/supabase` config, seed and SQL tests.

## Uses sample data (clearly marked in the UI and code)

All feed, people, song, playlist and chat content comes from `core:sample` (`SampleData`). This includes the profile
"Hemant Kumar / @hemantkumar" and its follower counts. Follow buttons, like hearts, filters, shuffle/repeat and play buttons
change **local screen state only**; nothing is saved or sent. Chat shows a conversation list only (no conversation screen).
Sample message times are fixed dates, so on a different day the list can show dates such as "Nov 8" instead of "Yesterday".

## Not implemented

- Sign-in / sign-up / onboarding screens (the repository exists, no UI calls it)
- Real profiles, follow graph, search, recommendations, notifications screen, settings
- Audio playback (Media3 is in the version catalog but not applied), playlists UI, likes persistence
- Conversation screen, sending/receiving messages, Realtime subscription, offline outbox
- `notifications` table, group-member management, blocking/reporting, Storage buckets
- Crash reporting vendor (a no-op `CrashReporter` is bound), analytics, release signing
- Light theme design, tablet/landscape layouts, localisation beyond English

## Known limitations and risks

- Package and application id are placeholders (`com.yourcompany.musicsocial`). The id is permanent once published: change it first.
- Retrofit is in the version catalog but unused (Supabase client handles networking); remove it or use it in Phase 3.
- The Supabase Kotlin client is a community library (listed in Supabase docs); pin and review upgrades.
- `fallbackToDestructiveMigration` is on for Room because every table is a cache; replace with real migrations before storing device-only data.
- Accessibility: content descriptions, headings and 44 dp targets were applied, but no TalkBack testing or formal audit was done.
- Not tested: tablets, small screens, dark/light switching UI, process death, slow networks, the `MusicCard` clipping change made after the last visual review.
- The Figma Profile frame shows someone else's profile; the Profile tab is an adaptation (Edit/Share instead of Follow/Message, no taste-match label).
- Player is a tab here, as in the Figma navigation. Chats shows a mini player dock that is local state only.

## Blockers

None for continuing development. Phase 2 needs a Supabase project (URL + publishable key) and a decision on the final
application id.

## Next steps

1. Create the Supabase project, apply migrations, set `local.properties`.
2. Build sign-in/sign-up/onboarding using `AuthRepository`; implement `UserRepository`; replace Profile and Discover sample data.
3. Pick the catalogue provider; implement search and Media3 playback; then chat.
4. Remove `core:sample` once no ViewModel uses it.
