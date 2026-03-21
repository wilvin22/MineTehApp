-- Notify conversation buyers when a listing is marked as sold by the seller.
-- Target behavior (per app requirement):
-- - When `listings.status` transitions to 'sold'
-- - Find conversations that reference this listing and include the seller
-- - Notify the *other participant(s)* (buyers) by inserting into `notifications`
--
-- Notification type used: `listing_sold` (maps to Android `ITEM_SOLD`).

CREATE OR REPLACE FUNCTION public.notify_buyers_on_listing_sold()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
  v_seller_id INTEGER := NEW.seller_id;
  v_listing_title TEXT := NEW.title;
  v_link TEXT := 'listing-details.php?id=' || NEW.id;
BEGIN
  IF NEW.status = 'sold'
     AND (OLD.status IS DISTINCT FROM 'sold') THEN

    INSERT INTO public.notifications (user_id, type, title, message, link, is_read)
    SELECT DISTINCT
      CASE
        WHEN c.user1_id = v_seller_id THEN c.user2_id
        ELSE c.user1_id
      END AS buyer_id,
      'listing_sold' AS type,
      'Item sold!' AS title,
      '"' || COALESCE(v_listing_title, 'Item') || '" has been marked as sold.' AS message,
      v_link AS link,
      FALSE AS is_read
    FROM public.conversations c
    WHERE c.listing_id = NEW.id
      AND (c.user1_id = v_seller_id OR c.user2_id = v_seller_id)
      AND CASE
            WHEN c.user1_id = v_seller_id THEN c.user2_id
            ELSE c.user1_id
          END <> v_seller_id
      AND NOT EXISTS (
        SELECT 1
        FROM public.notifications n
        WHERE n.user_id = CASE
                              WHEN c.user1_id = v_seller_id THEN c.user2_id
                              ELSE c.user1_id
                           END
          AND n.type = 'listing_sold'
          AND COALESCE(n.link, '') = v_link
      );
  END IF;

  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_notify_buyers_on_listing_sold ON public.listings;

CREATE TRIGGER trg_notify_buyers_on_listing_sold
AFTER UPDATE OF status ON public.listings
FOR EACH ROW
EXECUTE FUNCTION public.notify_buyers_on_listing_sold();

