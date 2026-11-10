-- Profiles (1:1 with auth.users) and the follow graph.

create table public.profiles (
  id              uuid primary key references auth.users (id) on delete cascade,
  username        extensions.citext not null unique,
  display_name    text not null,
  bio             text,
  avatar_url      text,
  follower_count  integer not null default 0 check (follower_count >= 0),
  following_count integer not null default 0 check (following_count >= 0),
  created_at      timestamptz not null default now(),
  updated_at      timestamptz not null default now(),
  -- Mirrors ValidateUsernameUseCase on the client. The database is the source of truth.
  constraint profiles_username_format check (
    char_length(username) between 3 and 30
    and username::text ~ '^[a-z0-9._]+$'
    and username::text !~ '^\.|\.$|\.\.'
  ),
  constraint profiles_display_name_length check (char_length(display_name) between 1 and 50),
  constraint profiles_bio_length check (bio is null or char_length(bio) <= 160)
);

create index profiles_username_trgm on public.profiles using gin ((username::text) extensions.gin_trgm_ops);
create index profiles_display_name_trgm on public.profiles using gin (display_name extensions.gin_trgm_ops);

create trigger profiles_set_updated_at
  before update on public.profiles
  for each row execute function public.set_updated_at();

-- Create a profile automatically when a user signs up. The username comes from sign-up metadata
-- when valid; otherwise a unique placeholder is generated that the user can change later.
create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
  requested text := lower(coalesce(new.raw_user_meta_data ->> 'username', ''));
  fallback  text := 'user_' || substr(replace(new.id::text, '-', ''), 1, 12);
begin
  insert into public.profiles (id, username, display_name)
  values (
    new.id,
    case when requested ~ '^[a-z0-9._]{3,30}$' and requested !~ '^\.|\.$|\.\.' then requested else fallback end,
    coalesce(nullif(trim(new.raw_user_meta_data ->> 'display_name'), ''), 'New listener')
  )
  on conflict (username) do nothing;

  -- Requested username was taken: fall back so sign-up never fails on a profile clash.
  if not found then
    insert into public.profiles (id, username, display_name)
    values (new.id, fallback, 'New listener');
  end if;
  return new;
end;
$$;

create trigger on_auth_user_created
  after insert on auth.users
  for each row execute function public.handle_new_user();

create table public.follows (
  follower_id uuid not null references public.profiles (id) on delete cascade,
  followee_id uuid not null references public.profiles (id) on delete cascade,
  created_at  timestamptz not null default now(),
  primary key (follower_id, followee_id),
  constraint follows_not_self check (follower_id <> followee_id)
);

-- "Who follows me" lookups; the primary key already covers "who do I follow".
create index follows_followee_idx on public.follows (followee_id, created_at desc);

-- Denormalised counters so profile reads never need COUNT(*).
create or replace function public.update_follow_counts()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  if tg_op = 'INSERT' then
    update public.profiles set following_count = following_count + 1 where id = new.follower_id;
    update public.profiles set follower_count  = follower_count  + 1 where id = new.followee_id;
  elsif tg_op = 'DELETE' then
    update public.profiles set following_count = greatest(following_count - 1, 0) where id = old.follower_id;
    update public.profiles set follower_count  = greatest(follower_count  - 1, 0) where id = old.followee_id;
  end if;
  return null;
end;
$$;

create trigger follows_update_counts
  after insert or delete on public.follows
  for each row execute function public.update_follow_counts();

-- Row Level Security -------------------------------------------------------------------------

alter table public.profiles enable row level security;
alter table public.follows enable row level security;

create policy "profiles are visible to signed-in users"
  on public.profiles for select to authenticated using (true);

create policy "users update their own profile"
  on public.profiles for update to authenticated
  using ((select auth.uid()) = id) with check ((select auth.uid()) = id);

-- Counters are maintained by the trigger only; clients may edit profile fields only.
revoke update on public.profiles from authenticated;
grant update (username, display_name, bio, avatar_url) on public.profiles to authenticated;

create policy "follows are visible to signed-in users"
  on public.follows for select to authenticated using (true);

create policy "users follow as themselves"
  on public.follows for insert to authenticated
  with check ((select auth.uid()) = follower_id);

create policy "users unfollow as themselves"
  on public.follows for delete to authenticated
  using ((select auth.uid()) = follower_id);
