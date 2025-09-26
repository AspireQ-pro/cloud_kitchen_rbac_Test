-- Check OTP attempts for debugging
SELECT user_id, phone, otp_code, otp_attempts, otp_expires_at, otp_blocked_until 
FROM users 
WHERE phone = '9075027006';

-- Check OTP logs (with proper enum values)
SELECT phone, otp_code, otp_type, status, attempts_count, expires_at, created_on 
FROM otp_logs 
WHERE phone = '9075027006' 
ORDER BY created_on DESC 
LIMIT 5;

-- Check all recent OTP logs
SELECT phone, status, attempts_count, created_on 
FROM otp_logs 
ORDER BY created_on DESC 
LIMIT 10;