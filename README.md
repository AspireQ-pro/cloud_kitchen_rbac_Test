# Cloud Kitchen RBAC Service

Role-Based Access Control (RBAC) service for Cloud Kitchen application with JWT authentication.

## Features

- JWT-based authentication
- Role-based access control
- OTP verification
- Customer and merchant registration
- Password reset functionality
- Input validation and error handling

## Quick Start

```bash
# Clone repository
git clone <repository-url>
cd cloud-kitchen-rbac-service

# Run application
mvn spring-boot:run
```

## API Endpoints

### Authentication
- `POST /api/auth/signup` - Customer registration
- `POST /api/auth/login` - Merchant/admin login
- `POST /api/auth/customer/login` - Customer login
- `POST /api/auth/refresh` - Refresh JWT token
- `POST /api/auth/logout` - Logout

### OTP
- `POST /api/auth/otp/request` - Request OTP
- `POST /api/auth/otp/verify` - Verify OTP

## Configuration

Set environment variables:
```bash
JWT_SECRET=your-jwt-secret-key-minimum-32-characters
DB_URL=jdbc:postgresql://localhost:5432/cloudkitchen
DB_USERNAME=your-db-username
DB_PASSWORD=your-db-password
```

## Database Schema

Required table: `users` with column `otp_used BOOLEAN DEFAULT FALSE`

## Technology Stack

- Spring Boot 3.x
- Spring Security
- JWT
- PostgreSQL
- Maven