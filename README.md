# Cloud Kitchen RBAC Service

## Overview
Enterprise-grade Role-Based Access Control (RBAC) service for Cloud Kitchen applications with multi-tenant architecture, comprehensive security features

## Features
- ✅ Multi-tenant RBAC with merchant isolation
- ✅ JWT-based authentication with refresh tokens
- ✅ OTP-based login with rate limiting
- ✅ Comprehensive input validation and sanitization
- ✅ Audit logging and security monitoring
- ✅ Rate limiting and DDoS protection
- ✅ SQL injection prevention
- ✅ XSS protection
- ✅ CSRF protection
- ✅ Secure headers (HSTS, CSP, etc.)
- ✅ Password policy enforcement
- ✅ Account lockout mechanisms
- ✅ Token blacklisting
- ✅ Comprehensive error handling
- ✅ Performance optimization
- ✅ Caching with Redis
- ✅ Database connection pooling
- ✅ Swagger API documentation

## Architecture

### Complete Project Structure
```
cloud-kitchen-rbac-service/
├── src/
│   ├── main/
│   │   ├── java/com/cloudkitchen/rbac/
│   │   │   ├── config/
│   │   │   │   ├── DataInitializer.java          # Database initialization
│   │   │   │   ├── RateLimitConfig.java          # Rate limiting configuration
│   │   │   │   └── SwaggerConfig.java            # API documentation config
│   │   │   ├── controller/
│   │   │   │   ├── AuthController.java           # Authentication endpoints
│   │   │   │   ├── CustomerController.java       # Customer management
│   │   │   │   ├── HealthController.java         # Health check endpoint
│   │   │   │   ├── MerchantController.java       # Merchant management
│   │   │   │   └── UserController.java           # User management
│   │   │   ├── domain/entity/
│   │   │   │   ├── Merchant.java                 # Merchant entity
│   │   │   │   ├── OtpLog.java                   # OTP audit logging
│   │   │   │   ├── Permission.java               # Permission entity
│   │   │   │   ├── Role.java                     # Role entity
│   │   │   │   ├── RolePermission.java           # Role-Permission mapping
│   │   │   │   ├── User.java                     # User entity
│   │   │   │   ├── UserRole.java                 # User-Role mapping
│   │   │   │   └── UserSession.java              # User session tracking
│   │   │   ├── dto/
│   │   │   │   ├── auth/
│   │   │   │   │   ├── AuthRequest.java          # Login request DTO
│   │   │   │   │   ├── AuthResponse.java         # Authentication response
│   │   │   │   │   ├── OtpRequest.java           # OTP request DTO
│   │   │   │   │   ├── OtpVerifyRequest.java     # OTP verification DTO
│   │   │   │   │   ├── RefreshTokenRequest.java  # Token refresh DTO
│   │   │   │   │   └── RegisterRequest.java      # User registration DTO
│   │   │   │   ├── customer/
│   │   │   │   │   └── CustomerResponse.java     # Customer response DTO
│   │   │   │   └── merchant/
│   │   │   │       └── MerchantRequest.java      # Merchant request DTO
│   │   │   ├── exception/
│   │   │   │   ├── AuthExceptions.java           # Authentication exceptions
│   │   │   │   └── GlobalExceptionHandler.java  # Global error handling
│   │   │   ├── repository/
│   │   │   │   ├── MerchantRepository.java       # Merchant data access
│   │   │   │   ├── OtpLogRepository.java         # OTP log data access
│   │   │   │   ├── PermissionRepository.java     # Permission data access
│   │   │   │   ├── RolePermissionRepository.java # Role-Permission data access
│   │   │   │   ├── RoleRepository.java           # Role data access
│   │   │   │   ├── UserRepository.java           # User data access
│   │   │   │   ├── UserRoleRepository.java       # User-Role data access
│   │   │   │   └── UserSessionRepository.java    # Session data access
│   │   │   ├── security/
│   │   │   │   ├── JwtAuthenticationFilter.java  # JWT request filter
│   │   │   │   ├── JwtTokenProvider.java         # JWT token management
│   │   │   │   └── SecurityConfig.java           # Security configuration
│   │   │   ├── service/
│   │   │   │   ├── impl/
│   │   │   │   │   ├── AuthServiceImpl.java      # Authentication logic
│   │   │   │   │   ├── CustomerServiceImpl.java # Customer business logic
│   │   │   │   │   ├── MerchantServiceImpl.java  # Merchant business logic
│   │   │   │   │   ├── OtpServiceImpl.java       # OTP generation logic
│   │   │   │   │   └── UserServiceImpl.java      # User business logic
│   │   │   │   ├── AuthService.java              # Authentication interface
│   │   │   │   ├── CustomerService.java          # Customer service interface
│   │   │   │   ├── MerchantService.java          # Merchant service interface
│   │   │   │   ├── OtpAuditService.java          # OTP audit service
│   │   │   │   ├── OtpService.java               # OTP service interface
│   │   │   │   ├── SmsService.java               # SMS service interface
│   │   │   │   └── UserService.java              # User service interface
│   │   │   ├── util/
│   │   │   │   ├── ResponseBuilder.java          # API response builder
│   │   │   │   └── SecurityUtils.java            # Security utilities
│   │   │   ├── validation/
│   │   │   │   └── ValidationUtils.java          # Input validation utilities
│   │   │   └── RbacServiceApplication.java       # Main application class
│   │   └── resources/
│   │       ├── static/
│   │       │   └── index.html                    # Welcome page
│   │       ├── application.properties            # Main configuration
│   │       └── application-prod.properties       # Production configuration
│   └── test/
│       ├── java/com/cloudkitchen/rbac/
│       │   └── RbacServiceApplicationTests.java  # Application tests
│       └── resources/
│           └── application-test.properties       # Test configuration
├                                
├── .dockerignore                                 # Docker ignore file
├── .env.example                                  # Environment variables example
├── .gitignore                                    # Git ignore file
├── docker-compose.yml                           # Docker compose for development
├── docker-compose.prod.yml                      # Docker compose for production
├── Dockerfile                                    # Docker image definition
├── pom.xml                                       # Maven configuration
├── README.md                                     # This file
```

### File Descriptions & Usage

#### Configuration Files (`config/`)
- **DataInitializer.java**: Initializes default roles, permissions, and admin users on application startup
- **RateLimitConfig.java**: Configures API rate limiting to prevent abuse (60 req/min general, 10 req/min auth)
- **SwaggerConfig.java**: Sets up OpenAPI documentation accessible at `/swagger-ui.html`

#### Controllers (`controller/`)
- **AuthController.java**: Handles authentication endpoints (customer signup, login, OTP, logout)
- **UserController.java**: Manages user CRUD operations with role-based access control
- **MerchantController.java**: Handles merchant management operations
- **CustomerController.java**: Customer-specific operations
- **HealthController.java**: Application health monitoring endpoint

#### Entities (`domain/entity/`)
- **User.java**: Core user entity with multi-tenant support
- **Merchant.java**: Merchant/tenant entity for multi-tenancy
- **Role.java**: Role definition entity
- **Permission.java**: Permission definition entity
- **UserRole.java**: Many-to-many mapping between users and roles
- **RolePermission.java**: Many-to-many mapping between roles and permissions
- **UserSession.java**: Tracks active user sessions for security
- **OtpLog.java**: Audit trail for OTP operations

#### DTOs (`dto/`)
- **RegisterRequest.java**: User registration with comprehensive validation
- **AuthRequest.java**: Login request with merchant context
- **AuthResponse.java**: Authentication response with tokens and user info
- **OtpRequest.java**: OTP generation request
- **OtpVerifyRequest.java**: OTP verification request
- **RefreshTokenRequest.java**: Token refresh request

#### Security (`security/`)
- **SecurityConfig.java**: Spring Security configuration with CORS, headers, and authentication
- **JwtTokenProvider.java**: JWT token creation, validation, and blacklisting
- **JwtAuthenticationFilter.java**: Request filter for JWT token validation

#### Services (`service/` & `service/impl/`)
- **AuthService.java/AuthServiceImpl.java**: Authentication business logic
- **UserService.java/UserServiceImpl.java**: User management business logic
- **MerchantService.java/MerchantServiceImpl.java**: Merchant management logic
- **OtpService.java/OtpServiceImpl.java**: OTP generation and validation
- **OtpAuditService.java**: OTP audit logging service

#### Repositories (`repository/`)
- **UserRepository.java**: User data access with custom queries
- **MerchantRepository.java**: Merchant data access
- **RoleRepository.java**: Role data access
- **PermissionRepository.java**: Permission data access
- **UserRoleRepository.java**: User-role relationship queries
- **UserSessionRepository.java**: Session management queries
- **OtpLogRepository.java**: OTP audit queries

#### Utilities (`util/` & `validation/`)
- **ResponseBuilder.java**: Standardized API response formatting
- **SecurityUtils.java**: Input sanitization and security utilities
- **ValidationUtils.java**: Custom validation methods

#### Exception Handling (`exception/`)
- **GlobalExceptionHandler.java**: Centralized exception handling with sanitized responses
- **AuthExceptions.java**: Custom authentication-related exceptions

### Security Features

#### Cryptographic Algorithms
- **JWT Signing**: HMAC-SHA256 (HS256) algorithm for token signing
- **Password Hashing**: BCrypt with 12 rounds for secure password storage
- **Token Security**: UUID-based JTI (JWT ID) for token uniqueness
- **Key Derivation**: HMAC-SHA key generation from configured secret

#### Authentication & Authorization
- JWT tokens with HMAC-SHA256 (HS256) signing
- Refresh token rotation with blacklisting
- Token blacklisting for logout security
- Role-based permissions with multi-tenant isolation
- Secure token validation with clock skew tolerance

#### Input Security
- Comprehensive input validation
- SQL injection prevention
- XSS protection
- Input sanitization
- Phone number masking in logs

#### Rate Limiting
- Per-IP rate limiting
- Authentication endpoint protection
- Configurable limits
- Automatic cleanup

#### Audit & Monitoring
- Comprehensive audit logging
- Security event tracking
- Performance monitoring
- Error tracking with trace IDs

## API Endpoints

### Authentication Endpoints
- `POST /api/auth/signup` - **Customer Registration** (Creates new customer account)
- `POST /api/auth/login` - **Merchant/Admin Login** (Password-based login for merchants/admins)
- `POST /api/auth/customer/login` - **Customer Login** (Password-based login for customers)
- `POST /api/auth/otp/request` - **Request OTP** (Send OTP to phone number)
- `POST /api/auth/otp/verify` - **Verify OTP** (Verify OTP and get tokens)
- `POST /api/auth/refresh` - **Refresh Tokens** (Get new access token using refresh token)
- `POST /api/auth/logout` - **Logout** (Invalidate user session)

#### Customer Signup Example
```json
POST /api/auth/signup
{
  "merchantId": 1,
  "userType": "customer",
  "phone": "9876543210",
  "password": "SecurePass123!",
  "firstName": "John",
  "lastName": "Doe",
  "email": "john.doe@example.com",
  "address": "123 Main Street, City"
}
```

#### Customer Login Example
```json
POST /api/auth/customer/login
{
  "phone": "9876543210",
  "password": "SecurePass123!",
  "merchantId": 1
}
```

### User Management
- `GET /api/users` - List users (paginated)
- `GET /api/users/{id}` - Get user details
- `POST /api/users` - Create user
- `PUT /api/users/{id}` - Update user
- `DELETE /api/users/{id}` - Delete user

### Merchant Management
- `GET /api/merchants` - List merchants
- `GET /api/merchants/{id}` - Get merchant details
- `POST /api/merchants` - Create merchant
- `PUT /api/merchants/{id}` - Update merchant
- `DELETE /api/merchants/{id}` - Delete merchant

## Configuration

### Environment Variables
```bash
# Database
DATABASE_URL=jdbc:postgresql://localhost:5432/cloud_kitchen_rbac
DB_USERNAME=postgres
DB_PASSWORD=your_password

# JWT
JWT_SECRET=your_jwt_secret_key_here
JWT_ACCESS_EXPIRY=3600
JWT_REFRESH_EXPIRY=604800

# Redis (Optional)
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

# CORS
CORS_ORIGINS=http://localhost:3000,http://localhost:8080

# Swagger
SWAGGER_ENABLED=true
SWAGGER_UI_ENABLED=true
```

## Security Considerations

### Cryptographic Implementation
- **JWT Algorithm**: HMAC-SHA256 (HS256) for symmetric key signing
- **Password Hashing**: BCrypt with 12 rounds (2^12 = 4,096 iterations)
- **Secret Requirements**: Minimum 32 characters for JWT signing key
- **Token Security**: UUID-based JTI prevents token replay attacks
- **Key Management**: Proper HMAC key derivation from configured secret

### Production Deployment
1. Use strong JWT secrets (256-bit minimum, 32+ characters)
2. Enable HTTPS only for all communications
3. Configure proper CORS origins (no wildcards in production)
4. Set up Redis for distributed token blacklisting
5. Enable comprehensive audit logging
6. Configure rate limiting (10 req/min auth, 60 req/min general)
7. Set up monitoring and alerting for security events
8. Regular security updates and dependency scanning

### Database Security
1. Use connection pooling
2. Parameterized queries only
3. Database user with minimal privileges
4. Regular backups
5. Encryption at rest

## Performance Optimization

### Caching Strategy
- Redis for session management
- JPA second-level cache
- Query result caching
- Token blacklist caching

### Database Optimization
- Connection pooling (HikariCP)
- Batch operations
- Proper indexing
- Query optimization

## Monitoring & Observability

### Metrics
- Micrometer with Prometheus
- JVM metrics
- Database metrics
- Custom business metrics

### Logging
- Structured logging
- Audit trails
- Security events
- Performance logs

## Testing

### Test Coverage
- Unit tests for services
- Integration tests for controllers
- Security tests
- Performance tests

### Test Profiles
- `test` - In-memory H2 database
- `integration` - Test containers
- `performance` - Load testing

## Deployment

### Docker
```bash
docker build -t cloud-kitchen-rbac .
docker run -p 8081:8081 cloud-kitchen-rbac
```

### Docker Compose
```bash
docker-compose up -d
```

## Quick Start

### Prerequisites
- Java 17+
- Maven 3.8+
- PostgreSQL 13+

### Setup
1. Configure database in `application.properties`
2. Run: `mvn spring-boot:run`
3. Access: `http://localhost:8081/swagger-ui.html`

### Testing
Run tests: `mvn test`

### Security
- JWT authentication with refresh tokens
- Role-based access control
- Input validation and sanitization
- Rate limiting and audit logging

## Documentation
- **Swagger UI**: `http://localhost:8081/swagger-ui.html`
- **Health Check**: `http://localhost:8081/actuator/health`

## License
MIT License
