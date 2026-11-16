-- Sharing a song in chat. message_shares pointed at public.songs (uuid rows the app never fills)
-- and was never used (empty), so it is replaced by message_songs, which keeps the shared catalogue
-- song's display data like library_songs. Preview audio is never stored.

drop table public.message_shares;

create table public.message_songs (
  message_id   uuid primary key references public.messages (id) on delete cascade,
  song_id      text not null check (song_id ~ '^[a-z]+:[0-9]{1,20}$'),
  title        text not null check (char_length(title) between 1 and 300),
  artist_names text not null check (char_length(artist_names) between 1 and 500),
  album_title  text check (album_title is null or char_length(album_title) <= 300),
  artwork_url  text check (artwork_url is null or (char_length(artwork_url) <= 500 and artwork_url like 'https://%')),
  external_url text check (external_url is null or (char_length(external_url) <= 500 and external_url like 'https://%')),
  duration_ms  integer not null default 0 check (duration_ms >= 0)
);

alter table public.message_songs enable row level security;

create policy "members read shared songs"
  on public.message_songs for select to authenticated
  using (exists (
    select 1 from public.messages m
    where m.id = message_id and public.is_conversation_member(m.conversation_id)
  ));

create policy "senders attach a song to their own song message"
  on public.message_songs for insert to authenticated
  with check (exists (
    select 1 from public.messages m
    where m.id = message_id and m.sender_id = (select auth.uid()) and m.kind = 'song'
  ));

revoke update on public.message_songs from authenticated;

-- Sends a song message and its song in one transaction, so a song message never exists without its
-- song. SECURITY INVOKER: the same RLS as direct inserts applies (sender must be the caller and a
-- member). Retrying with the same client id returns the message stored the first time.
create or replace function public.send_song_message(
  target_conversation uuid,
  client uuid,
  comment text,
  song_id text,
  title text,
  artist_names text,
  album_title text default null,
  artwork_url text default null,
  external_url text default null,
  duration_ms integer default 0
)
returns uuid
language plpgsql
security invoker
set search_path = ''
as $$
declare
  me uuid := auth.uid();
  message uuid;
begin
  if me is null then raise exception 'not authenticated' using errcode = '28000'; end if;

  insert into public.messages (conversation_id, sender_id, client_id, kind, body)
  values (target_conversation, me, client, 'song', nullif(trim(comment), ''))
  on conflict (sender_id, client_id) do nothing
  returning id into message;

  if message is null then
    select m.id into message from public.messages m where m.sender_id = me and m.client_id = client;
    return message;
  end if;

  insert into public.message_songs (message_id, song_id, title, artist_names, album_title, artwork_url, external_url, duration_ms)
  values (message, song_id, title, artist_names, album_title, artwork_url, external_url, coalesce(duration_ms, 0));
  return message;
end;
$$;

revoke execute on function public.send_song_message(uuid, uuid, text, text, text, text, text, text, text, integer) from public, anon;
grant execute on function public.send_song_message(uuid, uuid, text, text, text, text, text, text, text, integer) to authenticated;

-- The chat list now also returns the title of a shared song, for "Shared a song · Kesariya".
drop function public.my_conversations();

create function public.my_conversations()
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
  last_song_title  text,
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
    (select s.title from public.message_songs s where s.message_id = last.id),
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
    select m.id, m.sender_id, m.kind, m.body, m.deleted_at from public.messages m
    where m.conversation_id = c.id
    order by m.created_at desc, m.id desc
    limit 1
  ) last on true
  where me.user_id = (select auth.uid())
  order by coalesce(c.last_message_at, c.created_at) desc;
$$;

revoke execute on function public.my_conversations() from public, anon;
grant execute on function public.my_conversations() to authenticated;
