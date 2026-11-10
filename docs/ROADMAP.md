# Roadmap

| Phase | Goal | Main work |
|---|---|---|
| 1. Foundation (current) | Buildable, structured project with the Afterglow UI | Modules, design system, five screens with sample data, error/logging foundations, schema migrations, tests |
| 2. Accounts and profiles | Real users | Supabase project setup, apply migrations, sign-up/sign-in/sign-out UI, onboarding, username check, `UserRepository`, profile read/edit, avatar upload (Storage), follow/unfollow, replace sample profile/Discover data |
| 3. Music | Real catalogue and playback | Choose catalogue provider (licensing), import via Edge Function, search, `MusicRepository` in UI, Media3 `MediaSessionService`, mini player + full player, likes, playlists |
| 4. Chat | Real-time messaging | `ChatRepository` (PostgREST + Realtime), conversation screen, send with retry/outbox (Room), read receipts, song/playlist sharing, group management |
| 5. Engagement | Retention features | Notifications (FCM + Edge Function), feed/recommendations, friends activity ("listening now", opt-in), settings, light theme |
| 6. Hardening and launch | Play Store readiness | Release signing and Play App Signing, crash reporting vendor, analytics/privacy policy, R8 verification, performance (baseline profiles), accessibility review with TalkBack, rate limiting/moderation, load testing, store listing |

Open product decisions: catalogue provider and licensing, final app/package id (`applicationId` is permanent after release),
whether the Player is a tab, a full-screen overlay from a mini player, or both, and privacy defaults for listening activity.
