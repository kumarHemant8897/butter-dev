# Supabase backend

Schema, security rules and local-dev config for Butter. Nothing here contains secrets.

## Contents

- `migrations/` – ordered SQL: foundation, profiles+follows, catalogue, playlists+likes, chat (with RLS and Realtime publication)
- `seed.sql` – a few catalogue rows for local development (no users or messages)
- `config.toml` – settings for the local stack
- `tests/rls_smoke_test.sql` – behavioural checks of triggers, constraints and RLS (raises on failure)
- `tests/supabase_shim.sql` – minimal `auth` schema stand-in so migrations can be checked on plain PostgreSQL

## Option A: hosted project (recommended for real use)

1. Create a project at supabase.com. Create a second one for production.
2. Link and push the schema with the CLI: `supabase link --project-ref <ref>` then `supabase db push`
   (or paste each migration into the SQL editor in order).
3. Dashboard -> Authentication: set minimum password length, enable email confirmation for production, configure the site URL and redirect URLs.
4. Copy the URL and **publishable/anon** key into `local.properties` (see the root README).
5. Never put the `service_role` key in the app, in `local.properties`, or in git.

## Option B: local stack (needs Docker + Supabase CLI)

```powershell
supabase start        # prints the local URL and anon key
supabase db reset     # applies migrations and seed.sql
```
Use `SUPABASE_URL=http://10.0.2.2:54321` for the Android emulator (debug builds allow this cleartext host).

## Verifying the schema without Supabase

Apply `tests/supabase_shim.sql`, the migrations in order, `seed.sql`, then `tests/rls_smoke_test.sql` to a scratch PostgreSQL 17
database. The last result row must read `ALL CHECKS PASSED`. This was done during Phase 1, but it is not a substitute for testing
on a real Supabase project (Auth, Realtime and Storage behave there, not in the shim).

## Before launch checklist

- Run Supabase's security/performance advisors in the dashboard.
- Add rate limiting for message sending and sign-up.
- Decide Storage buckets and policies (avatars, playlist covers).
- Set up backups/point-in-time recovery on the production project.
