-- Behavioural checks for migration 7: follow requests, library privacy and who may message whom.
-- Every check raises an exception on failure, so a clean run means all checks passed.
-- Run inside a transaction that is rolled back (see backend/supabase/README.md).
--
-- People: Asha (a), Ben (b), Cara (c).

insert into auth.users (id, email, raw_user_meta_data) values
  ('a0000000-0000-4000-8000-00000000000a', 'asha@example.com', '{"username":"asha_t","display_name":"Asha"}'),
  ('b0000000-0000-4000-8000-00000000000b', 'ben@example.com',  '{"username":"ben_t","display_name":"Ben"}'),
  ('c0000000-0000-4000-8000-00000000000c', 'cara@example.com', '{"username":"cara_t","display_name":"Cara"}');

set role authenticated;

-- Ben likes a song and saves another.
select set_config('request.jwt.claim.sub', 'b0000000-0000-4000-8000-00000000000b', false);
insert into public.library_songs (user_id, kind, song_id, title, artist_names) values
  ('b0000000-0000-4000-8000-00000000000b', 'liked', 'itunes:1', 'Kesariya', 'Arijit Singh'),
  ('b0000000-0000-4000-8000-00000000000b', 'saved', 'itunes:2', 'Tum Hi Ho', 'Arijit Singh');

-- Asha: before following, Ben's library is hidden and messaging Ben is refused.
select set_config('request.jwt.claim.sub', 'a0000000-0000-4000-8000-00000000000a', false);
do $$
begin
  if exists (select 1 from public.library_songs where user_id = 'b0000000-0000-4000-8000-00000000000b') then
    raise exception 'a library was visible to someone who does not follow its owner';
  end if;
  if (select display_name from public.profiles where id = 'b0000000-0000-4000-8000-00000000000b') <> 'Ben' then
    raise exception 'profiles should stay visible to everyone signed in';
  end if;

  begin
    perform public.create_direct_conversation('b0000000-0000-4000-8000-00000000000b');
    raise exception 'a chat was opened without an accepted follow';
  exception when insufficient_privilege then null;
  end;

  -- Writing into someone else's library is refused.
  begin
    insert into public.library_songs (user_id, kind, song_id, title, artist_names)
    values ('b0000000-0000-4000-8000-00000000000b', 'liked', 'itunes:9', 'Injected', 'x');
    raise exception 'wrote into another user''s library';
  exception when insufficient_privilege then null;
  end;

  -- Only https artwork and well-formed ids are stored.
  begin
    insert into public.library_songs (user_id, kind, song_id, title, artist_names, artwork_url)
    values ('a0000000-0000-4000-8000-00000000000a', 'liked', 'itunes:3', 'x', 'y', 'http://evil.example/x.png');
    raise exception 'a non-https artwork address was stored';
  exception when check_violation then null;
  end;

  -- Asha asks to follow Ben; a request on someone else's behalf is refused.
  insert into public.follow_requests (requester_id, target_id)
  values ('a0000000-0000-4000-8000-00000000000a', 'b0000000-0000-4000-8000-00000000000b');
  begin
    insert into public.follow_requests (requester_id, target_id)
    values ('c0000000-0000-4000-8000-00000000000c', 'b0000000-0000-4000-8000-00000000000b');
    raise exception 'requested on behalf of another user';
  exception when insufficient_privilege then null;
  end;

  -- Asha cannot accept her own request to Ben.
  if public.accept_follow_request('a0000000-0000-4000-8000-00000000000a') then
    raise exception 'the requester accepted their own request';
  end if;
end $$;

-- Cara cannot see a request between two other people.
select set_config('request.jwt.claim.sub', 'c0000000-0000-4000-8000-00000000000c', false);
do $$
begin
  if exists (select 1 from public.follow_requests) then
    raise exception 'a stranger could see someone else''s follow request';
  end if;
end $$;

-- Ben sees the request and accepts it. Accepting twice is harmless.
select set_config('request.jwt.claim.sub', 'b0000000-0000-4000-8000-00000000000b', false);
do $$
begin
  if not exists (select 1 from public.follow_requests where target_id = 'b0000000-0000-4000-8000-00000000000b') then
    raise exception 'the target cannot see a request sent to them';
  end if;
  if not public.accept_follow_request('a0000000-0000-4000-8000-00000000000a') then
    raise exception 'accepting a pending request failed';
  end if;
  if public.accept_follow_request('a0000000-0000-4000-8000-00000000000a') then
    raise exception 'accepting an already accepted request reported success';
  end if;
end $$;

-- Asha now follows Ben: she sees his library and can message him; Ben can reply.
select set_config('request.jwt.claim.sub', 'a0000000-0000-4000-8000-00000000000a', false);
do $$
declare conversation uuid; listed int;
begin
  if (select count(*) from public.library_songs where user_id = 'b0000000-0000-4000-8000-00000000000b') <> 2 then
    raise exception 'an approved follower could not see the library';
  end if;
  if exists (select 1 from public.follow_requests) then
    raise exception 'the accepted request was not removed';
  end if;

  conversation := public.create_direct_conversation('b0000000-0000-4000-8000-00000000000b');
  insert into public.messages (conversation_id, sender_id, client_id, kind, body)
  values (conversation, 'a0000000-0000-4000-8000-00000000000a', 'd0000000-0000-4000-8000-000000000001', 'text', 'hey ben');
  perform set_config('test.conversation', conversation::text, false);

  -- A second request to someone already followed is refused.
  begin
    insert into public.follow_requests (requester_id, target_id)
    values ('a0000000-0000-4000-8000-00000000000a', 'b0000000-0000-4000-8000-00000000000b');
    raise exception 'requested to follow someone already followed';
  exception when insufficient_privilege then null;
  end;

  select count(*) into listed from public.my_conversations();
  if listed <> 1 then raise exception 'my_conversations returned % rows for Asha', listed; end if;
end $$;

select set_config('request.jwt.claim.sub', 'b0000000-0000-4000-8000-00000000000b', false);
do $$
declare row record;
begin
  select * into row from public.my_conversations();
  if row.other_display_name <> 'Asha' or row.last_body <> 'hey ben' or row.unread_count <> 1 then
    raise exception 'chat list for Ben is wrong: % / % / %', row.other_display_name, row.last_body, row.unread_count;
  end if;
  -- The follow points from Asha to Ben, and Ben may still message Asha.
  if public.create_direct_conversation('a0000000-0000-4000-8000-00000000000a') <> row.id then
    raise exception 'the followee could not open the same conversation';
  end if;
end $$;

-- Cara sees none of it.
select set_config('request.jwt.claim.sub', 'c0000000-0000-4000-8000-00000000000c', false);
do $$
begin
  if exists (select 1 from public.my_conversations()) then raise exception 'a stranger saw a conversation'; end if;
  if exists (select 1 from public.messages) then raise exception 'a stranger read messages'; end if;
  if exists (select 1 from public.library_songs) then raise exception 'a stranger read a library'; end if;
end $$;

reset role;
select 'social rules: all checks passed' as result;
