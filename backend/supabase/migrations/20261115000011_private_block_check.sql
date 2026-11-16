-- is_blocked_between is callable through the API (/rest/v1/rpc/is_blocked_between), and it used to answer
-- for any two people. That let anyone signed in find out whether two other people had blocked each other.
-- It now answers only when the caller is one of the two; for anyone else it returns false.
-- Every existing caller passes the signed-in user as one side:
--   follow_requests insert policy (requester_id = auth.uid()), create_direct_conversation and
--   create_group_conversation (me = auth.uid()), and is_direct_blocked (a member of the conversation,
--   checked by the messages insert policy).
create or replace function public.is_blocked_between(a uuid, b uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select (select auth.uid()) in (a, b)
     and exists (
       select 1 from public.blocks
       where (blocker_id = a and blocked_id = b) or (blocker_id = b and blocked_id = a)
     );
$$;

revoke execute on function public.is_blocked_between(uuid, uuid) from public, anon;
grant execute on function public.is_blocked_between(uuid, uuid) to authenticated;
