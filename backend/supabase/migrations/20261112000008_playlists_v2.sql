-- Playlists hold catalogue songs (iTunes ids such as "itunes:123"), like library_songs, instead of
-- rows of public.songs, which the app never fills. The old playlist_songs table was never used
-- (empty), so it is replaced.
--
-- Visibility (product decision): a playlist is visible to its owner, and to the owner's approved
-- followers when is_public. Same rule as liked and saved songs.

drop table public.playlist_songs;

create table public.playlist_songs (
  playlist_id  uuid not null references public.playlists (id) on delete cascade,
  song_id      text not null check (song_id ~ '^[a-z]+:[0-9]{1,20}$'),
  title        text not null check (char_length(title) between 1 and 300),
  artist_names text not null check (char_length(artist_names) between 1 and 500),
  album_title  text check (album_title is null or char_length(album_title) <= 300),
  artwork_url  text check (artwork_url is null or (char_length(artwork_url) <= 500 and artwork_url like 'https://%')),
  external_url text check (external_url is null or (char_length(external_url) <= 500 and external_url like 'https://%')),
  duration_ms  integer not null default 0 check (duration_ms >= 0),
  added_at     timestamptz not null default now(),
  primary key (playlist_id, song_id)
);

-- Songs in the order they were added.
create index playlist_songs_order_idx on public.playlist_songs (playlist_id, added_at);

-- Count and cover: the cover is the art of the first song, kept in step as songs come and go.
create or replace function public.update_playlist_song_count()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  if tg_op = 'INSERT' then
    update public.playlists
       set song_count = song_count + 1,
           cover_url = coalesce(cover_url, new.artwork_url)
     where id = new.playlist_id;
  elsif tg_op = 'DELETE' then
    update public.playlists p
       set song_count = greatest(p.song_count - 1, 0),
           cover_url = case
             when p.cover_url is distinct from old.artwork_url then p.cover_url
             else (select s.artwork_url from public.playlist_songs s
                    where s.playlist_id = p.id and s.artwork_url is not null
                    order by s.added_at limit 1)
           end
     where p.id = old.playlist_id;
  end if;
  return null;
end;
$$;

revoke execute on function public.update_playlist_song_count() from public, anon, authenticated;

create trigger playlist_songs_update_count
  after insert or delete on public.playlist_songs
  for each row execute function public.update_playlist_song_count();

-- Who may read a playlist: the owner, or an approved follower when it is public.
create or replace function public.can_view_playlist(target uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select exists (
    select 1 from public.playlists p
    where p.id = target
      and (
        p.owner_id = (select auth.uid())
        or (p.is_public and exists (
          select 1 from public.follows f where f.follower_id = (select auth.uid()) and f.followee_id = p.owner_id
        ))
      )
  );
$$;

revoke execute on function public.can_view_playlist(uuid) from public, anon;
grant execute on function public.can_view_playlist(uuid) to authenticated;

drop policy "public playlists and own playlists are readable" on public.playlists;
create policy "owners and approved followers read playlists"
  on public.playlists for select to authenticated
  using (
    owner_id = (select auth.uid())
    or (is_public and exists (
      select 1 from public.follows f where f.follower_id = (select auth.uid()) and f.followee_id = owner_id
    ))
  );

alter table public.playlist_songs enable row level security;

create policy "playlist songs follow playlist visibility"
  on public.playlist_songs for select to authenticated
  using (public.can_view_playlist(playlist_id));

create policy "owners add songs"
  on public.playlist_songs for insert to authenticated
  with check (exists (select 1 from public.playlists p where p.id = playlist_id and p.owner_id = (select auth.uid())));

create policy "owners remove songs"
  on public.playlist_songs for delete to authenticated
  using (exists (select 1 from public.playlists p where p.id = playlist_id and p.owner_id = (select auth.uid())));

-- Rows are immutable: remove and add again instead of editing.
revoke update on public.playlist_songs from authenticated;

-- Keep lists bounded: 100 playlists per user, 500 songs per playlist.
create or replace function public.enforce_playlist_limits()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  if tg_table_name = 'playlists' then
    if (select count(*) from public.playlists where owner_id = new.owner_id) >= 100 then
      raise exception 'playlist limit reached' using errcode = '54000';
    end if;
  elsif (select count(*) from public.playlist_songs where playlist_id = new.playlist_id) >= 500 then
    raise exception 'playlist is full' using errcode = '54000';
  end if;
  return new;
end;
$$;

revoke execute on function public.enforce_playlist_limits() from public, anon, authenticated;

create trigger playlists_limit before insert on public.playlists
  for each row execute function public.enforce_playlist_limits();
create trigger playlist_songs_limit before insert on public.playlist_songs
  for each row execute function public.enforce_playlist_limits();
