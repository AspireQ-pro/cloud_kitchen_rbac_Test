-- CLOUD KITCHEN RBAC SERVICE DATABASE SCHEMA
-- ============================================================================
-- Multitenant Role-Based Access Control for Cloud Kitchen Application
-- Supporting roles: Super Admin, Merchant, Customer
-- ============================================================================

-- ============================================================================
-- 1. MERCHANTS TABLE (Global - no merchant_id needed)
-- ============================================================================
CREATE TABLE IF NOT EXISTS merchants (
    merchant_id SERIAL PRIMARY KEY,
    merchant_name VARCHAR(255) NOT NULL UNIQUE,
    business_name VARCHAR(255) NOT NULL,
    business_type VARCHAR(100) DEFAULT 'restaurant',
    website_url VARCHAR(500),
    phone VARCHAR(20),
    email VARCHAR(255) UNIQUE,
    address TEXT,
    city VARCHAR(100),
    state VARCHAR(100),
    country VARCHAR(100) DEFAULT 'India',
    pincode VARCHAR(10),
    gstin VARCHAR(20),
    fssai_license VARCHAR(50),
    is_active BOOLEAN DEFAULT true,
    subscription_plan VARCHAR(50) DEFAULT 'basic',
    subscription_expires_at TIMESTAMP,
    created_by INTEGER,
    updated_by INTEGER,
    created_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- 2. ROLES TABLE (Global - system-wide roles)
-- ============================================================================
CREATE TABLE IF NOT EXISTS roles (
    role_id SERIAL PRIMARY KEY,
    role_name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    is_system_role BOOLEAN DEFAULT false,
    created_by INTEGER,
    updated_by INTEGER,
    created_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- 3. PERMISSIONS TABLE (Global - system-wide permissions)
-- ============================================================================
CREATE TABLE IF NOT EXISTS permissions (
    permission_id SERIAL PRIMARY KEY,
    permission_name VARCHAR(100) NOT NULL UNIQUE,
    resource VARCHAR(100) NOT NULL,
    action VARCHAR(50) NOT NULL,
    description TEXT,
    created_by INTEGER,
    updated_by INTEGER,
    created_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- 4. ROLE PERMISSIONS MAPPING (Global)
-- ============================================================================
CREATE TABLE IF NOT EXISTS role_permissions (
    role_permission_id SERIAL PRIMARY KEY,
    role_id INTEGER NOT NULL REFERENCES roles(role_id) ON DELETE CASCADE,
    permission_id INTEGER NOT NULL REFERENCES permissions(permission_id) ON DELETE CASCADE,
    created_by INTEGER,
    updated_by INTEGER,
    created_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(role_id, permission_id)
);

-- ============================================================================
-- 5. USERS TABLE (Multitenant - includes merchant_id for data isolation)
-- ============================================================================
CREATE TABLE IF NOT EXISTS users (
    user_id SERIAL PRIMARY KEY,
    merchant_id INTEGER REFERENCES merchants(merchant_id) ON DELETE CASCADE,
    phone VARCHAR(20) NOT NULL,
    email VARCHAR(255),
    username VARCHAR(100),
    password_hash VARCHAR(255),
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    date_of_birth DATE,
    gender VARCHAR(10) CHECK (gender IN ('male', 'female', 'other')),
    address TEXT,
    profile_image_url VARCHAR(500),
    user_type VARCHAR(20) DEFAULT 'customer' CHECK (user_type IN ('super_admin', 'merchant', 'customer')),
    is_active BOOLEAN DEFAULT true,
    is_verified BOOLEAN DEFAULT false,
    phone_verified BOOLEAN DEFAULT false,
    email_verified BOOLEAN DEFAULT false,
    last_login_at TIMESTAMP,
    
    -- Password reset functionality
    password_reset_token VARCHAR(255),
    password_reset_expires_at TIMESTAMP,
    
    -- Email verification
    email_verification_token VARCHAR(255),
    email_verified_at TIMESTAMP,
    
    -- OTP authentication fields
    otp_code VARCHAR(6),
    otp_expires_at TIMESTAMP,
    otp_attempts INTEGER DEFAULT 0,
    otp_blocked_until TIMESTAMP,
    
    -- Guest user fields (only for customers)
    is_guest BOOLEAN DEFAULT false,
    guest_converted_at TIMESTAMP,
    
    -- Login preferences
    preferred_login_method VARCHAR(20) DEFAULT 'otp' CHECK (preferred_login_method IN ('password', 'otp', 'both')),
    
    created_by INTEGER,
    updated_by INTEGER,
    created_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- 6. USER ROLES MAPPING (Multitenant)
-- ============================================================================
CREATE TABLE IF NOT EXISTS user_roles (
    user_role_id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    role_id INTEGER NOT NULL REFERENCES roles(role_id) ON DELETE CASCADE,
    merchant_id INTEGER REFERENCES merchants(merchant_id) ON DELETE CASCADE,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,
    created_by INTEGER,
    updated_by INTEGER,
    created_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, role_id, merchant_id)
);

-- ============================================================================
-- 7. OTP LOGS TABLE (For tracking OTP attempts and security)
-- ============================================================================
CREATE TABLE IF NOT EXISTS otp_logs (
    otp_log_id SERIAL PRIMARY KEY,
    merchant_id INTEGER REFERENCES merchants(merchant_id) ON DELETE CASCADE,
    phone VARCHAR(20) NOT NULL,
    otp_code VARCHAR(6) NOT NULL,
    otp_type VARCHAR(20) DEFAULT 'login' CHECK (otp_type IN ('login', 'registration', 'password_reset', 'phone_verification')),
    status VARCHAR(20) DEFAULT 'sent' CHECK (status IN ('sent', 'verified', 'expired', 'failed')),
    ip_address VARCHAR(45),
    user_agent TEXT,
    attempts_count INTEGER DEFAULT 0,
    verified_at TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    created_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);