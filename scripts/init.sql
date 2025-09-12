-- Cloud Kitchen RBAC Database Schema
-- This script creates the complete database schema for the RBAC system

-- Create merchants table
CREATE TABLE IF NOT EXISTS merchants (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    phone VARCHAR(20) UNIQUE NOT NULL,
    address TEXT,
    city VARCHAR(100),
    state VARCHAR(100),
    pincode VARCHAR(10),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create roles table
CREATE TABLE IF NOT EXISTS roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL CHECK (name IN ('super_admin', 'merchant_admin', 'merchant_staff', 'customer')),
    description TEXT,
    is_system_role BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create permissions table
CREATE TABLE IF NOT EXISTS permissions (
    id BIGSERIAL PRIMARY KEY,
    resource VARCHAR(100) NOT NULL,
    action VARCHAR(50) NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(resource, action)
);

-- Create role_permissions table
CREATE TABLE IF NOT EXISTS role_permissions (
    id BIGSERIAL PRIMARY KEY,
    role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id BIGINT NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(role_id, permission_id)
);

-- Create users table
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    phone VARCHAR(20) NOT NULL,
    email VARCHAR(255),
    password VARCHAR(255),
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    user_type VARCHAR(20) NOT NULL CHECK (user_type IN ('super_admin', 'merchant', 'customer')),
    merchant_id BIGINT REFERENCES merchants(id) ON DELETE SET NULL,
    city VARCHAR(100),
    state VARCHAR(100),
    pincode VARCHAR(10),
    is_active BOOLEAN DEFAULT true,
    is_verified BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create user_roles table
CREATE TABLE IF NOT EXISTS user_roles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    merchant_id BIGINT REFERENCES merchants(id) ON DELETE CASCADE,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    assigned_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    UNIQUE(user_id, role_id, merchant_id)
);

-- Create otp_logs table
CREATE TABLE IF NOT EXISTS otp_logs (
    id BIGSERIAL PRIMARY KEY,
    phone VARCHAR(20) NOT NULL,
    otp_code VARCHAR(10) NOT NULL,
    purpose VARCHAR(50) NOT NULL,
    ip_address VARCHAR(45),
    user_agent TEXT,
    is_used BOOLEAN DEFAULT false,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    used_at TIMESTAMP
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_users_phone ON users(phone);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_merchant_phone ON users(merchant_id, phone);
CREATE INDEX IF NOT EXISTS idx_users_type_merchant ON users(user_type, merchant_id);
CREATE INDEX IF NOT EXISTS idx_user_roles_user_merchant ON user_roles(user_id, merchant_id);
CREATE INDEX IF NOT EXISTS idx_otp_logs_phone_purpose ON otp_logs(phone, purpose);
CREATE INDEX IF NOT EXISTS idx_otp_logs_expires ON otp_logs(expires_at);

-- Insert default roles
INSERT INTO roles (name, description, is_system_role) VALUES
('super_admin', 'System Super Administrator', true),
('merchant_admin', 'Merchant Administrator', false),
('merchant_staff', 'Merchant Staff Member', false),
('customer', 'Customer User', false)
ON CONFLICT (name) DO NOTHING;

-- Insert default permissions
INSERT INTO permissions (resource, action, description) VALUES
('user', 'create', 'Create new users'),
('user', 'read', 'View user information'),
('user', 'update', 'Update user information'),
('user', 'delete', 'Delete users'),
('merchant', 'create', 'Create new merchants'),
('merchant', 'read', 'View merchant information'),
('merchant', 'update', 'Update merchant information'),
('merchant', 'delete', 'Delete merchants'),
('role', 'create', 'Create new roles'),
('role', 'read', 'View roles'),
('role', 'update', 'Update roles'),
('role', 'delete', 'Delete roles'),
('permission', 'read', 'View permissions'),
('order', 'create', 'Create orders'),
('order', 'read', 'View orders'),
('order', 'update', 'Update orders'),
('order', 'delete', 'Cancel orders')
ON CONFLICT (resource, action) DO NOTHING;