-- Behavioural checks for migration 10: blocks, reports, group chats, avatar storage rules.
-- Every check raises on failure. Run inside a transaction that is rolled back.
-- People: Asha (a), Ben (b), Cara (c), Dev (d). Asha and Ben follow each other; Asha follows Cara.

insert into auth.users (id, email, raw_user_meta_data) values
  ('a0000000-0000-4000-8000-00000000000a', 'asha@example.com', '{"username":"asha_s","display_name":"Asha"}'),
  ('b0000000-0000-4000-8000-00000000000b', 'ben@example.com',  '{"username":"ben_s","display_name":"Ben"}'),
  ('c0000000-0000-4000-8000-00000000000c', 'cara@example.com', '{"username":"cara_s","display_name":"Cara"}'),
  ('d0000000-0000-4000-8000-00000000000d', 'dev@example.com',  '{"username":"dev_s","display_name":"Dev"}');
insert into public.follows (follower_id, followee_id) values
  ('a0000000-0000-4000-8000-00000000000a', 'b0000000-0000-4000-8000-00000000000b'),
  ('b0000000-0000-4000-8000-00000000000b', 'a0000000-0000-4000-8000-00000000000a'),
  ('a0000000-0000-4000-8000-00000000000a', 'c0000000-0000-4000-8000-00000000000c');

set role authenticated;

-- Groups: Asha makes a group with Ben, Cara and Dev. Dev is not connected, so he is skipped.
select set_config('request.jwt.claim.sub', 'a0000000-0000-4000-8000-00000000000a', false);
do $$
declare g uuid; dm uuid; row record;
begin
  g := public.create_group_conversation('Weekend', array[
    'b0000000-0000-4000-8000-00000000000b', 'c0000000-0000-4000-8000-00000000000c', 'd0000000-0000-4000-8000-00000000000d']::uuid[]);
  if (select count(*) from public.conversation_members where conversation_id = g) <> 3 then
    raise exception 'group should have Asha, Ben and Cara only';
  end if;
  begin
    perform public.create_group_conversation('Strangers', array['d0000000-0000-4000-8000-00000000000d']::uuid[]);
    raise exception 'a group with only an unconnected person was created';
  exception when insufficient_privilege then null;
  end;
  insert into public.messages (conversation_id, sender_id, client_id, kind, body)
  values (g, 'a0000000-0000-4000-8000-00000000000a', 'e0000000-0000-4000-8000-000000000001', 'text', 'hello group');
  select * into row from public.my_conversations() where type = 'group';
  if row.title <> 'Weekend' or row.member_count <> 3 or row.last_sender_name <> 'Asha' then
    raise exception 'group row in chat list is wrong: % / % / %', row.title, row.member_count, row.last_sender_name;
  end if;
  perform set_config('test.group', g::text, false);

  dm := public.create_direct_conversation('b0000000-0000-4000-8000-00000000000b');
  perform set_config('test.dm', dm::text, false);
end $$;

-- Dev cannot read the group.
select set_config('request.jwt.claim.sub', 'd0000000-0000-4000-8000-00000000000d', false);
do $$
begin
  if exists (select 1 from public.messages) then raise exception 'a non-member read group messages'; end if;
end $$;

-- Ben blocks Asha: follows both ways are gone, and nobody can request, open or message a DM.
select set_config('request.jwt.claim.sub', 'b0000000-0000-4000-8000-00000000000b', false);
insert into public.blocks (blocker_id, blocked_id) values ('b0000000-0000-4000-8000-00000000000b', 'a0000000-0000-4000-8000-00000000000a');
do $$
begin
  if exists (select 1 from public.follows where 'b0000000-0000-4000-8000-00000000000b' in (follower_id, followee_id)) then
    raise exception 'blocking did not remove the follows';
  end if;
  if (select count(*) from public.blocks) <> 1 then raise exception 'the blocker cannot see the block'; end if;
  begin
    insert into public.messages (conversation_id, sender_id, client_id, kind, body)
    values (current_setting('test.dm')::uuid, 'b0000000-0000-4000-8000-00000000000b', 'e0000000-0000-4000-8000-000000000002', 'text', 'x');
    raise exception 'the blocker could still message in the DM';
  exception when insufficient_privilege then null;
  end;
  -- Group chats are not affected by a block.
  insert into public.messages (conversation_id, sender_id, client_id, kind, body)
  values (current_setting('test.group')::uuid, 'b0000000-0000-4000-8000-00000000000b', 'e0000000-0000-4000-8000-000000000003', 'text', 'still here');
end $$;

select set_config('request.jwt.claim.sub', 'a0000000-0000-4000-8000-00000000000a', false);
do $$
begin
  if exists (select 1 from public.blocks) then raise exception 'the blocked person can see the block'; end if;
  begin
    insert into public.follow_requests (requester_id, target_id) values ('a0000000-0000-4000-8000-00000000000a', 'b0000000-0000-4000-8000-00000000000b');
    raise exception 'a blocked person sent a follow request';
  exception when insufficient_privilege then null;
  end;
  begin
    insert into public.messages (conversation_id, sender_id, client_id, kind, body)
    values (current_setting('test.dm')::uuid, 'a0000000-0000-4000-8000-00000000000a', 'e0000000-0000-4000-8000-000000000004', 'text', 'x');
    raise exception 'the blocked person messaged in the DM';
  exception when insufficient_privilege then null;
  end;
  begin
    perform public.create_direct_conversation('b0000000-0000-4000-8000-00000000000b');
    raise exception 'a DM was opened while blocked';
  exception when insufficient_privilege then null;
  end;

  -- Reports: allowed as yourself, never readable back, not on someone else's behalf.
  insert into public.reports (reporter_id, target_user_id, reason, details)
  values ('a0000000-0000-4000-8000-00000000000a', 'd0000000-0000-4000-8000-00000000000d', 'spam', 'sends links');
  if exists (select 1 from public.reports) then raise exception 'reports are readable by users'; end if;
  begin
    insert into public.reports (reporter_id, target_user_id, reason)
    values ('c0000000-0000-4000-8000-00000000000c', 'd0000000-0000-4000-8000-00000000000d', 'spam');
    raise exception 'reported on behalf of someone else';
  exception when insufficient_privilege then null;
  end;

  -- Avatar addresses must look like web addresses.
  begin
    update public.profiles set avatar_url = 'javascript:alert(1)' where id = 'a0000000-0000-4000-8000-00000000000a';
    raise exception 'a non-http avatar address was saved';
  exception when check_violation then null;
  end;
end $$;

-- Cara is not part of the block, so the API must not tell her about it.
select set_config('request.jwt.claim.sub', 'c0000000-0000-4000-8000-00000000000c', false);
do $$
begin
  if public.is_blocked_between('a0000000-0000-4000-8000-00000000000a', 'b0000000-0000-4000-8000-00000000000b') then
    raise exception 'someone outside a block could see it';
  end if;
end $$;

-- Unblocking lets requests happen again.
select set_config('request.jwt.claim.sub', 'b0000000-0000-4000-8000-00000000000b', false);
delete from public.blocks where blocker_id = 'b0000000-0000-4000-8000-00000000000b';
select set_config('request.jwt.claim.sub', 'a0000000-0000-4000-8000-00000000000a', false);
insert into public.follow_requests (requester_id, target_id) values ('a0000000-0000-4000-8000-00000000000a', 'b0000000-0000-4000-8000-00000000000b');

-- Storage: Asha may write only in her own folder.
do $$
begin
  insert into storage.objects (bucket_id, name, owner_id) values ('avatars', 'a0000000-0000-4000-8000-00000000000a/me.jpg', 'a0000000-0000-4000-8000-00000000000a');
  begin
    insert into storage.objects (bucket_id, name, owner_id) values ('avatars', 'b0000000-0000-4000-8000-00000000000b/me.jpg', 'a0000000-0000-4000-8000-00000000000a');
    raise exception 'uploaded into someone else''s avatar folder';
  exception when insufficient_privilege then null;
  end;
end $$;

reset role;
select 'blocks, reports, groups, avatars: all checks passed' as result;
