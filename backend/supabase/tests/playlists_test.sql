-- Behavioural checks for migration 8 (playlists). Every check raises on failure.
-- Run inside a transaction that is rolled back (see backend/supabase/README.md).
-- People: Asha (a) owns playlists, Ben (b) is her approved follower, Cara (c) is a stranger.

insert into auth.users (id, email, raw_user_meta_data) values
  ('a0000000-0000-4000-8000-00000000000a', 'asha@example.com', '{"username":"asha_p"}'),
  ('b0000000-0000-4000-8000-00000000000b', 'ben@example.com',  '{"username":"ben_p"}'),
  ('c0000000-0000-4000-8000-00000000000c', 'cara@example.com', '{"username":"cara_p"}');
insert into public.follows (follower_id, followee_id) values
  ('b0000000-0000-4000-8000-00000000000b', 'a0000000-0000-4000-8000-00000000000a');

set role authenticated;
select set_config('request.jwt.claim.sub', 'a0000000-0000-4000-8000-00000000000a', false);
insert into public.playlists (id, owner_id, title, is_public) values
  ('11111111-0000-4000-8000-000000000001', 'a0000000-0000-4000-8000-00000000000a', 'Late night', true),
  ('11111111-0000-4000-8000-000000000002', 'a0000000-0000-4000-8000-00000000000a', 'Just me', false);
insert into public.playlist_songs (playlist_id, song_id, title, artist_names, artwork_url) values
  ('11111111-0000-4000-8000-000000000001', 'itunes:1', 'Kesariya', 'Arijit Singh', 'https://is1.mzstatic.com/1.jpg'),
  ('11111111-0000-4000-8000-000000000001', 'itunes:2', 'Tum Hi Ho', 'Arijit Singh', 'https://is1.mzstatic.com/2.jpg');

do $$
begin
  if (select song_count from public.playlists where id = '11111111-0000-4000-8000-000000000001') <> 2 then
    raise exception 'song_count was not maintained';
  end if;
  if (select cover_url from public.playlists where id = '11111111-0000-4000-8000-000000000001') <> 'https://is1.mzstatic.com/1.jpg' then
    raise exception 'cover was not taken from the first song';
  end if;
  begin
    insert into public.playlist_songs (playlist_id, song_id, title, artist_names)
    values ('11111111-0000-4000-8000-000000000001', 'itunes:1', 'Kesariya', 'Arijit Singh');
    raise exception 'the same song was added twice';
  exception when unique_violation then null;
  end;
end $$;

-- Removing the cover song moves the cover to the next song.
delete from public.playlist_songs where playlist_id = '11111111-0000-4000-8000-000000000001' and song_id = 'itunes:1';
do $$
begin
  if (select cover_url from public.playlists where id = '11111111-0000-4000-8000-000000000001') <> 'https://is1.mzstatic.com/2.jpg'
     or (select song_count from public.playlists where id = '11111111-0000-4000-8000-000000000001') <> 1 then
    raise exception 'removing the cover song did not update cover and count';
  end if;
end $$;

-- Ben (approved follower) sees the public playlist and its songs, not the private one, and cannot edit.
select set_config('request.jwt.claim.sub', 'b0000000-0000-4000-8000-00000000000b', false);
do $$
declare n int;
begin
  if (select count(*) from public.playlists) <> 1 then raise exception 'follower saw % playlists, expected 1', (select count(*) from public.playlists); end if;
  if (select count(*) from public.playlist_songs) <> 1 then raise exception 'follower could not read the public playlist''s songs'; end if;
  begin
    insert into public.playlist_songs (playlist_id, song_id, title, artist_names)
    values ('11111111-0000-4000-8000-000000000001', 'itunes:9', 'Injected', 'x');
    raise exception 'a follower added a song to someone else''s playlist';
  exception when insufficient_privilege then null;
  end;
  delete from public.playlist_songs where playlist_id = '11111111-0000-4000-8000-000000000001';
  get diagnostics n = row_count;
  if n <> 0 then raise exception 'a follower removed songs from someone else''s playlist'; end if;
end $$;

-- Cara (stranger) sees nothing.
select set_config('request.jwt.claim.sub', 'c0000000-0000-4000-8000-00000000000c', false);
do $$
begin
  if exists (select 1 from public.playlists) or exists (select 1 from public.playlist_songs) then
    raise exception 'a stranger saw a playlist';
  end if;
end $$;

reset role;
select 'playlists: all checks passed' as result;
