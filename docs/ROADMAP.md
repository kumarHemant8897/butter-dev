# Roadmap

| Phase | Goal | Main work |
|---|---|---|
| 1. Foundation (done) | Buildable, structured project with the Afterglow UI | Modules, design system, five screens with sample data, error/logging foundations, schema migrations, tests |
| 2. Accounts and profiles (mostly done, local backend only) | Real users | Done and verified on a phone against the local stack: sign-up/sign-in/sign-out, real profile, edit profile, suggested people, follow/unfollow. Still to do: apply migrations to the hosted project, email confirmation and password reset flows, onboarding, avatar upload (Storage), people search UI, profile of other users |
| 3. Music (in progress) | Real catalogue and playback | Done: iTunes India catalogue search, 30-second preview playback, Player and mini player. Remaining: likes, playlists, real Home feed, licensing talks for full songs. | Choose catalogue provider (licensing), import via Edge Function, search, `MusicRepository` in UI, Media3 `MediaSessionService`, mini player + full player, likes, playlists |
| 4. Chat | Real-time messaging | `ChatRepository` (PostgREST + Realtime), conversation screen, send with retry/outbox (Room), read receipts, song/playlist sharing, group management |
| 5. Engagement | Retention features | Notifications (FCM + Edge Function), feed/recommendations, friends activity ("listening now", opt-in), settings, light theme |
| 6. Hardening and launch | Play Store readiness | Release signing and Play App Signing, crash reporting vendor, analytics/privacy policy, R8 verification, performance (baseline profiles), accessibility review with TalkBack, rate limiting/moderation, load testing, store listing |

Open product decisions: catalogue provider and licensing, final app/package id (`applicationId` is permanent after release),
whether the Player is a tab, a full-screen overlay from a mini player, or both, and privacy defaults for listening activity.
