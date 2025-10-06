-- Initialize Cloud Kitchen RBAC Database
-- This script runs when PostgreSQL container starts

-- Set timezone for database
SET timezone = 'Asia/Kolkata';

-- Create database if not exists (handled by POSTGRES_DB env var)
-- CREATE DATABASE IF NOT EXISTS cloud_kitchen_rbac;

-- Grant permissions
GRANT ALL PRIVILEGES ON DATABASE cloud_kitchen_rbac TO postgres;

-- Create extensions if needed
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Set default timezone for all sessions
ALTER DATABASE cloud_kitchen_rbac SET timezone TO 'Asia/Kolkata';