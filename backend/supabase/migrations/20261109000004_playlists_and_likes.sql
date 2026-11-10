-- Playlists, their songs, and liked songs.

create table public.playlists (
  id          uuid primary key default gen_random_uuid(),
  owner_id    uuid not null references public.profiles (id) on delete cascade,
  title       text not null check (char_length(title) between 1 and 100),
  description text check (description is null or char_length(description) <= 300),
  cover_url   text,
  is_public   boolean not null default true,
  song_count  integer not null default 0 check (song_count >= 0),
  created_at  timestamptz not null default now(),
  updated_at  timestamptz not null default now()
);

create index playlists_owner_idx on public.playlists (owner_id, updated_at desc);

create trigger playlists_set_updated_at before update on public.playlists
  for each row execute function public.set_updated_at();

create table public.playlist_songs (
  playlist_id uuid not null references public.playlists (id) on delete cascade,
  song_id     uuid not null references public.songs (id) on delete cascade,
  position    integer not null check (position >= 0),
  added_by    uuid references public.profiles (id) on delete set null,
  added_at    timestamptz not null default now(),
  primary key (playlist_id, song_id)
);

create index playlist_songs_order_idx on public.playlist_songs (playlist_id, position);

create or replace function public.update_playlist_song_count()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  if tg_op = 'INSERT' then
    update public.playlists set song_count = song_count + 1 where id = new.playlist_id;
  elsif tg_op = 'DELETE' then
    update public.playlists set song_count = greatest(song_count - 1, 0) where id = old.playlist_id;
  end if;
  return null;
end;
$$;

create trigger playlist_songs_update_count
  after insert or delete on public.playlist_songs
  for each row execute function public.update_playlist_song_count();

create table public.user_likes (
  user_id    uuid not null references public.profiles (id) on delete cascade,
  song_id    uuid not null references public.songs (id) on delete cascade,
  created_at timestamptz not null default now(),
  primary key (user_id, song_id)
);

-- "My liked songs, newest first" with keyset pagination on (created_at, song_id).
create index user_likes_recent_idx on public.user_likes (user_id, created_at desc, song_id);

-- Row Level Security -------------------------------------------------------------------------

alter table public.playlists enable row level security;
alter table public.playlist_songs enable row level security;
alter table public.user_likes enable row level security;

create policy "public playlists and own playlists are readable"
  on public.playlists for select to authenticated
  using (is_public or owner_id = (select auth.uid()));

create policy "users create their own playlists"
  on public.playlists for insert to authenticated
  with check (owner_id = (select auth.uid()));

create policy "owners update their playlists"
  on public.playlists for update to authenticated
  using (owner_id = (select auth.uid())) with check (owner_id = (select auth.uid()));

create policy "owners delete their playlists"
  on public.playlists for delete to authenticated
  using (owner_id = (select auth.uid()));

-- song_count is trigger-maintained.
revoke update on public.playlists from authenticated;
grant update (title, description, cover_url, is_public) on public.playlists to authenticated;

create policy "playlist songs follow playlist visibility"
  on public.playlist_songs for select to authenticated
  using (exists (
    select 1 from public.playlists p
    where p.id = playlist_id and (p.is_public or p.owner_id = (select auth.uid()))
  ));

create policy "owners add songs"
  on public.playlist_songs for insert to authenticated
  with check (
    added_by = (select auth.uid())
    and exists (select 1 from public.playlists p where p.id = playlist_id and p.owner_id = (select auth.uid()))
  );

create policy "owners reorder songs"
  on public.playlist_songs for update to authenticated
  using (exists (select 1 from public.playlists p where p.id = playlist_id and p.owner_id = (select auth.uid())));

create policy "owners remove songs"
  on public.playlist_songs for delete to authenticated
  using (exists (select 1 from public.playlists p where p.id = playlist_id and p.owner_id = (select auth.uid())));

create policy "likes are visible to signed-in users"
  on public.user_likes for select to authenticated using (true);

create policy "users like as themselves"
  on public.user_likes for insert to authenticated with check (user_id = (select auth.uid()));

create policy "users unlike as themselves"
  on public.user_likes for delete to authenticated using (user_id = (select auth.uid()));
