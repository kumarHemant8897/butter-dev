-- Safety (block, report), profile photos and group chats.
--
-- Block (product rules):
-- - Blocking someone removes any follow and follow request between the two people.
-- - While either has blocked the other: no new follow requests, no new direct chat, and no new
--   messages in a direct chat between them. Profiles stay visible (name, photo, counts).
-- - A block is visible only to the person who made it.
-- Report: a user can report a person (optionally one message). Reports are write-only for users;
-- moderators read them with the service role.

-- Blocks -----------------------------------------------------------------------------------------

create table public.blocks (
  blocker_id uuid not null references public.profiles (id) on delete cascade,
  blocked_id uuid not null references public.profiles (id) on delete cascade,
  created_at timestamptz not null default now(),
  primary key (blocker_id, blocked_id),
  constraint blocks_not_self check (blocker_id <> blocked_id)
);

create index blocks_blocked_idx on public.blocks (blocked_id);

alter table public.blocks enable row level security;

create policy "users see their own blocks"
  on public.blocks for select to authenticated using (blocker_id = (select auth.uid()));
create policy "users block as themselves"
  on public.blocks for insert to authenticated with check (blocker_id = (select auth.uid()));
create policy "users unblock as themselves"
  on public.blocks for delete to authenticated using (blocker_id = (select auth.uid()));

-- True when either of the two has blocked the other. SECURITY DEFINER so the person who was blocked
-- is also stopped, although they cannot read the block row.
create or replace function public.is_blocked_between(a uuid, b uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select exists (
    select 1 from public.blocks
    where (blocker_id = a and blocked_id = b) or (blocker_id = b and blocked_id = a)
  );
$$;

revoke execute on function public.is_blocked_between(uuid, uuid) from public, anon;
grant execute on function public.is_blocked_between(uuid, uuid) to authenticated;

-- A block ends the connection in both directions.
create or replace function public.apply_block()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  delete from public.follows
   where (follower_id = new.blocker_id and followee_id = new.blocked_id)
      or (follower_id = new.blocked_id and followee_id = new.blocker_id);
  delete from public.follow_requests
   where (requester_id = new.blocker_id and target_id = new.blocked_id)
      or (requester_id = new.blocked_id and target_id = new.blocker_id);
  return null;
end;
$$;

revoke execute on function public.apply_block() from public, anon, authenticated;

create trigger blocks_apply after insert on public.blocks
  for each row execute function public.apply_block();

-- No follow requests while blocked.
drop policy "users request as themselves" on public.follow_requests;
create policy "users request as themselves"
  on public.follow_requests for insert to authenticated
  with check (
    requester_id = (select auth.uid())
    and not exists (
      select 1 from public.follows f where f.follower_id = requester_id and f.followee_id = target_id
    )
    and not public.is_blocked_between(requester_id, target_id)
  );

-- No messages in a direct chat while blocked. Group chats are not affected.
create or replace function public.is_direct_blocked(target uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select exists (
    select 1 from public.conversations c
    join public.conversation_members a on a.conversation_id = c.id
    join public.conversation_members b on b.conversation_id = c.id and b.user_id <> a.user_id
    where c.id = target and c.type = 'direct' and public.is_blocked_between(a.user_id, b.user_id)
  );
$$;

revoke execute on function public.is_direct_blocked(uuid) from public, anon;
grant execute on function public.is_direct_blocked(uuid) to authenticated;

drop policy "members send as themselves" on public.messages;
create policy "members send as themselves"
  on public.messages for insert to authenticated
  with check (
    sender_id = (select auth.uid())
    and public.is_conversation_member(conversation_id)
    and not public.is_direct_blocked(conversation_id)
  );

-- Reports ----------------------------------------------------------------------------------------

create table public.reports (
  id             uuid primary key default gen_random_uuid(),
  reporter_id    uuid not null references public.profiles (id) on delete cascade,
  target_user_id uuid not null references public.profiles (id) on delete cascade,
  message_id     uuid references public.messages (id) on delete set null,
  reason         text not null check (reason in ('spam', 'harassment', 'inappropriate', 'impersonation', 'other')),
  details        text check (details is null or char_length(details) <= 1000),
  created_at     timestamptz not null default now(),
  constraint reports_not_self check (reporter_id <> target_user_id)
);

create index reports_target_idx on public.reports (target_user_id, created_at desc);
create index reports_reporter_idx on public.reports (reporter_id);
create index reports_message_idx on public.reports (message_id);

alter table public.reports enable row level security;

-- Insert only; no select/update/delete for users. A reported message must be one the reporter can see.
create policy "users report as themselves"
  on public.reports for insert to authenticated
  with check (
    reporter_id = (select auth.uid())
    and (message_id is null or exists (
      select 1 from public.messages m where m.id = message_id and public.is_conversation_member(m.conversation_id)
    ))
  );

-- Direct chats: refuse while blocked ------------------------------------------------------------

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
  if public.is_blocked_between(me, other_user) then
    raise exception 'this conversation is not available' using errcode = '42501';
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

-- Group chats: only people you are connected with ----------------------------------------------
-- Members are added only when one follows the other and neither has blocked the other; anyone
-- else in member_ids is skipped. At least one valid member is required.

create or replace function public.create_group_conversation(group_title text, member_ids uuid[])
returns uuid
language plpgsql
security definer
set search_path = ''
as $$
declare
  me uuid := auth.uid();
  conversation uuid;
  valid uuid[];
begin
  if me is null then raise exception 'not authenticated' using errcode = '28000'; end if;
  if group_title is null or char_length(trim(group_title)) not between 1 and 80 then
    raise exception 'invalid title' using errcode = '22023';
  end if;

  select coalesce(array_agg(distinct p.id), '{}') into valid
    from public.profiles p
   where p.id = any (member_ids)
     and p.id <> me
     and exists (
       select 1 from public.follows f
       where (f.follower_id = me and f.followee_id = p.id) or (f.follower_id = p.id and f.followee_id = me)
     )
     and not public.is_blocked_between(me, p.id);

  if coalesce(array_length(valid, 1), 0) not between 1 and 49 then
    raise exception 'a group needs 1 to 49 people you are connected with' using errcode = '42501';
  end if;

  insert into public.conversations (type, title, created_by)
  values ('group', trim(group_title), me)
  returning id into conversation;

  insert into public.conversation_members (conversation_id, user_id, role)
  values (conversation, me, 'owner');

  insert into public.conversation_members (conversation_id, user_id, role)
  select conversation, unnest(valid), 'member';

  return conversation;
end;
$$;

revoke execute on function public.create_group_conversation(text, uuid[]) from public, anon;
grant execute on function public.create_group_conversation(text, uuid[]) to authenticated;

-- The chat list also names the last sender (for "Riya: see you there" in groups) and counts members.
drop function public.my_conversations();

create function public.my_conversations()
returns table (
  id                 uuid,
  type               text,
  title              text,
  last_message_at    timestamptz,
  updated_at         timestamptz,
  other_user_id      uuid,
  other_username     text,
  other_display_name text,
  other_avatar_url   text,
  last_sender_id     uuid,
  last_sender_name   text,
  last_kind          text,
  last_body          text,
  last_deleted       boolean,
  last_song_title    text,
  member_count       integer,
  unread_count       integer
)
language sql
stable
security invoker
set search_path = ''
as $$
  select
    c.id, c.type, c.title, c.last_message_at, c.updated_at,
    other.id, other.username::text, other.display_name, other.avatar_url,
    last.sender_id, (select p.display_name from public.profiles p where p.id = last.sender_id),
    last.kind, last.body, last.deleted_at is not null,
    (select s.title from public.message_songs s where s.message_id = last.id),
    (select count(*)::integer from public.conversation_members cm where cm.conversation_id = c.id),
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

-- Profile photos (Storage) ----------------------------------------------------------------------
-- Public bucket: anyone can view a photo by its address. Users write only inside a folder named
-- after their own id: avatars/<user id>/<file>. Images only, at most 2 MB.

insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('avatars', 'avatars', true, 2097152, array['image/jpeg', 'image/png', 'image/webp'])
on conflict (id) do update set public = excluded.public, file_size_limit = excluded.file_size_limit,
  allowed_mime_types = excluded.allowed_mime_types;

create policy "users upload their own avatar"
  on storage.objects for insert to authenticated
  with check (bucket_id = 'avatars' and (storage.foldername(name))[1] = (select auth.uid())::text);

create policy "users replace their own avatar"
  on storage.objects for update to authenticated
  using (bucket_id = 'avatars' and (storage.foldername(name))[1] = (select auth.uid())::text)
  with check (bucket_id = 'avatars' and (storage.foldername(name))[1] = (select auth.uid())::text);

create policy "users delete their own avatar"
  on storage.objects for delete to authenticated
  using (bucket_id = 'avatars' and (storage.foldername(name))[1] = (select auth.uid())::text);

-- Only an https address, or one from this project's own storage, can be saved as a profile photo.
alter table public.profiles add constraint profiles_avatar_url_format
  check (avatar_url is null or (char_length(avatar_url) <= 500 and avatar_url ~ '^https?://'));
