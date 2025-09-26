-- OTP Logs table schema
CREATE TABLE IF NOT EXISTS otp_logs (
    otp_log_id SERIAL PRIMARY KEY,
    merchant_id INTEGER REFERENCES merchants(merchant_id),
    phone VARCHAR(20) NOT NULL,
    otp_code VARCHAR(4) NOT NULL,
    otp_type VARCHAR(20) NOT NULL DEFAULT 'LOGIN',
    status VARCHAR(20) NOT NULL DEFAULT 'SENT',
    ip_address INET,
    user_agent TEXT,
    attempts_count INTEGER NOT NULL DEFAULT 0,
    verified_at TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    created_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_otp_logs_merchant_id ON otp_logs(merchant_id);
CREATE INDEX IF NOT EXISTS idx_otp_logs_phone ON otp_logs(phone);
CREATE INDEX IF NOT EXISTS idx_otp_logs_phone_status ON otp_logs(phone, status);
CREATE INDEX IF NOT EXISTS idx_otp_logs_expires_at ON otp_logs(expires_at);
CREATE INDEX IF NOT EXISTS idx_otp_logs_merchant_phone ON otp_logs(merchant_id, phone);

-- Check if table exists and has data
SELECT COUNT(*) as total_records FROM otp_logs;
SELECT * FROM otp_logs ORDER BY created_on DESC LIMIT 5;