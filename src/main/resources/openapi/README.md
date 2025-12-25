# OpenAPI Documentation

This directory contains the OpenAPI 3.0 specification for the Cloud Kitchen RBAC Service API.

## Directory Structure

```
openapi/
├── openapi.yaml           # Main OpenAPI specification (entry point)
├── auth.yaml             # Authentication endpoints
├── users.yaml            # User management endpoints
├── customers.yaml        # Customer management endpoints
├── merchants.yaml        # Merchant management endpoints
├── health.yaml           # Health check endpoints
├── common/
│   ├── schemas.yaml      # Common/shared schemas
│   └── errors.yaml       # Error response schemas
└── README.md             # This file
```

## Organization by Controller

The API documentation is split by controller/domain:

- **auth.yaml**: All authentication-related endpoints
  - Registration, login, logout
  - OTP request and verification
  - Token refresh

- **users.yaml**: User management operations (Super Admin only)
  - List all users with pagination/filtering
  - Get user by ID

- **customers.yaml**: Customer management operations
  - Update customer profile
  - Get customer details
  - List customers with pagination
  - Get customer profile
  - Get customers by merchant

- **merchants.yaml**: Merchant CRUD operations
  - Create merchant
  - Update merchant
  - Get merchant details
  - List all merchants
  - Delete merchant

- **health.yaml**: Service health and monitoring
  - Get user count

## How to Use

### 1. Swagger UI (Recommended)

The easiest way to view and interact with the API documentation is through Swagger UI.

#### Spring Boot Configuration

Add the following dependencies to your `pom.xml`:

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.3.0</version>
</dependency>
```

Add configuration to `application.properties` or `application.yml`:

```yaml
springdoc:
  api-docs:
    enabled: true
    path: /v3/api-docs
  swagger-ui:
    enabled: true
    path: /swagger-ui.html
    urls:
      - name: Cloud Kitchen RBAC API
        url: /openapi/openapi.yaml
```

Access Swagger UI at: `http://localhost:8080/swagger-ui.html`

### 2. Standalone Tools

#### Swagger Editor
1. Go to [editor.swagger.io](https://editor.swagger.io)
2. File → Import File → Select `openapi.yaml`
3. The editor will render the complete API documentation

#### Redoc
For a cleaner, read-only documentation view:
```bash
npm install -g redoc-cli
redoc-cli serve openapi.yaml
```

#### Postman
1. Open Postman
2. Import → Link → Paste your OpenAPI URL or upload `openapi.yaml`
3. Postman will generate a complete collection with all endpoints

### 3. Generate Client SDKs

Use OpenAPI Generator to create client libraries:

```bash
# JavaScript/TypeScript
openapi-generator-cli generate -i openapi.yaml -g typescript-axios -o ./client/typescript

# Java
openapi-generator-cli generate -i openapi.yaml -g java -o ./client/java

# Python
openapi-generator-cli generate -i openapi.yaml -g python -o ./client/python
```

## Key Features

### 1. Complete RBAC Documentation
Every endpoint includes:
- Required roles/permissions
- HTTP status codes (401, 403, etc.)
- Access control rules
- Authentication requirements

### 2. Accurate Request/Response Schemas
All schemas match the actual DTOs in the codebase:
- Field types and validations
- Required vs. optional fields
- Pattern constraints
- Size limits

### 3. Production-Ready Contract
The OpenAPI spec can be used as:
- API contract for frontend teams
- Automated testing specification
- Client SDK generation
- API gateway configuration

### 4. HTTP Status Code Documentation

#### Authentication & Authorization
- **401 Unauthorized**: Missing or invalid authentication token
- **403 Forbidden**: Valid token but insufficient permissions

#### Validation
- **400 Bad Request**: Invalid input data or validation failed

#### Resource Status
- **404 Not Found**: Resource does not exist
- **409 Conflict**: Resource already exists or conflict

#### Rate Limiting
- **429 Too Many Requests**: Rate limit exceeded

#### Server Errors
- **500 Internal Server Error**: Unexpected server error
- **503 Service Unavailable**: Service temporarily unavailable

## Authentication

All protected endpoints require JWT Bearer token:

```http
Authorization: Bearer <your_jwt_token>
```

### How to Obtain a Token

1. **Merchant/Admin Login**:
   ```bash
   POST /api/v1/auth/login
   {
     "merchantId": 0,
     "username": "yogesh_kitchen",
     "password": "SecurePass123!"
   }
   ```

2. **Customer Login**:
   ```bash
   POST /api/v1/auth/customer/login
   {
     "merchantId": 1,
     "username": "9876543210",
     "password": "SecurePass123!"
   }
   ```

3. **OTP-Based Login**:
   ```bash
   # Request OTP
   POST /api/v1/auth/otp/request
   {
     "merchantId": 0,
     "phone": "9876543210",
     "otpType": "login"
   }

   # Verify OTP
   POST /api/v1/auth/otp/verify
   {
     "merchantId": 0,
     "phone": "9876543210",
     "otp": "1234",
     "otpType": "login"
   }
   ```

## MerchantId Convention

The `merchantId` field has special meaning:

- **null**: Super admin operations
- **0**: Merchant/admin operations (merchant login, general OTP)
- **> 0**: Customer operations for specific merchant

## Validation & Security

All endpoints include:
- Input validation rules
- Pattern constraints (phone numbers, emails, etc.)
- SQL injection prevention
- XSS attack prevention
- Token format validation

## Testing the API

### Using cURL

```bash
# Login
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "merchantId": 0,
    "username": "yogesh_kitchen",
    "password": "SecurePass123!"
  }'

# Use the returned token
TOKEN="your_jwt_token_here"

# Get all users (requires ROLE_SUPER_ADMIN)
curl -X GET http://localhost:8080/api/v1/users \
  -H "Authorization: Bearer $TOKEN"
```

### Using Swagger UI

1. Navigate to the Swagger UI
2. Click "Authorize" button
3. Enter: `Bearer <your_jwt_token>`
4. Try out any endpoint

## Updates and Maintenance

When updating the API:

1. **Add new endpoints**: Create or update the appropriate domain YAML file
2. **Add new schemas**: Add to the domain YAML or `common/schemas.yaml`
3. **Add new errors**: Add to `common/errors.yaml`
4. **Update main spec**: Reference new paths in `openapi.yaml`

## Validation

Validate the OpenAPI specification:

```bash
# Using swagger-cli
npm install -g @apidevtools/swagger-cli
swagger-cli validate openapi.yaml

# Using openapi-generator
openapi-generator-cli validate -i openapi.yaml
```

## Best Practices

1. **Do NOT add Swagger annotations to controllers**
   - Keep all documentation in YAML files
   - Controllers remain clean and focused

2. **Keep schemas accurate**
   - Match actual DTO classes
   - Include all validation constraints

3. **Document RBAC rules**
   - Specify required roles/permissions
   - Document 401 vs 403 scenarios

4. **Use correct HTTP status codes**
   - 401 for unauthenticated
   - 403 for unauthorized
   - Follow REST conventions

5. **Provide examples**
   - Include realistic example values
   - Show common use cases

## Support

For questions or issues with the API documentation, please contact the development team.
