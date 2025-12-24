# API Endpoints Overview

## Authentication Endpoints (auth.yaml)

### Public Endpoints (No Authentication Required)

| Method | Endpoint | Description | RBAC |
|--------|----------|-------------|------|
| POST | `/api/v1/auth/signup` | Customer registration | Public |
| POST | `/api/v1/auth/login` | Merchant/Admin login (merchantId=0) | Public |
| POST | `/api/v1/auth/customer/login` | Customer login (merchantId>0) | Public |
| POST | `/api/v1/auth/refresh` | Refresh access token | Public (valid refresh token) |
| POST | `/api/v1/auth/otp/request` | Request OTP | Public |
| POST | `/api/v1/auth/otp/verify` | Verify OTP | Public |

### Authenticated Endpoints

| Method | Endpoint | Description | RBAC |
|--------|----------|-------------|------|
| POST | `/api/v1/auth/logout` | Logout user | Any authenticated user |

---

## User Management Endpoints (users.yaml)

All endpoints require **ROLE_SUPER_ADMIN**

| Method | Endpoint | Description | Query Parameters |
|--------|----------|-------------|------------------|
| GET | `/api/v1/users` | List all users | page, size, sortBy, sortDirection, role, search |
| GET | `/api/v1/users/{id}` | Get user by ID | - |

**Features:**
- Pagination (0-indexed)
- Filtering by role
- Search by name, email, phone, username
- Sorting (ascending/descending)

---

## Customer Management Endpoints (customers.yaml)

All endpoints require authentication. Access control based on merchantId and permissions.

| Method | Endpoint | Description | RBAC Rules |
|--------|----------|-------------|------------|
| PUT | `/api/customers/{id}` | Update customer profile | Own profile or merchant permissions |
| GET | `/api/customers/{id}` | Get customer by ID | Own profile or merchant permissions |
| GET | `/api/customers` | List all customers | Merchant scope access |
| GET | `/api/customers/profile` | Get current customer profile | Own profile only |
| GET | `/api/customers/merchant/{merchantId}` | Get customers by merchant | Merchant access required |

**Features:**
- Pagination support
- Filter by status
- Search functionality
- Merchant-scoped access

---

## Merchant Management Endpoints (merchants.yaml)

| Method | Endpoint | Description | RBAC Rules |
|--------|----------|-------------|------------|
| POST | `/api/v1/merchants` | Create merchant | ROLE_SUPER_ADMIN or merchants.create |
| GET | `/api/v1/merchants` | List all merchants | ROLE_SUPER_ADMIN or merchants.read |
| GET | `/api/v1/merchants/{id}` | Get merchant by ID | Super admin, merchants.read, or own merchant |
| PATCH | `/api/v1/merchants/{id}` | Update merchant | Super admin or own merchant |
| DELETE | `/api/v1/merchants/{id}` | Delete merchant | ROLE_SUPER_ADMIN only |

**Features:**
- Pagination support
- Filter by status
- Search by name, email, phone
- Sorting capabilities
- Creates associated user account

---

## Health Check Endpoints (health.yaml)

| Method | Endpoint | Description | RBAC |
|--------|----------|-------------|------|
| GET | `/api/v1/health/users` | Get user count | Public |

---

## HTTP Status Codes Reference

### Success Codes
- **200 OK** - Successful operation
- **201 Created** - Resource created successfully

### Client Error Codes
- **400 Bad Request** - Invalid input or validation failed
- **401 Unauthorized** - Missing or invalid authentication token
- **403 Forbidden** - Valid token but insufficient permissions
- **404 Not Found** - Resource not found
- **409 Conflict** - Resource already exists or conflict

### Rate Limiting
- **429 Too Many Requests** - Rate limit exceeded (OTP requests)

### Server Error Codes
- **500 Internal Server Error** - Unexpected server error
- **503 Service Unavailable** - Service temporarily unavailable (SMS service)

---

## Authentication Flow

### 1. Merchant/Admin Login Flow
```
Client → POST /api/v1/auth/login
{
  "merchantId": 0,
  "username": "yogesh_kitchen",
  "password": "SecurePass123!"
}

Server → 200 OK
{
  "status": 200,
  "message": "Login successful",
  "data": {
    "accessToken": "eyJhbGc...",
    "refreshToken": "eyJhbGc...",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "userId": 10,
    "merchantId": 0,
    "roles": ["ROLE_MERCHANT"]
  }
}

Client → All subsequent requests
Authorization: Bearer eyJhbGc...
```

### 2. Customer Login Flow
```
Client → POST /api/v1/auth/customer/login
{
  "merchantId": 1,
  "username": "9876543210",
  "password": "SecurePass123!"
}

Server → 200 OK
{
  "data": {
    "accessToken": "...",
    "userId": 123,
    "merchantId": 1,
    "customerId": 456,
    "roles": ["ROLE_CUSTOMER"]
  }
}
```

### 3. OTP Login Flow
```
Step 1: Request OTP
Client → POST /api/v1/auth/otp/request
{
  "merchantId": 0,
  "phone": "9876543210",
  "otpType": "login"
}

Server → 200 OK
{
  "status": 200,
  "message": "OTP sent successfully"
}

Step 2: Verify OTP
Client → POST /api/v1/auth/otp/verify
{
  "merchantId": 0,
  "phone": "9876543210",
  "otp": "1234",
  "otpType": "login"
}

Server → 200 OK
{
  "data": {
    "accessToken": "...",
    "refreshToken": "...",
    ...
  }
}
```

---

## MerchantId Convention

| Value | Context | Description |
|-------|---------|-------------|
| `null` | Super Admin | System-wide operations, no merchant scope |
| `0` | Merchant/Admin | Merchant login, general OTP requests |
| `> 0` | Customer | Customer operations for specific merchant |

---

## Pagination Parameters

All list endpoints support:

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `page` | integer | 0 | Page number (0-indexed) |
| `size` | integer | 10-20 | Items per page (1-100) |
| `sortBy` | string | - | Field to sort by |
| `sortDirection` | string | asc | Sort direction (asc/desc) |

---

## Search & Filter Parameters

### User Management
- `role` - Filter by role (e.g., "ROLE_MERCHANT")
- `search` - Search by name, email, phone, username

### Customer Management
- `status` - Filter by customer status
- `search` - Search by name, phone, email

### Merchant Management
- `status` - Filter by merchant status
- `search` - Search by name, email, phone

---

## Request Examples

### Create Merchant
```bash
POST /api/v1/merchants
Authorization: Bearer <token>

{
  "merchantName": "Yogesh Kitchen",
  "email": "yogesh@gmail.com",
  "username": "yogesh_kitchen",
  "password": "SecurePass123!",
  "phone": "8095242733",
  "address": "123 Main Street, Mumbai",
  "gstin": "29ABCDE1234F1Z5",
  "fssaiLicense": "12345678901234"
}
```

### Update Customer Profile
```bash
PUT /api/customers/456
Authorization: Bearer <token>

{
  "firstName": "John",
  "lastName": "Doe",
  "email": "john@example.com",
  "address": "123 Main Street",
  "city": "Mumbai",
  "state": "Maharashtra",
  "pincode": "400001",
  "dob": "1990-01-15"
}
```

### List Users with Filters
```bash
GET /api/v1/users?page=0&size=20&role=ROLE_MERCHANT&search=yogesh
Authorization: Bearer <token>
```

---

## Validation Rules

### Phone Numbers
- Pattern: `^[6-9]\d{9}$`
- Length: 10 digits
- Must start with 6-9

### Email
- RFC 5322 compliant
- Max 255 characters

### Password
- Min 8, max 128 characters
- Must contain:
  - Uppercase letter
  - Lowercase letter
  - Digit
  - Special character (@$!%*?&)

### GSTIN
- Pattern: `^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$`
- Length: 15 characters

### OTP
- Pattern: `^\d{4}$`
- Length: 4 digits
- Numeric only

---

## Security Features

### Token Validation
- SQL injection prevention
- XSS attack prevention
- Token format validation
- Expiry checking

### Rate Limiting
- OTP requests limited per phone number
- 429 status code returned on rate limit

### Access Control
- JWT Bearer authentication
- Role-based authorization (RBAC)
- Permission-based authorization
- Merchant-scoped data access

---

## Complete Endpoint Count

| Domain | Endpoints | Public | Authenticated |
|--------|-----------|--------|---------------|
| Authentication | 7 | 6 | 1 |
| User Management | 2 | 0 | 2 |
| Customer Management | 5 | 0 | 5 |
| Merchant Management | 5 | 0 | 5 |
| Health | 1 | 1 | 0 |
| **Total** | **20** | **7** | **13** |

---

## Next Steps

1. **View in Swagger UI**: Access `http://localhost:8080/swagger-ui.html`
2. **Test Endpoints**: Use the "Try it out" feature in Swagger UI
3. **Generate Client SDK**: Use OpenAPI Generator for your language
4. **Import to Postman**: Import the OpenAPI spec for collection

---

**Last Updated**: 2024-12-24
**API Version**: 1.0.0
**OpenAPI Version**: 3.0.3
