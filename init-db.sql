-- Cloud Kitchen RBAC Database Initialization
-- This script creates the necessary database schema for the RBAC service

-- Create database if it doesn't exist (handled by POSTGRES_DB env var)
-- CREATE DATABASE IF NOT EXISTS cloud_kitchen_rbac;

-- Set timezone
SET timezone = 'Asia/Kolkata';

-- Enable necessary extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create schema comment
COMMENT ON DATABASE cloud_kitchen_rbac IS 'Cloud Kitchen RBAC Service Database - Multi-tenant Role-Based Access Control';

-- The actual table creation will be handled by Hibernate DDL auto-update
-- This file ensures the database is properly initialized with extensions and settings