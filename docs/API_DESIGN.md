# API design

The app talks to Supabase directly (PostgREST, Auth, Realtime, Storage) using the publishable key; **Row Level Security is the
authorisation layer**. There is no custom backend yet. Sensitive or privileged operations (catalogue import, push
notifications, moderation) belong in Supabase Edge Functions using the service role, never in the app.

## Client-side layering

`ViewModel -> Repository (interface in core:domain) -> RemoteDataSource (interface in data:*) -> Supabase SDK`

| Contract | Interface | Implementation status |
|---|---|---|
| Auth | `AuthRepository` / `AuthRemoteDataSource` | Implemented (email sign-in/up/out, session state). Not exercised against a real project yet; no UI calls it |
| Music | `MusicRepository` / `MusicRemoteDataSource` | Implemented with Room cache; unit-tested; no UI uses it yet |
| User | `UserRepository` / `UserRemoteDataSource` | Interfaces and DTOs only (Phase 2) |
| Chat | `ChatRepository` / `ChatRemoteDataSource` | Interfaces and DTOs only (Phase 4) |

## Error contract

Every call returns `AppResult<T>` = `Success(data)` or `Failure(AppError)`.
`AppError`: `Network.Unavailable`, `Network.Timeout`, `Auth.{InvalidCredentials, SessionExpired, Forbidden, NotSignedIn}`,
`Server(status)`, `NotFound`, `Conflict`, `Validation(field, rule)`, `BackendNotConfigured`, `Unknown`.
`core:ui` maps each to user-facing text and decides whether a retry button makes sense.

## Planned Supabase calls

| Feature | Call |
|---|---|
| Song by id | `from("songs").select(SONG_COLUMNS).eq("id", id)` with embedded `song_artists(position, artist:artists(...))` |
| Song search | `ilike("title", "%q%")` with LIKE wildcards escaped; ranges 0-199 max |
| Follow / unfollow | insert / delete on `follows` (the server derives the follower from `auth.uid()`) |
| Open a DM / create a group | RPC `create_direct_conversation`, `create_group_conversation` |
| Message history | `messages` filtered by `conversation_id`, ordered `(created_at desc, id desc)`, keyset cursor, `limit <= 100` |
| Send | insert into `messages` with a client-generated `client_id` (retry-safe) |
| New messages | Realtime `postgres_changes` INSERT on `messages` filtered by `conversation_id` |
| Read receipts | update own `conversation_members.last_read_at` |

## Validation

Client rules (`ValidateUsernameUseCase`, `SendTextMessageUseCase`) mirror database CHECK constraints for fast feedback; the
database constraints are the source of truth.
