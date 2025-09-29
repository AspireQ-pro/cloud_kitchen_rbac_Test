# Cloud Kitchen RBAC Service - API Testing Guide

## 🚀 How to Test

1. **Start the application:**
   ```bash
   mvn spring-boot:run
   ```

2. **Run the test script:**
   ```bash
   test-apis.bat
   ```

## 📋 API Endpoints & Expected HTTP Status Codes

### 🔐 Authentication APIs

#### 1. User Registration
- **Endpoint:** `POST /api/auth/signup`
- **Success:** `201 Created` - User registered successfully
- **Conflict:** `409 Conflict` - Phone number already registered
- **Validation:** `400 Bad Request` - Invalid input data

#### 2. Customer Login
- **Endpoint:** `POST /api/auth/customer/login`
- **Success:** `200 OK` - Login successful
- **Unauthorized:** `401 Unauthorized` - Invalid credentials
- **Validation:** `400 Bad Request` - Missing/invalid parameters

#### 3. Merchant/Admin Login
- **Endpoint:** `POST /api/auth/login`
- **Success:** `200 OK` - Login successful
- **Unauthorized:** `401 Unauthorized` - Invalid credentials
- **Validation:** `400 Bad Request` - Missing/invalid parameters

#### 4. Token Refresh
- **Endpoint:** `POST /api/auth/refresh`
- **Success:** `200 OK` - Token refreshed
- **Unauthorized:** `401 Unauthorized` - Invalid/expired refresh token

#### 5. Logout
- **Endpoint:** `POST /api/auth/logout`
- **Success:** `200 OK` - Logged out successfully
- **Unauthorized:** `401 Unauthorized` - Invalid token

### 📱 OTP APIs

#### 6. OTP Request
- **Endpoint:** `POST /api/auth/otp/request`
- **Success:** `200 OK` - OTP sent successfully
- **Rate Limited:** `429 Too Many Requests` - Rate limit exceeded
- **Not Found:** `404 Not Found` - User not found
- **Service Error:** `503 Service Unavailable` - SMS service down
- **Validation:** `400 Bad Request` - Missing OTP type

#### 7. OTP Verification
- **Endpoint:** `POST /api/auth/otp/verify`
- **Success:** `200 OK` - OTP verified successfully
- **Invalid:** `400 Bad Request` - Invalid/expired OTP
- **Not Found:** `404 Not Found` - User not found

#### 8. Password Reset
- **Endpoint:** `POST /api/auth/password/reset`
- **Success:** `200 OK` - Password reset successfully
- **Validation:** `400 Bad Request` - Invalid OTP or password

### 👥 User Management APIs (Requires Authentication)

#### 9. List Users
- **Endpoint:** `GET /api/users`
- **Success:** `200 OK` - Users retrieved
- **Unauthorized:** `401 Unauthorized` - No valid token
- **Forbidden:** `403 Forbidden` - Insufficient permissions

#### 10. Get User Details
- **Endpoint:** `GET /api/users/{id}`
- **Success:** `200 OK` - User details retrieved
- **Not Found:** `404 Not Found` - User not found
- **Forbidden:** `403 Forbidden` - Access denied

#### 11. Create User
- **Endpoint:** `POST /api/users`
- **Success:** `201 Created` - User created
- **Validation:** `400 Bad Request` - Invalid data
- **Forbidden:** `403 Forbidden` - Insufficient permissions

#### 12. Update User
- **Endpoint:** `PUT /api/users/{id}`
- **Success:** `200 OK` - User updated
- **Not Found:** `404 Not Found` - User not found
- **Validation:** `400 Bad Request` - Invalid data
- **Forbidden:** `403 Forbidden` - Access denied

#### 13. Delete User
- **Endpoint:** `DELETE /api/users/{id}`
- **Success:** `200 OK` - User deleted
- **Not Found:** `404 Not Found` - User not found
- **Forbidden:** `403 Forbidden` - Access denied

### 🏪 Merchant Management APIs (Requires Authentication)

#### 14. List Merchants
- **Endpoint:** `GET /api/merchants`
- **Success:** `200 OK` - Merchants retrieved
- **Forbidden:** `403 Forbidden` - Super admin only

#### 15. Get Merchant Details
- **Endpoint:** `GET /api/merchants/{id}`
- **Success:** `200 OK` - Merchant details retrieved
- **Not Found:** `404 Not Found` - Merchant not found

#### 16. Create Merchant
- **Endpoint:** `POST /api/merchants`
- **Success:** `201 Created` - Merchant created
- **Validation:** `400 Bad Request` - Invalid data
- **Forbidden:** `403 Forbidden` - Super admin only

#### 17. Update Merchant
- **Endpoint:** `PUT /api/merchants/{id}`
- **Success:** `200 OK` - Merchant updated
- **Not Found:** `404 Not Found` - Merchant not found
- **Forbidden:** `403 Forbidden` - Access denied

#### 18. Delete Merchant
- **Endpoint:** `DELETE /api/merchants/{id}`
- **Success:** `200 OK` - Merchant deleted
- **Not Found:** `404 Not Found` - Merchant not found
- **Forbidden:** `403 Forbidden` - Super admin only

### 🛠️ System APIs

#### 19. Health Check
- **Endpoint:** `GET /actuator/health`
- **Success:** `200 OK` - System healthy

#### 20. API Documentation
- **Endpoint:** `GET /swagger-ui.html`
- **Success:** `200 OK` - Documentation loaded

## 🔑 Authentication Headers

For protected endpoints, include the JWT token:
```
Authorization: Bearer <access_token>
```

## 📝 Sample Test Data

### Customer Registration:
```json
{
  "merchantId": 1,
  "phone": "9921607907",
  "password": "Yogesh@1234",
  "firstName": "John",
  "lastName": "Doe",
  "email": "john@example.com",
  "address": "123 Main Street"
}
```

### Merchant Registration:
```json
{
  "merchantId": 0,
  "phone": "9876543221",
  "password": "Admin@1234",
  "firstName": "Admin",
  "lastName": "User",
  "email": "admin@example.com",
  "address": "Admin Address"
}
```

### OTP Request:
```json
{
  "phone": "9921607907",
  "merchantId": 1,
  "otpType": "password_reset"
}
```

## 🎯 Key Features Tested

1. **User Registration** - Customer (merchantId > 0) and Merchant (merchantId = 0)
2. **Authentication** - Password-based login with proper JWT tokens
3. **OTP System** - 4-digit OTP with type-based expiry and rate limiting
4. **Validation** - Comprehensive input validation with proper error messages
5. **Security** - Proper HTTP status codes and error handling
6. **CRUD Operations** - Full user and merchant management
7. **Authorization** - Role-based access control

## 🚨 Important Notes

- All APIs return proper HTTP status codes
- Phone numbers must be 10-digit Indian mobile numbers (6-9 prefix)
- Passwords must meet complexity requirements
- OTP system uses 4-digit codes with type-specific expiry
- Rate limiting prevents abuse
- All sensitive data is properly masked in logs