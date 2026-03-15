-- Check if foreign key constraint exists for listing_images
SELECT
    tc.constraint_name,
    tc.table_name,
    kcu.column_name,
    ccu.table_name AS foreign_table_name,
    ccu.column_name AS foreign_column_name
FROM information_schema.table_constraints AS tc
JOIN information_schema.key_column_usage AS kcu
    ON tc.constraint_name = kcu.constraint_name
    AND tc.table_schema = kcu.table_schema
JOIN information_schema.constraint_column_usage AS ccu
    ON ccu.constraint_name = tc.constraint_name
    AND ccu.table_schema = tc.table_schema
WHERE tc.constraint_type = 'FOREIGN KEY'
    AND tc.table_name = 'listing_images';

-- If the above query returns no results, run this to add the foreign key:
-- (Only run if needed)
/*
ALTER TABLE listing_images
DROP CONSTRAINT IF EXISTS listing_images_listing_id_fkey;

ALTER TABLE listing_images
ADD CONSTRAINT listing_images_listing_id_fkey
FOREIGN KEY (listing_id)
REFERENCES listings(id)
ON DELETE CASCADE;
*/

-- After adding the foreign key, test the query:
SELECT
    l.id,
    l.title,
    json_agg(
        json_build_object('image_path', li.image_path)
    ) as listing_images
FROM listings l
LEFT JOIN listing_images li ON l.id = li.listing_id
WHERE l.id = 27
GROUP BY l.id, l.title;
