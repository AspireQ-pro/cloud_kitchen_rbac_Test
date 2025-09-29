# Cloud Kitchen RBAC Service - Curl Test Commands

## 🚀 Prerequisites
1. Start the application: `mvn spring-boot:run`
2. Ensure PostgreSQL is running and accessible
3. Run tests: `test-all-apis.bat`

## 📋 Individual Curl Commands

### 1. Health Check
```bash
curl -X GET "http://localhost:8081/actuator/health"
```
**Expected:** `200 OK` - `{"status":"UP"}`

### 2. User Registration (Customer)
```bash
curl -X POST "http://localhost:8081/api/auth/signup" \
  -H "Content-Type: application/json" \
  -d '{
    "merchantId": 1,
    "phone": "9921607907",
    "password": "Yogesh@1234",
    "firstName": "John",
    "lastName": "Doe",
    "email": "john@example.com",
    "address": "123 Main Street"
  }'
```
**Expected:** `201 Created` - Returns JWT tokens

### 3. Duplicate Registration
```bash
curl -X POST "http://localhost:8081/api/auth/signup" \
  -H "Content-Type: application/json" \
  -d '{
    "merchantId": 1,
    "phone": "9921607907",
    "password": "Yogesh@1234",
    "firstName": "John",
    "lastName": "Doe",
    "email": "john@example.com",
    "address": "123 Main Street"
  }'
```
**Expected:** `409 Conflict` - Phone already registered

### 4. Customer Login
```bash
curl -X POST "http://localhost:8081/api/auth/customer/login" \
  -H "Content-Type: application/json" \
  -d '{
    "phone": "9921607907",
    "password": "Yogesh@1234",
    "merchantId": 1
  }'
```
**Expected:** `200 OK` - Returns JWT tokens

### 5. Invalid Login
```bash
curl -X POST "http://localhost:8081/api/auth/customer/login" \
  -H "Content-Type: application/json" \
  -d '{
    "phone": "9921607907",
    "password": "WrongPassword",
    "merchantId": 1
  }'
```
**Expected:** `401 Unauthorized` - Invalid credentials

### 6. OTP Request - Password Reset
```bash
curl -X POST "http://localhost:8081/api/auth/otp/request" \
  -H "Content-Type: application/json" \
  -d '{
    "phone": "9921607907",
    "merchantId": 1,
    "otpType": "password_reset"
  }'
```
**Expected:** `200 OK` - OTP sent (4-digit)

### 7. OTP Request - Login
```bash
curl -X POST "http://localhost:8081/api/auth/otp/request" \
  -H "Content-Type: application/json" \
  -d '{
    "phone": "9921607907",
    "merchantId": 1,
    "otpType": "login"
  }'
```
**Expected:** `200 OK` - OTP sent (4-digit)

### 8. OTP Verification
```bash
curl -X POST "http://localhost:8081/api/auth/otp/verify" \
  -H "Content-Type: application/json" \
  -d '{
    "phone": "9921607907",
    "merchantId": 1,
    "otp": "1234"
  }'
```
**Expected:** `400 Bad Request` - Invalid OTP (unless you use actual OTP)

### 9. Merchant Registration
```bash
curl -X POST "http://localhost:8081/api/auth/signup" \
  -H "Content-Type: application/json" \
  -d '{
    "merchantId": 0,
    "phone": "9876543221",
    "password": "Admin@1234",
    "firstName": "Admin",
    "lastName": "User",
    "email": "admin@example.com",
    "address": "Admin Address"
  }'
```
**Expected:** `201 Created` - Merchant user created

### 10. Merchant Login
```bash
curl -X POST "http://localhost:8081/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "phone": "9876543221",
    "password": "Admin@1234",
    "merchantId": 0
  }'
```
**Expected:** `200 OK` - Returns JWT tokens with merchant_admin role

### 11. Validation Error - Invalid Phone
```bash
curl -X POST "http://localhost:8081/api/auth/signup" \
  -H "Content-Type: application/json" \
  -d '{
    "merchantId": 1,
    "phone": "123",
    "password": "Yogesh@1234",
    "firstName": "John",
    "lastName": "Doe",
    "email": "john@example.com",
    "address": "123 Main Street"
  }'
```
**Expected:** `400 Bad Request` - Validation error

### 12. Missing Required Fields
```bash
curl -X POST "http://localhost:8081/api/auth/signup" \
  -H "Content-Type: application/json" \
  -d '{
    "merchantId": 1,
    "phone": "9876543222"
  }'
```
**Expected:** `400 Bad Request` - Missing required fields

### 13. OTP Request without Type
```bash
curl -X POST "http://localhost:8081/api/auth/otp/request" \
  -H "Content-Type: application/json" \
  -d '{
    "phone": "9921607907",
    "merchantId": 1
  }'
```
**Expected:** `400 Bad Request` - OTP type required

### 14. Invalid JSON
```bash
curl -X POST "http://localhost:8081/api/auth/signup" \
  -H "Content-Type: application/json" \
  -d '{invalid json'
```
**Expected:** `400 Bad Request` - Invalid request format

## 🔐 Authenticated Endpoints

### Get Access Token First:
```bash
TOKEN=$(curl -s -X POST "http://localhost:8081/api/auth/customer/login" \
  -H "Content-Type: application/json" \
  -d '{
    "phone": "9921607907",
    "password": "Yogesh@1234",
    "merchantId": 1
  }' | jq -r '.data.accessToken')
```

### 15. List Users
```bash
curl -X GET "http://localhost:8081/api/users" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json"
```
**Expected:** `200 OK` or `403 Forbidden` (based on role)

### 16. Get User Details
```bash
curl -X GET "http://localhost:8081/api/users/1" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json"
```
**Expected:** `200 OK` or `404 Not Found`

### 17. List Merchants
```bash
curl -X GET "http://localhost:8081/api/merchants" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json"
```
**Expected:** `200 OK` or `403 Forbidden` (super_admin only)

### 18. Unauthorized Access
```bash
curl -X GET "http://localhost:8081/api/users" \
  -H "Content-Type: application/json"
```
**Expected:** `401 Unauthorized` - No token provided

### 19. Invalid Token
```bash
curl -X GET "http://localhost:8081/api/users" \
  -H "Authorization: Bearer invalid_token" \
  -H "Content-Type: application/json"
```
**Expected:** `401 Unauthorized` - Invalid token

### 20. Token Refresh
```bash
curl -X POST "http://localhost:8081/api/auth/refresh" \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "YOUR_REFRESH_TOKEN_HERE"
  }'
```
**Expected:** `200 OK` - New access token

### 21. Logout
```bash
curl -X POST "http://localhost:8081/api/auth/logout" \
  -H "Authorization: Bearer $TOKEN"
```
**Expected:** `200 OK` - Logged out successfully

## 🎯 Expected HTTP Status Codes Summary

| Scenario | Expected Code | Description |
|----------|---------------|-------------|
| Successful Registration | 201 | User created |
| Duplicate Registration | 409 | Phone already exists |
| Successful Login | 200 | Login successful |
| Invalid Credentials | 401 | Wrong password/user |
| Validation Error | 400 | Invalid input data |
| Missing Fields | 400 | Required fields missing |
| Invalid JSON | 400 | Malformed request |
| OTP Sent | 200 | OTP sent successfully |
| Invalid OTP | 400 | Wrong/expired OTP |
| Rate Limited | 429 | Too many requests |
| Unauthorized | 401 | No/invalid token |
| Forbidden | 403 | Insufficient permissions |
| Not Found | 404 | Resource not found |
| Server Error | 500 | Internal error |

## 🔧 Testing Tips

1. **Start Application:** Ensure the app is running on port 8081
2. **Database:** Verify PostgreSQL connection is working
3. **Sequential Testing:** Run registration before login tests
4. **Token Management:** Save tokens for authenticated endpoint testing
5. **Rate Limiting:** Wait between OTP requests to avoid rate limits
6. **Phone Numbers:** Use different phone numbers for different test scenarios

## 📱 OTP Testing

Since OTP is sent via SMS (mock service), you can:
1. Check application logs for generated OTP
2. Use the actual OTP from logs for verification tests
3. Test rate limiting by making multiple OTP requests

All APIs are designed with proper HTTP status codes and comprehensive error handling for production use!