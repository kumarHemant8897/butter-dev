-- Chat: one-to-one and group conversations, messages, song/playlist shares and read cursors.

create table public.conversations (
  id              uuid primary key default gen_random_uuid(),
  type            text not null check (type in ('direct', 'group')),
  title           text check (title is null or char_length(title) between 1 and 80),
  avatar_url      text,
  created_by      uuid references public.profiles (id) on delete set null,
  -- "smaller_uuid:larger_uuid" for direct chats, so two people can only have one DM thread.
  direct_key      text unique,
  last_message_at timestamptz,
  created_at      timestamptz not null default now(),
  updated_at      timestamptz not null default now(),
  constraint conversations_direct_key check (
    (type = 'direct' and direct_key is not null and title is null)
    or (type = 'group' and direct_key is null)
  )
);

create trigger conversations_set_updated_at before update on public.conversations
  for each row execute function public.set_updated_at();

create table public.conversation_members (
  conversation_id uuid not null references public.conversations (id) on delete cascade,
  user_id         uuid not null references public.profiles (id) on delete cascade,
  role            text not null default 'member' check (role in ('owner', 'admin', 'member')),
  joined_at       timestamptz not null default now(),
  -- Delivery/read state as cursors: one row per member instead of one receipt per message.
  last_delivered_at timestamptz,
  last_read_at      timestamptz,
  primary key (conversation_id, user_id)
);

-- "My conversations" lookups.
create index conversation_members_user_idx on public.conversation_members (user_id);

create table public.messages (
  id              uuid primary key default gen_random_uuid(),
  conversation_id uuid not null references public.conversations (id) on delete cascade,
  sender_id       uuid not null references public.profiles (id) on delete cascade,
  -- Generated once per send on the device and reused on retry: makes sends idempotent.
  client_id       uuid not null,
  kind            text not null check (kind in ('text', 'song', 'playlist')),
  body            text,
  created_at      timestamptz not null default now(),
  edited_at       timestamptz,
  deleted_at      timestamptz,
  constraint messages_client_id_unique unique (sender_id, client_id),
  -- Matches SendTextMessageUseCase.MAX_LENGTH.
  constraint messages_body_length check (body is null or char_length(body) <= 4000),
  constraint messages_text_has_body check (kind <> 'text' or deleted_at is not null or char_length(trim(body)) > 0)
);

-- History pages: newest first, keyset on (created_at, id).
create index messages_history_idx on public.messages (conversation_id, created_at desc, id desc);

-- A message shares one song or one playlist. Both can become null later if the target is
-- deleted; the client then shows the share as unavailable.
create table public.message_shares (
  message_id  uuid primary key references public.messages (id) on delete cascade,
  song_id     uuid references public.songs (id) on delete set null,
  playlist_id uuid references public.playlists (id) on delete set null,
  constraint message_shares_one_target check (num_nonnulls(song_id, playlist_id) <= 1)
);

-- Membership check used by RLS. SECURITY DEFINER avoids recursive RLS on conversation_members.
create or replace function public.is_conversation_member(target uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select exists (
    select 1 from public.conversation_members m
    where m.conversation_id = target and m.user_id = (select auth.uid())
  );
$$;

revoke execute on function public.is_conversation_member(uuid) from public, anon;
grant execute on function public.is_conversation_member(uuid) to authenticated;

-- Keep the conversation list ordered by activity.
create or replace function public.touch_conversation()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  update public.conversations
     set last_message_at = new.created_at
   where id = new.conversation_id
     and (last_message_at is null or last_message_at < new.created_at);
  return null;
end;
$$;

create trigger messages_touch_conversation
  after insert on public.messages
  for each row execute function public.touch_conversation();

-- Conversation creation goes through RPCs so the conversation and its members are written
-- atomically and nobody can add members to a conversation they are not in.

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

create or replace function public.create_group_conversation(group_title text, member_ids uuid[])
returns uuid
language plpgsql
security definer
set search_path = ''
as $$
declare
  me uuid := auth.uid();
  conversation uuid;
begin
  if me is null then raise exception 'not authenticated' using errcode = '28000'; end if;
  if group_title is null or char_length(trim(group_title)) not between 1 and 80 then
    raise exception 'invalid title' using errcode = '22023';
  end if;
  if coalesce(array_length(member_ids, 1), 0) not between 1 and 255 then
    raise exception 'a group needs between 2 and 256 members' using errcode = '22023';
  end if;

  insert into public.conversations (type, title, created_by)
  values ('group', trim(group_title), me)
  returning id into conversation;

  insert into public.conversation_members (conversation_id, user_id, role)
  values (conversation, me, 'owner');

  insert into public.conversation_members (conversation_id, user_id, role)
  select conversation, p.id, 'member'
    from public.profiles p
   where p.id = any (member_ids) and p.id <> me
  on conflict do nothing;

  return conversation;
end;
$$;

revoke execute on function public.create_direct_conversation(uuid) from public, anon;
revoke execute on function public.create_group_conversation(text, uuid[]) from public, anon;
grant execute on function public.create_direct_conversation(uuid) to authenticated;
grant execute on function public.create_group_conversation(text, uuid[]) to authenticated;

-- Row Level Security -------------------------------------------------------------------------

alter table public.conversations enable row level security;
alter table public.conversation_members enable row level security;
alter table public.messages enable row level security;
alter table public.message_shares enable row level security;

create policy "members read their conversations"
  on public.conversations for select to authenticated
  using (public.is_conversation_member(id));

create policy "group owners and admins rename groups"
  on public.conversations for update to authenticated
  using (
    type = 'group' and exists (
      select 1 from public.conversation_members m
      where m.conversation_id = conversations.id and m.user_id = (select auth.uid()) and m.role in ('owner', 'admin')
    )
  );

revoke update on public.conversations from authenticated;
grant update (title, avatar_url) on public.conversations to authenticated;

create policy "members see fellow members"
  on public.conversation_members for select to authenticated
  using (public.is_conversation_member(conversation_id));

create policy "members move their own read cursors"
  on public.conversation_members for update to authenticated
  using (user_id = (select auth.uid())) with check (user_id = (select auth.uid()));

create policy "members leave conversations"
  on public.conversation_members for delete to authenticated
  using (user_id = (select auth.uid()));

revoke update on public.conversation_members from authenticated;
grant update (last_delivered_at, last_read_at) on public.conversation_members to authenticated;

create policy "members read messages"
  on public.messages for select to authenticated
  using (public.is_conversation_member(conversation_id));

create policy "members send as themselves"
  on public.messages for insert to authenticated
  with check (sender_id = (select auth.uid()) and public.is_conversation_member(conversation_id));

create policy "senders edit or delete their messages"
  on public.messages for update to authenticated
  using (sender_id = (select auth.uid())) with check (sender_id = (select auth.uid()));

-- Clients can only edit the body and soft-delete; never move a message or change its sender.
revoke update on public.messages from authenticated;
grant update (body, edited_at, deleted_at) on public.messages to authenticated;

create policy "members read shares"
  on public.message_shares for select to authenticated
  using (exists (
    select 1 from public.messages msg
    where msg.id = message_id and public.is_conversation_member(msg.conversation_id)
  ));

create policy "senders attach shares to their messages"
  on public.message_shares for insert to authenticated
  with check (exists (
    select 1 from public.messages msg
    where msg.id = message_id and msg.sender_id = (select auth.uid())
  ));

-- Realtime: stream new messages and read-cursor changes. postgres_changes respects RLS.
alter publication supabase_realtime add table public.messages;
alter publication supabase_realtime add table public.conversation_members;
