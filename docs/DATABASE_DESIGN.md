# Database design (Supabase / PostgreSQL)

Implemented as migrations in `backend/supabase/migrations/`. All migrations plus an RLS test script were applied successfully
on PostgreSQL 17 using a minimal stand-in for Supabase's `auth` schema (see `backend/supabase/tests/`). They have **not** yet
been applied to a real Supabase project.

## Conventions

- UUID primary keys (`gen_random_uuid()`), `created_at` / `updated_at timestamptz`, `updated_at` maintained by trigger (not the client clock).
- RLS is enabled on every table. Policies use `(select auth.uid())` so the value is evaluated once per statement.
- Clients get column-level `UPDATE` grants only on user-editable columns; counters and sender/owner columns are not client-writable.
- Catalogue tables are read-only for clients; only the service role (trusted backend) writes them.
- Pagination: keyset on `(created_at, id)` for feeds and messages; offset only for ranked search results.
- Songs are **references to licensed catalogues** (`catalog_provider`, `catalog_external_id`), with `preview_url` only when licensed. Audio is not assumed to be hosted by us.

## Tables

| Table | Purpose | Key constraints / indexes |
|---|---|---|
| `profiles` | 1:1 with `auth.users`, created by trigger on sign-up | unique citext `username` (3-30 chars, `[a-z0-9._]`, no leading/trailing/double dot); trigram indexes for search; counters trigger-maintained |
| `follows` | follow graph | PK `(follower_id, followee_id)`, no self-follow, index `(followee_id, created_at desc)` |
| `artists`, `songs`, `song_artists` | catalogue; credit order in `position` | unique `(catalog_provider, catalog_external_id)`, trigram on `songs.title` |
| `playlists`, `playlist_songs` | user playlists; `song_count` trigger-maintained | index `(owner_id, updated_at desc)`, `(playlist_id, position)` |
| `user_likes` | liked songs | PK `(user_id, song_id)`, index `(user_id, created_at desc, song_id)` |
| `conversations` | `direct` or `group`; `direct_key` (`smallerUuid:largerUuid`) makes DM threads unique | `last_message_at` trigger-maintained |
| `conversation_members` | membership, role, **read/delivery cursors** (`last_delivered_at`, `last_read_at`) | index on `user_id` |
| `messages` | `text`/`song`/`playlist`; `client_id` makes sends idempotent | unique `(sender_id, client_id)`, body <= 4000 chars, history index `(conversation_id, created_at desc, id desc)` |
| `message_shares` | the song or playlist a message shares | at most one target |

Not yet created: `notifications` (planned table 12). Its design depends on push-notification decisions (Phase 5).
The planned name `message_song_shares` became `message_shares` so it also covers shared playlists.

## Chat design

- **1:1 and groups:** one `conversations` model; DMs are created through `create_direct_conversation(other_user)` (idempotent), groups through `create_group_conversation(title, member_ids)`. Both are `SECURITY DEFINER` RPCs so the conversation and its members are written atomically.
- **Delivery status:** stored as per-member cursors, not a receipt row per message per member (which grows quadratically in groups). The client derives SENT / DELIVERED / READ by comparing a message's `created_at` to other members' cursors.
- **Timestamps:** server `created_at` is authoritative for ordering.
- **Shared songs/playlists:** `messages.kind` plus a `message_shares` row. If the target is deleted the FK becomes null and the client shows it as unavailable.
- **Realtime:** `messages` and `conversation_members` are in the `supabase_realtime` publication; `postgres_changes` respects RLS, so members only receive their conversations.

## Known gaps

- Group member management (add/remove, promote) has no RPCs yet.
- Blocking/reporting users, message reactions, attachments (Storage buckets and policies) are not designed.
- `profiles` are readable by every signed-in user; add privacy controls before launch if private accounts are wanted.
- Rate limiting for message sending is not implemented (consider an Edge Function or a trigger-based limit).
