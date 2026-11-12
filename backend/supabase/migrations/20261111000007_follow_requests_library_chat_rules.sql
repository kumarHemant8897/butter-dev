-- Follow requests, the song library (liked and saved songs) and who may message whom.
--
-- Rules (product decisions):
-- - Every account approves followers: "Follow" creates a request; only the target can accept it.
-- - Anyone signed in sees name, photo, bio and counts. Liked and saved songs are visible to the
--   owner and to approved followers only.
-- - A direct chat can start only between two people where one follows the other (an accepted request).

-- Follow requests ------------------------------------------------------------------------------

create table public.follow_requests (
  requester_id uuid not null references public.profiles (id) on delete cascade,
  target_id    uuid not null references public.profiles (id) on delete cascade,
  created_at   timestamptz not null default now(),
  primary key (requester_id, target_id),
  constraint follow_requests_not_self check (requester_id <> target_id)
);

-- "Requests waiting for me", newest first.
create index follow_requests_target_idx on public.follow_requests (target_id, created_at desc);

alter table public.follow_requests enable row level security;

create policy "requests are visible to both people"
  on public.follow_requests for select to authenticated
  using ((select auth.uid()) in (requester_id, target_id));

-- A request for someone already followed would never resolve; refuse it.
create policy "users request as themselves"
  on public.follow_requests for insert to authenticated
  with check (
    requester_id = (select auth.uid())
    and not exists (
      select 1 from public.follows f where f.follower_id = requester_id and f.followee_id = target_id
    )
  );

-- The requester cancels, the target declines; both are a delete.
create policy "either person removes a request"
  on public.follow_requests for delete to authenticated
  using ((select auth.uid()) in (requester_id, target_id));

-- Following now happens only by accepting a request. Clients can no longer insert follows.
drop policy "users follow as themselves" on public.follows;
revoke insert on public.follows from authenticated;

-- The target accepts: the request becomes a follow in one transaction. Returns false when there was
-- no such request (already handled, or cancelled), which the client treats as "nothing to do".
create or replace function public.accept_follow_request(requester uuid)
returns boolean
language plpgsql
security definer
set search_path = ''
as $$
declare
  me uuid := auth.uid();
begin
  if me is null then raise exception 'not authenticated' using errcode = '28000'; end if;

  delete from public.follow_requests where requester_id = requester and target_id = me;
  if not found then return false; end if;

  insert into public.follows (follower_id, followee_id) values (requester, me)
  on conflict do nothing;
  return true;
end;
$$;

revoke execute on function public.accept_follow_request(uuid) from public, anon;
grant execute on function public.accept_follow_request(uuid) to authenticated;

-- Song library ---------------------------------------------------------------------------------
-- Songs come from an external catalogue (iTunes), not from public.songs, so each row keeps the
-- display metadata it needs. Preview audio addresses are never stored: the app looks them up at
-- play time, as the provider's terms require.

create table public.library_songs (
  user_id      uuid not null references public.profiles (id) on delete cascade,
  kind         text not null check (kind in ('liked', 'saved')),
  -- The app's song id, e.g. "itunes:1635014240".
  song_id      text not null check (song_id ~ '^[a-z]+:[0-9]{1,20}$'),
  title        text not null check (char_length(title) between 1 and 300),
  artist_names text not null check (char_length(artist_names) between 1 and 500),
  album_title  text check (album_title is null or char_length(album_title) <= 300),
  artwork_url  text check (artwork_url is null or (char_length(artwork_url) <= 500 and artwork_url like 'https://%')),
  external_url text check (external_url is null or (char_length(external_url) <= 500 and external_url like 'https://%')),
  duration_ms  integer not null default 0 check (duration_ms >= 0),
  created_at   timestamptz not null default now(),
  primary key (user_id, kind, song_id)
);

-- "Liked songs of X, newest first".
create index library_songs_recent_idx on public.library_songs (user_id, kind, created_at desc);

alter table public.library_songs enable row level security;

create policy "owners and approved followers read a library"
  on public.library_songs for select to authenticated
  using (
    user_id = (select auth.uid())
    or exists (
      select 1 from public.follows f where f.follower_id = (select auth.uid()) and f.followee_id = user_id
    )
  );

create policy "users add to their own library"
  on public.library_songs for insert to authenticated
  with check (user_id = (select auth.uid()));

create policy "users remove from their own library"
  on public.library_songs for delete to authenticated
  using (user_id = (select auth.uid()));

-- Rows are immutable: remove and add again instead of editing.
revoke update on public.library_songs from authenticated;

-- Chat rules -----------------------------------------------------------------------------------

-- Same as before, plus: the two people must be connected by a follow in either direction.
create or replace function public.create_direct_conversation(other_user uuid)
returns uuid
language plpgsql
security definer
set search_path = ''
as $$
declare
  me  uuid := auth.uid();
  key text;
  conversation uuid;
begin
  if me is null then raise exception 'not authenticated' using errcode = '28000'; end if;
  if other_user is null or other_user = me then raise exception 'invalid participant' using errcode = '22023'; end if;
  if not exists (select 1 from public.profiles where id = other_user) then
    raise exception 'user not found' using errcode = 'P0002';
  end if;
  if not exists (
    select 1 from public.follows f
    where (f.follower_id = me and f.followee_id = other_user)
       or (f.follower_id = other_user and f.followee_id = me)
  ) then
    raise exception 'you can message people once a follow request is accepted' using errcode = '42501';
  end if;

  key := least(me::text, other_user::text) || ':' || greatest(me::text, other_user::text);

  select id into conversation from public.conversations where direct_key = key;
  if conversation is not null then return conversation; end if;

  insert into public.conversations (type, created_by, direct_key)
  values ('direct', me, key)
  on conflict (direct_key) do update set direct_key = excluded.direct_key
  returning id into conversation;

  insert into public.conversation_members (conversation_id, user_id, role)
  values (conversation, me, 'member'), (conversation, other_user, 'member')
  on conflict do nothing;

  return conversation;
end;
$$;

-- The chat list in one call: each conversation of the caller with the other person (direct chats),
-- the latest message and the caller's unread count. SECURITY INVOKER: it reads through the same RLS
-- policies as direct table access, so it can only ever return the caller's own conversations.
create or replace function public.my_conversations()
returns table (
  id               uuid,
  type             text,
  title            text,
  last_message_at  timestamptz,
  updated_at       timestamptz,
  other_user_id    uuid,
  other_username   text,
  other_display_name text,
  other_avatar_url text,
  last_sender_id   uuid,
  last_kind        text,
  last_body        text,
  last_deleted     boolean,
  unread_count     integer
)
language sql
stable
security invoker
set search_path = ''
as $$
  select
    c.id, c.type, c.title, c.last_message_at, c.updated_at,
    other.id, other.username::text, other.display_name, other.avatar_url,
    last.sender_id, last.kind, last.body, last.deleted_at is not null,
    (
      select count(*)::integer from public.messages m
      where m.conversation_id = c.id
        and m.sender_id <> me.user_id
        and m.deleted_at is null
        and (me.last_read_at is null or m.created_at > me.last_read_at)
    )
  from public.conversation_members me
  join public.conversations c on c.id = me.conversation_id
  left join lateral (
    select p.* from public.conversation_members om
    join public.profiles p on p.id = om.user_id
    where om.conversation_id = c.id and om.user_id <> me.user_id
    order by om.joined_at
    limit 1
  ) other on c.type = 'direct'
  left join lateral (
    select m.sender_id, m.kind, m.body, m.deleted_at from public.messages m
    where m.conversation_id = c.id
    order by m.created_at desc, m.id desc
    limit 1
  ) last on true
  where me.user_id = (select auth.uid())
  order by coalesce(c.last_message_at, c.created_at) desc;
$$;

revoke execute on function public.my_conversations() from public, anon;
grant execute on function public.my_conversations() to authenticated;
