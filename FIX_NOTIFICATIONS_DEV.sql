-- Dev fix: remove notification errors seen in Logcat
-- 1) App queries expect `notifications.data`
-- 2) RLS policies reference `app.current_user_id` which isn't set by this app's REST calls
--
-- Recommended execution tool:
--   psql / DBeaver / pgAdmin (not Supabase SQL Editor, due to wrapper issues you hit)
--
-- After running:
--   reinstall the app (or ensure Android Studio isn't serving cached APKs)

-- Add the missing column if your DB doesn't have it yet
alter table public.notifications
  add column if not exists data jsonb;

-- Avoid RLS/policy evaluation errors for now (development only)
-- If you want to keep RLS later, we can re-enable and create correct policies for this app's auth approach.
alter table public.notifications disable row level security;
alter table public.notification_preferences disable row level security;

