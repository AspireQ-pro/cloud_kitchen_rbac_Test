-- Add profile_image_key column to customers table for storing S3 object keys
ALTER TABLE customers 
ADD COLUMN profile_image_key VARCHAR(500);

-- Add comment for documentation
COMMENT ON COLUMN customers.profile_image_key IS 'S3 object key for customer profile image';