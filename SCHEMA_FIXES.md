# Schema Fixes Applied

## Issues Fixed

### 1. Database Schema Alignment
- **Fixed OTP code length**: Changed from 4 digits to 6 digits in both `User` and `OtpLog` entities
- **Added CHECK constraints**: Added validation for `gender`, `user_type`, and `preferred_login_method` fields
- **Made fields non-nullable**: Updated `Permission.resource` and `Permission.action` to be NOT NULL
- **Added proper indexes**: Implemented multitenant unique constraints for phone, email, and username

### 2. Entity Improvements
- **User Entity**: Added proper table-level constraints and indexes for multitenancy
- **Permission Entity**: Made resource and action fields mandatory
- **OtpLog Entity**: Corrected OTP code length to match schema

### 3. Database Initialization
- **schema.sql**: Complete database schema with all tables, constraints, and indexes
- **data.sql**: Initial data with roles, permissions, and role-permission mappings
- **init-db.sql**: Database and user creation script

### 4. Configuration Updates
- **application.properties**: Updated to use schema and data initialization
- **OTP Service**: Updated to generate 6-digit OTPs instead of 4-digit

## Key Schema Features Implemented

### Multitenancy Support
- Phone numbers unique per merchant (same phone can exist across merchants)
- Email addresses unique per merchant (same email can exist across merchants)
- Super admin users (merchant_id = NULL) have global uniqueness

### Role-Based Access Control
- 5 system roles: super_admin, merchant_admin, merchant_manager, merchant_staff, customer
- Comprehensive permission system with resource-action mapping
- Role-permission assignments with proper constraints

### Security Features
- OTP-based authentication with 6-digit codes
- Password reset functionality
- Email verification system
- Guest user support for customers
- Login attempt tracking and blocking

### Data Integrity
- Proper foreign key relationships
- CHECK constraints for enum-like fields
- Unique constraints for business rules
- Automatic timestamp management

## Database Setup

1. Run `init-db.sql` to create database and user
2. Update `application.properties` with your database credentials
3. Start the application - schema and data will be initialized automatically

## Roles and Permissions

### Default Roles
- **super_admin**: Full system access
- **merchant_admin**: Full merchant access (cannot manage other merchants)
- **merchant_manager**: Limited admin access within merchant
- **merchant_staff**: Operational access within merchant
- **customer**: Order and profile access

### Permission Categories
- User management (users.*)
- Merchant management (merchants.*)
- Role management (roles.*)
- Order management (orders.*)
- Product management (products.*)
- Payment management (payments.*)

## API Authentication

The service supports:
- Password-based login
- OTP-based login (6-digit codes)
- Guest user registration
- JWT token authentication with refresh tokens