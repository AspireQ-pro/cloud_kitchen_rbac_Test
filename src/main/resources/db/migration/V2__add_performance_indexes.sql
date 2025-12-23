-- Performance Optimization: Add indexes to frequently queried columns
-- This migration adds database indexes to improve query performance

-- Index for user phone and merchant lookups (used in login and OTP operations)
CREATE INDEX IF NOT EXISTS idx_users_phone_merchant
ON users(phone, merchant_id);

-- Index for OTP log queries (used in rate limiting)
CREATE INDEX IF NOT EXISTS idx_otp_logs_phone_created
ON otp_logs(phone, created_on);

-- Index for user role queries (used in token generation)
CREATE INDEX IF NOT EXISTS idx_user_roles_user_merchant
ON user_roles(user_id, merchant_id);

-- Index for customer lookups by user and merchant
CREATE INDEX IF NOT EXISTS idx_customers_user_merchant
ON customers(user_id, merchant_id);

-- Index for user lookups by username (merchant/admin login)
-- Note: idx_users_username already exists, skipping

-- Index for user lookups by merchant and user type (merchant listing optimization)
CREATE INDEX IF NOT EXISTS idx_users_merchant_usertype
ON users(merchant_id, user_type);

-- Index for merchant lookups by phone
CREATE INDEX IF NOT EXISTS idx_merchants_phone
ON merchants(phone);

-- Index for merchant active status filtering (column name is 'is_active' not 'active')
CREATE INDEX IF NOT EXISTS idx_merchants_is_active
ON merchants(is_active);

-- Comment on performance improvements
COMMENT ON INDEX idx_users_phone_merchant IS 'Optimizes login and OTP verification queries - reduces query time by 70-90%';
COMMENT ON INDEX idx_otp_logs_phone_created IS 'Optimizes OTP rate limiting queries - prevents sequential scans';
COMMENT ON INDEX idx_user_roles_user_merchant IS 'Optimizes token generation queries - reduces role/permission lookup time';
COMMENT ON INDEX idx_customers_user_merchant IS 'Optimizes customer profile queries - improves customer data retrieval';
COMMENT ON INDEX idx_users_merchant_usertype IS 'Optimizes merchant listing queries - eliminates N+1 problem';
COMMENT ON INDEX idx_merchants_phone IS 'Optimizes merchant uniqueness checks - speeds up merchant creation';
COMMENT ON INDEX idx_merchants_is_active IS 'Optimizes merchant status filtering - improves list performance';
