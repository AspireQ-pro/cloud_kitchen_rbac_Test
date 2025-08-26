-- ============================================================================
-- INITIAL DATA INSERTION
-- ============================================================================

-- Insert default system roles
INSERT INTO roles (role_name, description, is_system_role, created_by) VALUES
('super_admin', 'Super Administrator with full system access', true, 1),
('merchant_admin', 'Merchant Administrator with full merchant access', true, 1),
('merchant_manager', 'Merchant Manager with limited admin access', true, 1),
('merchant_staff', 'Merchant Staff with operational access', true, 1),
('customer', 'Customer with order and profile access', true, 1)
ON CONFLICT (role_name) DO NOTHING;

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
('payments.refund', 'payments', 'refund', 'Process refunds', 1)
ON CONFLICT (permission_name) DO NOTHING;

-- Assign permissions to super_admin role (gets all permissions)
INSERT INTO role_permissions (role_id, permission_id, created_by)
SELECT 1, permission_id, 1 FROM permissions
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- Assign permissions to merchant_admin role
INSERT INTO role_permissions (role_id, permission_id, created_by)
SELECT 2, permission_id, 1 FROM permissions 
WHERE permission_name NOT LIKE 'merchants.%' OR permission_name = 'merchants.read'
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- Assign permissions to customer role
INSERT INTO role_permissions (role_id, permission_id, created_by)
SELECT 5, permission_id, 1 FROM permissions 
WHERE permission_name IN ('orders.create', 'orders.read', 'products.read', 'users.update')
ON CONFLICT (role_id, permission_id) DO NOTHING;