-- Local development seed (`supabase db reset` runs it after the migrations).
-- Catalogue rows only: they mirror the app's SampleData songs so a local backend has something
-- to return. No users, messages or credentials are created here; sign up through the app.
-- The catalog references are placeholders, not real provider ids.

insert into public.artists (id, name, catalog_provider, catalog_external_id) values
  ('00000000-0000-4000-a000-000000000001', 'The Midnight',    'dev', 'artist-the-midnight'),
  ('00000000-0000-4000-a000-000000000002', 'Arlo Parks',      'dev', 'artist-arlo-parks'),
  ('00000000-0000-4000-a000-000000000003', 'Phoebe Bridgers', 'dev', 'artist-phoebe'),
  ('00000000-0000-4000-a000-000000000004', 'Djo',             'dev', 'artist-djo'),
  ('00000000-0000-4000-a000-000000000005', 'Men I Trust',     'dev', 'artist-men-i-trust')
on conflict do nothing;

insert into public.songs (id, title, duration_ms, catalog_provider, catalog_external_id) values
  ('00000000-0000-4000-b000-000000000001', 'Night Drive',      222000, 'dev', 'song-night-drive'),
  ('00000000-0000-4000-b000-000000000002', 'Soft Landing',     198000, 'dev', 'song-soft-landing'),
  ('00000000-0000-4000-b000-000000000003', 'Moon Song',        276000, 'dev', 'song-moon-song'),
  ('00000000-0000-4000-b000-000000000004', 'End of Beginning', 159000, 'dev', 'song-end-of-beginning'),
  ('00000000-0000-4000-b000-000000000005', 'Show Me How',      214000, 'dev', 'song-show-me')
on conflict do nothing;

insert into public.song_artists (song_id, artist_id, position) values
  ('00000000-0000-4000-b000-000000000001', '00000000-0000-4000-a000-000000000001', 1),
  ('00000000-0000-4000-b000-000000000002', '00000000-0000-4000-a000-000000000002', 1),
  ('00000000-0000-4000-b000-000000000003', '00000000-0000-4000-a000-000000000003', 1),
  ('00000000-0000-4000-b000-000000000004', '00000000-0000-4000-a000-000000000004', 1),
  ('00000000-0000-4000-b000-000000000005', '00000000-0000-4000-a000-000000000005', 1)
on conflict do nothing;
