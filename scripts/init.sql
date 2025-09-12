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
INSERT INTO roles (role_name, description, is_system_role, created_by) VALUES
('super_admin', 'Super Administrator with full system access', true, 1),
('merchant_admin', 'Merchant Administrator with full merchant access', true, 1),
('merchant_manager', 'Merchant Manager with limited admin access', true, 1),
('merchant_staff', 'Merchant Staff with operational access', true, 1),
('customer', 'Customer with order and profile access', true, 1)
ON CONFLICT (role_name) DO NOTHING;

-- Insert default permissions
INSERT INTO permissions (permission_name, resource, action, description, created_by) VALUES
('users.create', 'users', 'create', 'Create new users', 1),
('users.read', 'users', 'read', 'View user details', 1),
('users.update', 'users', 'update', 'Update user information', 1),
('users.delete', 'users', 'delete', 'Delete users', 1),
('merchants.create', 'merchants', 'create', 'Create new merchants', 1),
('merchants.read', 'merchants', 'read', 'View merchant details', 1),
('merchants.update', 'merchants', 'update', 'Update merchant information', 1),
('merchants.delete', 'merchants', 'delete', 'Delete merchants', 1),
('roles.create', 'roles', 'create', 'Create new roles', 1),
('roles.read', 'roles', 'read', 'View roles', 1),
('roles.update', 'roles', 'update', 'Update roles', 1),
('roles.delete', 'roles', 'delete', 'Delete roles', 1),
('roles.assign', 'roles', 'assign', 'Assign roles to users', 1),
('orders.create', 'orders', 'create', 'Create new orders', 1),
('orders.read', 'orders', 'read', 'View orders', 1),
('orders.update', 'orders', 'update', 'Update order status', 1),
('orders.delete', 'orders', 'delete', 'Cancel/delete orders', 1),
('products.create', 'products', 'create', 'Create new products', 1),
('products.read', 'products', 'read', 'View products', 1),
('products.update', 'products', 'update', 'Update product information', 1),
('products.delete', 'products', 'delete', 'Delete products', 1),
('payments.read', 'payments', 'read', 'View payment information', 1),
('payments.process', 'payments', 'process', 'Process payments', 1),
('payments.refund', 'payments', 'refund', 'Process refunds', 1)
ON CONFLICT (permission_name) DO NOTHING;