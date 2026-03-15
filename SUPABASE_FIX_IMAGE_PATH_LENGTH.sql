-- Fix image_path column length to support base64 data URIs
-- Base64 images can be 50,000+ characters, so we need TEXT type instead of VARCHAR(500)

-- Update listing_images table to support longer image paths (base64 data URIs)
ALTER TABLE listing_images 
ALTER COLUMN image_path TYPE TEXT;

-- Verify the change
SELECT column_name, data_type, character_maximum_length 
FROM information_schema.columns 
WHERE table_name = 'listing_images' 
AND column_name = 'image_path';