# EC2 Deployment Guide

## Server Details
- **EC2 IP**: 65.0.92.207
- **Application Port**: 8081
- **Swagger UI**: http://65.0.92.207:8081/swagger-ui/index.html

## Quick Test Commands for EC2

### 1. Health Check
```bash
curl http://65.0.92.207:8081/actuator/health
```

### 2. User Registration
```bash
curl -X POST "http://65.0.92.207:8081/api/auth/signup" \
  -H "Content-Type: application/json" \
  -d '{
    "merchantId": 1,
    "phone": "9876543210",
    "password": "SecurePass123!",
    "firstName": "John",
    "lastName": "Doe",
    "email": "john@example.com",
    "address": "123 Main Street"
  }'
```

### 3. OTP Request
```bash
curl -X POST "http://65.0.92.207:8081/api/auth/otp/request" \
  -H "Content-Type: application/json" \
  -d '{
    "merchantId": 1,
    "phone": "9876543210",
    "otpType": "login"
  }'
```

### 4. Customer Login
```bash
curl -X POST "http://65.0.92.207:8081/api/auth/customer/login" \
  -H "Content-Type: application/json" \
  -d '{
    "phone": "9876543210",
    "password": "SecurePass123!",
    "merchantId": 1
  }'
```

## Environment Configuration for EC2

Create `.env` file on EC2:
```bash
# Database Configuration
DATABASE_URL=jdbc:postgresql://65.0.92.207:5432/cloud_kitchen_rbac
DB_USERNAME=postgres
DB_PASSWORD=aspire123

# JWT Configuration
JWT_SECRET=2M7w1SiV5XDAXEmVqg9gZWCXfF50l11FqJR2aSC1a8K9mN3pQ7rT2uV8wX1yZ4bC6dE9fG2hJ5kL8mN1oP4qR7sT3uV6wX9yZ2
JWT_ACCESS_EXPIRY=3600
JWT_REFRESH_EXPIRY=604800

# CORS Configuration
CORS_ORIGINS=http://localhost:3000,http://65.0.92.207:8081,https://65.0.92.207:8081

# Application Port
PORT=8081
```

## API Endpoints Available

- **Swagger UI**: http://65.0.92.207:8081/swagger-ui/index.html
- **Health Check**: http://65.0.92.207:8081/actuator/health
- **User Registration**: POST http://65.0.92.207:8081/api/auth/signup
- **OTP Request**: POST http://65.0.92.207:8081/api/auth/otp/request
- **Customer Login**: POST http://65.0.92.207:8081/api/auth/customer/login
- **Merchant Login**: POST http://65.0.92.207:8081/api/auth/login

## Security Notes
- Ensure PostgreSQL is running on port 5432
- Database should have the required schema
- JWT secret is production-ready (256-bit)
- CORS is configured for your EC2 IP