-- Foundation: extensions and shared helpers used by every later migration.

create extension if not exists pg_trgm with schema extensions;
create extension if not exists citext with schema extensions;

-- Keeps updated_at correct without trusting the client clock.
create or replace function public.set_updated_at()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
  new.updated_at := now();
  return new;
end;
$$;
