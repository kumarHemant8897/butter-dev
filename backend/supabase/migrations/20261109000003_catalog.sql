-- Music catalogue. Rows reference licensed external catalogues (catalog_provider +
-- catalog_external_id); audio is never assumed to be hosted by us. Only trusted backend code
-- (service role / Edge Functions) writes these tables.

create table public.artists (
  id                  uuid primary key default gen_random_uuid(),
  name                text not null check (char_length(name) between 1 and 200),
  image_url           text,
  catalog_provider    text,
  catalog_external_id text,
  created_at          timestamptz not null default now(),
  updated_at          timestamptz not null default now(),
  constraint artists_catalog_ref unique (catalog_provider, catalog_external_id)
);

create table public.songs (
  id                  uuid primary key default gen_random_uuid(),
  title               text not null check (char_length(title) between 1 and 300),
  album_title         text,
  artwork_url         text,
  duration_ms         integer not null default 0 check (duration_ms >= 0),
  -- Only set when the provider licence allows previews.
  preview_url         text,
  external_url        text,
  catalog_provider    text,
  catalog_external_id text,
  isrc                text,
  created_at          timestamptz not null default now(),
  updated_at          timestamptz not null default now(),
  constraint songs_catalog_ref unique (catalog_provider, catalog_external_id)
);

create index songs_title_trgm on public.songs using gin (title extensions.gin_trgm_ops);
create index songs_isrc_idx on public.songs (isrc) where isrc is not null;

-- Songs can credit several artists ("feat."); position keeps credit order.
create table public.song_artists (
  song_id   uuid not null references public.songs (id) on delete cascade,
  artist_id uuid not null references public.artists (id) on delete cascade,
  position  smallint not null default 1 check (position >= 1),
  primary key (song_id, artist_id)
);

create index song_artists_artist_idx on public.song_artists (artist_id);

create trigger artists_set_updated_at before update on public.artists
  for each row execute function public.set_updated_at();
create trigger songs_set_updated_at before update on public.songs
  for each row execute function public.set_updated_at();

alter table public.artists enable row level security;
alter table public.songs enable row level security;
alter table public.song_artists enable row level security;

-- Read-only for signed-in users. No insert/update/delete policies: writes need the service role.
create policy "catalogue artists are readable" on public.artists for select to authenticated using (true);
create policy "catalogue songs are readable" on public.songs for select to authenticated using (true);
create policy "catalogue credits are readable" on public.song_artists for select to authenticated using (true);
