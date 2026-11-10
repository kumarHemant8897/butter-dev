-- Behavioural checks for the migrations: triggers, constraints and Row Level Security.
-- Every check raises an exception on failure, so a clean run means all checks passed.
-- Runs as the `authenticated` role with a simulated JWT subject, like a real app request.

-- Two users sign up (as the auth service would).
insert into auth.users (id, email, raw_user_meta_data) values
  ('11111111-1111-4111-8111-111111111111', 'hemant@example.com', '{"username":"hemantkumar","display_name":"Hemant Kumar"}'),
  ('22222222-2222-4222-8222-222222222222', 'eli@example.com',    '{"username":"Bad..Name"}'),
  ('33333333-3333-4333-8333-333333333333', 'june@example.com',   '{"username":"june"}');

do $$
begin
  if (select username from public.profiles where id = '11111111-1111-4111-8111-111111111111') <> 'hemantkumar' then
    raise exception 'profile trigger did not use the requested username';
  end if;
  if (select username from public.profiles where id = '22222222-2222-4222-8222-222222222222') not like 'user\_%' then
    raise exception 'invalid requested username was not replaced by a placeholder';
  end if;
end $$;

-- Act as Hemant.
set role authenticated;
select set_config('request.jwt.claim.sub', '11111111-1111-4111-8111-111111111111', false);

do $$
declare n int;
begin
  -- Own profile is editable; someone else's is not (RLS silently matches zero rows).
  update public.profiles set bio = 'building butter' where id = '11111111-1111-4111-8111-111111111111';
  update public.profiles set bio = 'hacked' where id = '22222222-2222-4222-8222-222222222222';
  get diagnostics n = row_count;
  if n <> 0 then raise exception 'RLS allowed editing another user''s profile'; end if;

  -- Counters are not client-writable (column privileges).
  begin
    update public.profiles set follower_count = 1000000 where id = '11111111-1111-4111-8111-111111111111';
    raise exception 'client was able to write follower_count';
  exception when insufficient_privilege then null;
  end;

  -- Username rules are enforced by the database.
  begin
    update public.profiles set username = 'x' where id = '11111111-1111-4111-8111-111111111111';
    raise exception 'too-short username was accepted';
  exception when check_violation then null;
  end;

  -- Following updates both counters; following as someone else is rejected.
  insert into public.follows (follower_id, followee_id) values ('11111111-1111-4111-8111-111111111111', '33333333-3333-4333-8333-333333333333');
  begin
    insert into public.follows (follower_id, followee_id) values ('22222222-2222-4222-8222-222222222222', '33333333-3333-4333-8333-333333333333');
    raise exception 'RLS allowed following on behalf of another user';
  exception when insufficient_privilege then null;
  end;
end $$;

reset role;
do $$
begin
  if (select follower_count from public.profiles where id = '33333333-3333-4333-8333-333333333333') <> 1
     or (select following_count from public.profiles where id = '11111111-1111-4111-8111-111111111111') <> 1 then
    raise exception 'follow counters were not maintained';
  end if;
end $$;

-- Catalogue is read-only for clients.
insert into public.songs (id, title, duration_ms) values ('44444444-4444-4444-8444-444444444444', 'Night Drive', 222000);
set role authenticated;
do $$
begin
  if not exists (select 1 from public.songs where id = '44444444-4444-4444-8444-444444444444') then
    raise exception 'signed-in users cannot read the catalogue';
  end if;
  begin
    insert into public.songs (title) values ('Injected');
    raise exception 'client was able to insert into the catalogue';
  exception when insufficient_privilege then null;
  end;
end $$;

-- Chat: Hemant opens a DM with June twice and gets the same conversation.
do $$
declare first_id uuid; second_id uuid;
begin
  first_id  := public.create_direct_conversation('33333333-3333-4333-8333-333333333333');
  second_id := public.create_direct_conversation('33333333-3333-4333-8333-333333333333');
  if first_id <> second_id then raise exception 'direct conversation was duplicated'; end if;
  perform set_config('test.conversation', first_id::text, false);

  insert into public.messages (conversation_id, sender_id, client_id, kind, body)
  values (first_id, '11111111-1111-4111-8111-111111111111', 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa', 'text', 'this song is a vibe');

  -- Retrying the same send (same client_id) is rejected instead of duplicating the message.
  begin
    insert into public.messages (conversation_id, sender_id, client_id, kind, body)
    values (first_id, '11111111-1111-4111-8111-111111111111', 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa', 'text', 'this song is a vibe');
    raise exception 'duplicate client_id was accepted';
  exception when unique_violation then null;
  end;

  -- Blank text messages are rejected.
  begin
    insert into public.messages (conversation_id, sender_id, client_id, kind, body)
    values (first_id, '11111111-1111-4111-8111-111111111111', gen_random_uuid(), 'text', '   ');
    raise exception 'blank message was accepted';
  exception when check_violation then null;
  end;
end $$;

-- Eli is not a member: he sees nothing and cannot post.
select set_config('request.jwt.claim.sub', '22222222-2222-4222-8222-222222222222', false);
do $$
declare conversation uuid := current_setting('test.conversation')::uuid;
begin
  if exists (select 1 from public.messages where conversation_id = conversation) then
    raise exception 'non-member can read messages';
  end if;
  if exists (select 1 from public.conversations where id = conversation) then
    raise exception 'non-member can see the conversation';
  end if;
  begin
    insert into public.messages (conversation_id, sender_id, client_id, kind, body)
    values (conversation, '22222222-2222-4222-8222-222222222222', gen_random_uuid(), 'text', 'let me in');
    raise exception 'non-member was able to post';
  exception when insufficient_privilege then null;
  end;
end $$;

-- June (member) can read it and the conversation was touched by the trigger.
select set_config('request.jwt.claim.sub', '33333333-3333-4333-8333-333333333333', false);
do $$
declare conversation uuid := current_setting('test.conversation')::uuid;
begin
  if (select count(*) from public.messages where conversation_id = conversation) <> 1 then
    raise exception 'member cannot read the message';
  end if;
  if (select last_message_at from public.conversations where id = conversation) is null then
    raise exception 'last_message_at was not updated';
  end if;
  -- Members may move only their own read cursor.
  update public.conversation_members set last_read_at = now()
   where conversation_id = conversation and user_id = '33333333-3333-4333-8333-333333333333';
  begin
    update public.conversation_members set role = 'owner'
     where conversation_id = conversation and user_id = '33333333-3333-4333-8333-333333333333';
    raise exception 'member was able to change their role';
  exception when insufficient_privilege then null;
  end;
end $$;

reset role;
select 'ALL CHECKS PASSED' as result;
