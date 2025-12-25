-- ============================================================================
-- CLOUD KITCHEN RBAC SERVICE DATABASE SCHEMA
-- ============================================================================
-- Multitenant Role-Based Access Control for Cloud Kitchen Application
-- Supporting roles: Super Admin, Merchant, Customer
-- Version: 1.0.0
-- ============================================================================

-- ============================================================================
-- 1. MERCHANTS TABLE (Global - no merchant_id needed)
-- ============================================================================
CREATE TABLE merchants (
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
CREATE TABLE roles (
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
CREATE TABLE permissions (
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
CREATE TABLE role_permissions (
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
CREATE TABLE users (
    user_id SERIAL PRIMARY KEY,
    merchant_id INTEGER REFERENCES merchants(merchant_id) ON DELETE CASCADE,
    phone VARCHAR(20) NOT NULL,
    email VARCHAR(255),
    username VARCHAR(100),
    password_hash VARCHAR(255),
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    address VARCHAR(250),
    favorite_food VARCHAR(100),
    dietary_preferences VARCHAR(200),
    date_of_birth DATE,
    gender VARCHAR(10) CHECK (gender IN ('male', 'female', 'other')),
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
    otp_code VARCHAR(4),
    otp_expires_at TIMESTAMP,
    otp_attempts INTEGER DEFAULT 0,
    otp_blocked_until TIMESTAMP,
    otp_used BOOLEAN DEFAULT false,

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
CREATE TABLE user_roles (
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
CREATE TABLE otp_logs (
    otp_log_id SERIAL PRIMARY KEY,
    merchant_id INTEGER REFERENCES merchants(merchant_id) ON DELETE CASCADE,
    phone VARCHAR(20) NOT NULL,
    otp_code VARCHAR(4) NOT NULL,
    otp_type VARCHAR(20) DEFAULT 'login' CHECK (otp_type IN ('login', 'registration', 'password_reset', 'phone_verification')),
    status VARCHAR(20) DEFAULT 'sent' CHECK (status IN ('sent', 'verified', 'expired', 'failed')),
    ip_address VARCHAR(45),
    user_agent TEXT,
    attempts_count INTEGER DEFAULT 0,
    verified_at TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    created_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- 8. CUSTOMERS TABLE (Merchant-scoped business entity)
-- ============================================================================
CREATE TABLE customers (
    customer_id SERIAL PRIMARY KEY,
    merchant_id INTEGER NOT NULL REFERENCES merchants(merchant_id) ON DELETE CASCADE,
    user_id INTEGER REFERENCES users(user_id) ON DELETE SET NULL,
    phone VARCHAR(20) NOT NULL,
    email VARCHAR(255),
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    address TEXT,
    city VARCHAR(100),
    state VARCHAR(100),
    country VARCHAR(100) DEFAULT 'India',
    pincode VARCHAR(10),
    dob DATE,
    favorite_food VARCHAR(255),
    is_active BOOLEAN DEFAULT true,
    deleted_at TIMESTAMP,
    created_by INTEGER,
    updated_by INTEGER,
    created_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_customer_phone_not_empty CHECK (phone <> ''),
    CONSTRAINT uq_customer_merchant_phone UNIQUE (merchant_id, phone)
);

-- ============================================================================
-- INDEXES FOR PERFORMANCE
-- ============================================================================

-- Merchants indexes
CREATE INDEX idx_merchants_email ON merchants(email);
CREATE INDEX idx_merchants_active ON merchants(is_active);

-- Users indexes
CREATE INDEX idx_users_merchant_id ON users(merchant_id);
CREATE INDEX idx_users_phone ON users(phone);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_active ON users(is_active);
CREATE INDEX idx_users_user_type ON users(user_type);
CREATE INDEX idx_users_merchant_phone ON users(merchant_id, phone);
CREATE INDEX idx_users_merchant_email ON users(merchant_id, email);
CREATE INDEX idx_users_is_guest ON users(is_guest);
CREATE INDEX idx_users_otp_expires ON users(otp_expires_at);
CREATE INDEX idx_users_phone_verified ON users(phone_verified);

-- User roles indexes
CREATE INDEX idx_user_roles_user_id ON user_roles(user_id);
CREATE INDEX idx_user_roles_role_id ON user_roles(role_id);
CREATE INDEX idx_user_roles_merchant_id ON user_roles(merchant_id);
CREATE INDEX idx_user_roles_merchant_user ON user_roles(merchant_id, user_id);

-- Role permissions indexes
CREATE INDEX idx_role_permissions_role_id ON role_permissions(role_id);
CREATE INDEX idx_role_permissions_permission_id ON role_permissions(permission_id);

-- Permissions indexes
CREATE INDEX idx_permissions_resource_action ON permissions(resource, action);

-- OTP logs indexes
CREATE INDEX idx_otp_logs_merchant_id ON otp_logs(merchant_id);
CREATE INDEX idx_otp_logs_phone ON otp_logs(phone);
CREATE INDEX idx_otp_logs_phone_status ON otp_logs(phone, status);
CREATE INDEX idx_otp_logs_expires_at ON otp_logs(expires_at);
CREATE INDEX idx_otp_logs_created_on ON otp_logs(created_on);
CREATE INDEX idx_otp_logs_merchant_phone ON otp_logs(merchant_id, phone);

-- Customers indexes
CREATE INDEX idx_customers_merchant_id ON customers(merchant_id);
CREATE INDEX idx_customers_phone ON customers(phone);
CREATE INDEX idx_customers_user_id ON customers(user_id);
CREATE INDEX idx_customers_merchant_active ON customers(merchant_id, is_active) WHERE deleted_at IS NULL;

-- ============================================================================
-- CONSTRAINTS AND UNIQUE INDEXES FOR MULTITENANCY
-- ============================================================================

-- Ensure phone is unique per merchant (allow same phone across merchants)
CREATE UNIQUE INDEX idx_users_merchant_phone_unique ON users(merchant_id, phone);

-- Ensure email is unique per merchant if provided (allow same email across merchants)
CREATE UNIQUE INDEX idx_users_merchant_email_unique ON users(merchant_id, email) WHERE email IS NOT NULL;

-- Ensure username is unique per merchant if provided (allow same username across merchants)
CREATE UNIQUE INDEX idx_users_merchant_username_unique ON users(merchant_id, username) WHERE username IS NOT NULL;

-- For super admin users (merchant_id is NULL), ensure global uniqueness
CREATE UNIQUE INDEX idx_users_global_phone_unique ON users(phone) WHERE merchant_id IS NULL;
CREATE UNIQUE INDEX idx_users_global_email_unique ON users(email) WHERE merchant_id IS NULL AND email IS NOT NULL;
CREATE UNIQUE INDEX idx_users_global_username_unique ON users(username) WHERE merchant_id IS NULL AND username IS NOT NULL;

-- ============================================================================
-- BUSINESS LOGIC CONSTRAINTS
-- ============================================================================

-- Ensure only customers can be guest users
ALTER TABLE users ADD CONSTRAINT chk_guest_user_type
CHECK (is_guest = false OR (is_guest = true AND user_type = 'customer'));

-- Ensure guest users belong to a merchant (not super admin)
ALTER TABLE users ADD CONSTRAINT chk_guest_merchant
CHECK (is_guest = false OR (is_guest = true AND merchant_id IS NOT NULL));

-- ============================================================================
-- TRIGGERS FOR AUTOMATIC TIMESTAMP UPDATES
-- ============================================================================

-- Function to update timestamp
CREATE OR REPLACE FUNCTION update_updated_on_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_on = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create triggers for all tables
CREATE TRIGGER update_merchants_updated_on BEFORE UPDATE ON merchants FOR EACH ROW EXECUTE FUNCTION update_updated_on_column();
CREATE TRIGGER update_roles_updated_on BEFORE UPDATE ON roles FOR EACH ROW EXECUTE FUNCTION update_updated_on_column();
CREATE TRIGGER update_permissions_updated_on BEFORE UPDATE ON permissions FOR EACH ROW EXECUTE FUNCTION update_updated_on_column();
CREATE TRIGGER update_role_permissions_updated_on BEFORE UPDATE ON role_permissions FOR EACH ROW EXECUTE FUNCTION update_updated_on_column();
CREATE TRIGGER update_users_updated_on BEFORE UPDATE ON users FOR EACH ROW EXECUTE FUNCTION update_updated_on_column();
CREATE TRIGGER update_user_roles_updated_on BEFORE UPDATE ON user_roles FOR EACH ROW EXECUTE FUNCTION update_updated_on_column();
CREATE TRIGGER update_customers_updated_on BEFORE UPDATE ON customers FOR EACH ROW EXECUTE FUNCTION update_updated_on_column();

-- ============================================================================
-- UTILITY FUNCTIONS FOR AUTHENTICATION
-- ============================================================================

-- Function to generate OTP
CREATE OR REPLACE FUNCTION generate_otp()
RETURNS VARCHAR(6) AS $$
BEGIN
    RETURN LPAD(FLOOR(RANDOM() * 1000000)::TEXT, 6, '0');
END;
$$ LANGUAGE plpgsql;

-- Function to check if phone is blocked due to too many OTP attempts
CREATE OR REPLACE FUNCTION is_phone_blocked(p_phone VARCHAR, p_merchant_id INTEGER)
RETURNS BOOLEAN AS $$
DECLARE
    blocked_until TIMESTAMP;
BEGIN
    SELECT otp_blocked_until INTO blocked_until
    FROM users
    WHERE phone = p_phone AND merchant_id = p_merchant_id;

    RETURN blocked_until IS NOT NULL AND blocked_until > CURRENT_TIMESTAMP;
END;
$$ LANGUAGE plpgsql;

-- Function to clean expired OTPs (run this periodically)
CREATE OR REPLACE FUNCTION cleanup_expired_otps()
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
BEGIN
    DELETE FROM otp_logs WHERE expires_at < CURRENT_TIMESTAMP;
    GET DIAGNOSTICS deleted_count = ROW_COUNT;

    UPDATE users SET
        otp_code = NULL,
        otp_expires_at = NULL,
        otp_attempts = 0
    WHERE otp_expires_at < CURRENT_TIMESTAMP;

    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- INITIAL DATA INSERTION
-- ============================================================================

-- Insert default system roles
INSERT INTO roles (role_name, description, is_system_role, created_by) VALUES
('super_admin', 'Super Administrator with full system access', true, 1),
('merchant_admin', 'Merchant Administrator with full merchant access', true, 1),
('merchant_manager', 'Merchant Manager with limited admin access', true, 1),
('merchant_staff', 'Merchant Staff with operational access', true, 1),
('customer', 'Customer with order and profile access', true, 1);

-- Insert default permissions
INSERT INTO permissions (permission_name, resource, action, description, created_by) VALUES
-- User management
('users.create', 'users', 'create', 'Create new users', 1),
('users.read', 'users', 'read', 'View user details', 1),
('users.update', 'users', 'update', 'Update user information', 1),
('users.delete', 'users', 'delete', 'Delete users', 1),

-- Merchant management
('merchants.create', 'merchants', 'create', 'Create new merchants', 1),
('merchants.read', 'merchants', 'read', 'View merchant details', 1),
('merchants.update', 'merchants', 'update', 'Update merchant information', 1),
('merchants.delete', 'merchants', 'delete', 'Delete merchants', 1),

-- Role management
('roles.create', 'roles', 'create', 'Create new roles', 1),
('roles.read', 'roles', 'read', 'View roles', 1),
('roles.update', 'roles', 'update', 'Update roles', 1),
('roles.delete', 'roles', 'delete', 'Delete roles', 1),
('roles.assign', 'roles', 'assign', 'Assign roles to users', 1),

-- Order management (for future integration)
('orders.create', 'orders', 'create', 'Create new orders', 1),
('orders.read', 'orders', 'read', 'View orders', 1),
('orders.update', 'orders', 'update', 'Update order status', 1),
('orders.delete', 'orders', 'delete', 'Cancel/delete orders', 1),

-- Product management (for future integration)
('products.create', 'products', 'create', 'Create new products', 1),
('products.read', 'products', 'read', 'View products', 1),
('products.update', 'products', 'update', 'Update product information', 1),
('products.delete', 'products', 'delete', 'Delete products', 1),

-- Payment management (for future integration)
('payments.read', 'payments', 'read', 'View payment information', 1),
('payments.process', 'payments', 'process', 'Process payments', 1),
('payments.refund', 'payments', 'refund', 'Process refunds', 1);

-- Assign permissions to super_admin role (gets all permissions)
INSERT INTO role_permissions (role_id, permission_id, created_by)
SELECT 1, permission_id, 1 FROM permissions;

-- Assign permissions to merchant_admin role
INSERT INTO role_permissions (role_id, permission_id, created_by)
SELECT 2, permission_id, 1 FROM permissions
WHERE permission_name NOT LIKE 'merchants.%' OR permission_name = 'merchants.read';

-- Assign permissions to customer role
INSERT INTO role_permissions (role_id, permission_id, created_by)
SELECT 5, permission_id, 1 FROM permissions
WHERE permission_name IN ('orders.create', 'orders.read', 'products.read', 'users.update');
