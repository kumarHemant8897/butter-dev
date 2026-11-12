-- Findings from Supabase's security and performance advisors after migrations 1-5.

-- Trigger functions are called by the database only. They must not be reachable as
-- /rest/v1/rpc/<name>. (EXECUTE is checked when a trigger is created, not when it fires,
-- so revoking it does not affect the triggers themselves.)
revoke execute on function public.handle_new_user() from public, anon, authenticated;
revoke execute on function public.touch_conversation() from public, anon, authenticated;
revoke execute on function public.update_follow_counts() from public, anon, authenticated;
revoke execute on function public.update_playlist_song_count() from public, anon, authenticated;

-- Intentionally still callable by signed-in users (the advisor reports them as warnings):
--   create_direct_conversation, create_group_conversation  -> app RPCs; they check auth.uid() themselves
--   is_conversation_member                                  -> used inside RLS policies, so the caller needs EXECUTE

-- Foreign keys without a covering index make deletes and joins on the parent table slow.
create index if not exists conversations_created_by_idx on public.conversations (created_by);
create index if not exists message_shares_song_idx on public.message_shares (song_id);
create index if not exists message_shares_playlist_idx on public.message_shares (playlist_id);
create index if not exists playlist_songs_song_idx on public.playlist_songs (song_id);
create index if not exists playlist_songs_added_by_idx on public.playlist_songs (added_by);
create index if not exists user_likes_song_idx on public.user_likes (song_id);
