-- Remove dummy/sample notification rows and preferences
-- (Development helper - run in Supabase/Postgres SQL runner.)

-- Remove sample notifications inserted by SUPABASE_NOTIFICATIONS_SCHEMA.sql
-- These were for user_id=1 and the initial "bid/auction/message" seed rows.
DELETE FROM public.notifications
WHERE user_id = 1
  AND type IN ('BID_PLACED', 'AUCTION_ENDING', 'NEW_MESSAGE', 'listing_sold', 'item_sold');

-- Remove all stored notification preferences (preferences are disabled in the app right now)
DELETE FROM public.notification_preferences;

