# Cloud Kitchen RBAC Service

Enterprise-grade Role-Based Access Control (RBAC) service for Cloud Kitchen application with JWT authentication, OTP verification, and multi-tenant support.

## 📋 Table of Contents

- [Features](#features)
- [Architecture](#architecture)
- [Project Structure](#project-structure)
- [Technology Stack](#technology-stack)
- [Getting Started](#getting-started)
- [Configuration](#configuration)
- [API Documentation](#api-documentation)
- [Database Schema](#database-schema)
- [Security](#security)
- [Deployment](#deployment)

---

## ✨ Features

### Authentication & Authorization
- **JWT-based Authentication** - Secure token-based authentication with access and refresh tokens
- **Role-Based Access Control** - Fine-grained permission management (Super Admin, Merchant, Customer)
- **Multi-tenant Support** - Merchant-specific user isolation and data segregation
- **OTP Verification** - SMS-based OTP for login and password reset
- **Password Management** - Secure password hashing with BCrypt and reset functionality

### Security Features
- **Rate Limiting** - API rate limiting to prevent abuse
- **Token Blacklisting** - Logout with token invalidation
- **Input Validation** - Comprehensive request validation
- **CORS Configuration** - Configurable cross-origin resource sharing
- **Security Logging** - Audit trail for security events

### Additional Features
- **File Upload** - AWS S3 integration for document management
- **Health Monitoring** - Actuator endpoints for application health
- **API Documentation** - OpenAPI/Swagger UI integration
- **Metrics & Monitoring** - Prometheus metrics support
- **Async Processing** - Asynchronous task execution

---

## 🏗️ Architecture

### System Design

```
┌─────────────────────────────────────────────────────────────────┐
│                         Client Layer                             │
│  (Web App / Mobile App / Third-party Services)                  │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│                      API Gateway / NGINX                         │
│                    (Load Balancer / Proxy)                       │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Spring Boot Application                       │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │              Controller Layer                             │  │
│  │  - AuthController                                         │  │
│  │  - UserController                                         │  │
│  │  - MerchantController                                     │  │
│  │  - CustomerController                                     │  │
│  └────────────────────┬─────────────────────────────────────┘  │
│                       │                                          │
│  ┌────────────────────▼─────────────────────────────────────┐  │
│  │              Security Layer                               │  │
│  │  - JwtAuthenticationFilter                                │  │
│  │  - JwtTokenProvider                                       │  │
│  │  - SecurityConfig                                         │  │
│  └────────────────────┬─────────────────────────────────────┘  │
│                       │                                          │
│  ┌────────────────────▼─────────────────────────────────────┐  │
│  │              Service Layer                                │  │
│  │  - AuthService                                            │  │
│  │  - UserService                                            │  │
│  │  - OtpService                                             │  │
│  │  - ValidationService                                      │  │
│  │  - FileUploadService                                      │  │
│  └────────────────────┬─────────────────────────────────────┘  │
│                       │                                          │
│  ┌────────────────────▼─────────────────────────────────────┐  │
│  │              Repository Layer                             │  │
│  │  - UserRepository                                         │  │
│  │  - RoleRepository                                         │  │
│  │  - MerchantRepository                                     │  │
│  │  - OtpLogRepository                                       │  │
│  └────────────────────┬─────────────────────────────────────┘  │
└────────────────────────┼────────────────────────────────────────┘
                         │
         ┌───────────────┴───────────────┐
         ▼                               ▼
┌──────────────────┐           ┌──────────────────┐
│   PostgreSQL     │           │     AWS S3       │
│    Database      │           │  File Storage    │
└──────────────────┘           └──────────────────┘
```

### Authentication Flow

```
┌────────┐                ┌────────────┐              ┌──────────┐
│ Client │                │   Server   │              │ Database │
└───┬────┘                └─────┬──────┘              └────┬─────┘
    │                           │                          │
    │  1. Login Request         │                          │
    ├──────────────────────────>│                          │
    │  (username, password)     │                          │
    │                           │  2. Validate User        │
    │                           ├─────────────────────────>│
    │                           │                          │
    │                           │  3. User Data            │
    │                           │<─────────────────────────┤
    │                           │                          │
    │                           │  4. Verify Password      │
    │                           │     (BCrypt)             │
    │                           │                          │
    │                           │  5. Fetch Roles &        │
    │                           │     Permissions          │
    │                           ├─────────────────────────>│
    │                           │<─────────────────────────┤
    │                           │                          │
    │                           │  6. Generate JWT Tokens  │
    │                           │     (Access + Refresh)   │
    │                           │                          │
    │  7. Auth Response         │                          │
    │<──────────────────────────┤                          │
    │  (tokens, user info)      │                          │
    │                           │                          │
    │  8. API Request           │                          │
    │  (Authorization: Bearer)  │                          │
    ├──────────────────────────>│                          │
    │                           │  9. Validate JWT         │
    │                           │                          │
    │                           │ 10. Check Permissions    │
    │                           │                          │
    │ 11. API Response          │                          │
    │<──────────────────────────┤                          │
```


## 📁 Project Structure

```
cloud-kitchen-rbac-service/
│
├── src/
│   ├── main/
│   │   ├── java/com/cloudkitchen/rbac/
│   │   │   │
│   │   │   ├── config/                      # Configuration classes
│   │   │   │   ├── AppConstants.java        # Application-wide constants
│   │   │   │   ├── SecurityConfig.java      # Spring Security configuration
│   │   │   │   ├── OpenApiConfig.java       # Swagger/OpenAPI setup
│   │   │   │   ├── AwsS3Config.java         # AWS S3 configuration
│   │   │   │   ├── RedisCacheConfig.java    # Redis cache configuration
│   │   │   │   ├── ApiRateLimitConfig.java  # Rate limiting setup
│   │   │   │   ├── AsyncExecutorConfig.java # Async task configuration
│   │   │   │   ├── DataInitializer.java     # Database initialization
│   │   │   │   └── JacksonDateTimeConfig.java # JSON date/time handling
│   │   │   │
│   │   │   ├── controller/                  # REST API Controllers
│   │   │   │   ├── AuthController.java      # Authentication endpoints
│   │   │   │   ├── UserController.java      # User management
│   │   │   │   ├── MerchantController.java  # Merchant operations
│   │   │   │   ├── CustomerController.java  # Customer operations
│   │   │   │   ├── FileUploadController.java # File upload handling
│   │   │   │   ├── FolderController.java    # Folder management
│   │   │   │   └── HealthController.java    # Health check endpoints
│   │   │   │
│   │   │   ├── domain/entity/               # JPA Entity classes
│   │   │   │   ├── User.java                # User entity
│   │   │   │   ├── Role.java                # Role entity
│   │   │   │   ├── Permission.java          # Permission entity
│   │   │   │   ├── UserRole.java            # User-Role mapping
│   │   │   │   ├── RolePermission.java      # Role-Permission mapping
│   │   │   │   ├── Merchant.java            # Merchant entity
│   │   │   │   ├── OtpLog.java              # OTP audit log
│   │   │   │   └── FileDocument.java        # File metadata
│   │   │   │
│   │   │   ├── dto/                         # Data Transfer Objects
│   │   │   │   ├── auth/                    # Authentication DTOs
│   │   │   │   │   ├── AuthRequest.java     # Login request
│   │   │   │   │   ├── AuthResponse.java    # Login response
│   │   │   │   │   ├── RegisterRequest.java # Registration request
│   │   │   │   │   ├── OtpRequest.java      # OTP request
│   │   │   │   │   ├── OtpVerifyRequest.java # OTP verification
│   │   │   │   │   └── RefreshTokenRequest.java # Token refresh
│   │   │   │   ├── user/                    # User DTOs
│   │   │   │   ├── merchant/                # Merchant DTOs
│   │   │   │   └── customer/                # Customer DTOs
│   │   │   │
│   │   │   ├── repository/                  # JPA Repositories
│   │   │   │   ├── UserRepository.java      # User data access
│   │   │   │   ├── RoleRepository.java      # Role data access
│   │   │   │   ├── PermissionRepository.java # Permission data access
│   │   │   │   ├── UserRoleRepository.java  # User-Role data access
│   │   │   │   ├── RolePermissionRepository.java # Role-Permission access
│   │   │   │   ├── MerchantRepository.java  # Merchant data access
│   │   │   │   ├── OtpLogRepository.java    # OTP log data access
│   │   │   │   └── FileDocumentRepository.java # File metadata access
│   │   │   │
│   │   │   ├── service/                     # Business Logic Layer
│   │   │   │   ├── impl/                    # Service implementations
│   │   │   │   │   ├── AuthServiceImpl.java # Authentication logic
│   │   │   │   │   ├── UserServiceImpl.java # User management logic
│   │   │   │   │   ├── OtpServiceImpl.java  # OTP generation/validation
│   │   │   │   │   └── ...
│   │   │   │   ├── AuthService.java         # Auth service interface
│   │   │   │   ├── UserService.java         # User service interface
│   │   │   │   ├── OtpService.java          # OTP service interface
│   │   │   │   ├── ValidationService.java   # Input validation
│   │   │   │   ├── SmsService.java          # SMS integration
│   │   │   │   ├── FileUploadService.java   # File upload logic
│   │   │   │   └── CloudStorageService.java # Cloud storage integration
│   │   │   │
│   │   │   ├── security/                    # Security Components
│   │   │   │   ├── JwtTokenProvider.java    # JWT token generation/validation
│   │   │   │   ├── JwtAuthenticationFilter.java # JWT filter
│   │   │   │   └── SecurityConfig.java      # Security configuration
│   │   │   │
│   │   │   ├── exception/                   # Exception Handling
│   │   │   │   ├── BusinessExceptions.java  # Custom business exceptions
│   │   │   │   └── GlobalExceptionHandler.java # Global exception handler
│   │   │   │
│   │   │   ├── util/                        # Utility Classes
│   │   │   │   ├── ResponseBuilder.java     # API response builder
│   │   │   │   ├── ValidationUtils.java     # Validation utilities
│   │   │   │   └── HttpResponseUtil.java    # HTTP response utilities
│   │   │   │
│   │   │   ├── enums/                       # Enumerations
│   │   │   │   └── UserType.java            # User type enum
│   │   │   │
│   │   │   └── RbacServiceApplication.java  # Main application class
│   │   │
│   │   └── resources/
│   │       ├── application.properties       # Application configuration
│   │       ├── logback-spring.xml          # Logging configuration
│   │       └── META-INF/
│   │           └── spring-configuration-metadata.json
│   │
│   └── test/
│       └── java/                            # Test classes
│
├──                         # Security audit logs
│
├── target/                                  # Maven build output
│
├── .env                                     # Environment variables (not in git)
├── .env.example                             # Environment template
├── .gitignore                               # Git ignore rules
├── docker-compose.yml                       # Docker compose for development
├── docker-compose.prod.yml                  # Docker compose for production
├── Dockerfile                               # Docker image definition
├── nginx.conf                               # NGINX configuration
├── pom.xml                                  # Maven dependencies
└── README.md                                # This file
```

### Key Components Explained

#### **Config Package**
- **AppConstants**: Centralized constants (OTP expiry, rate limits, etc.)
- **SecurityConfig**: Spring Security setup, CORS, authentication
- **DataInitializer**: Seeds initial roles, permissions, and admin user

#### **Controller Package**
- REST API endpoints
- Request validation
- Response formatting
- Exception handling

#### **Service Package**
- Business logic implementation
- Transaction management
- Data validation
- External service integration

#### **Repository Package**
- Database operations
- Custom queries
- JPA specifications

#### **Security Package**
- JWT token management
- Authentication filters
- Authorization logic

---

## 🛠️ Technology Stack

### Backend Framework
- **Spring Boot 3.3.13** - Application framework
- **Spring Security** - Authentication & authorization
- **Spring Data JPA** - Database ORM
- **Spring Actuator** - Health monitoring

### Database
- **PostgreSQL** - Primary database
- **H2** - In-memory database for testing

### Security & Authentication
- **JWT (JJWT 0.12.6)** - Token-based authentication
- **BCrypt** - Password hashing

### API Documentation
- **SpringDoc OpenAPI 2.6.0** - API documentation (Swagger UI)

### Cloud Services
- **AWS S3** - File storage

### Build & Deployment
- **Maven** - Dependency management
- **Docker** - Containerization
- **NGINX** - Reverse proxy

### Code Quality
- **SonarQube** - Code quality analysis
- **JaCoCo** - Code coverage
- **OWASP Dependency Check** - Security vulnerability scanning

### Development Tools
- **Java 21** - Programming language
- **Lombok** - Boilerplate code reduction (optional)

---

## 🚀 Getting Started

### Prerequisites

- **Java 21** or higher
- **Maven 3.8+**
- **PostgreSQL 14+**
- **Docker** (optional)

### Installation

1. **Clone the repository**
```bash
git clone <repository-url>
cd cloud-kitchen-rbac-service
```

2. **Configure environment variables**
```bash
cp .env.example .env
# Edit .env with your configuration
```

3. **Set up PostgreSQL database**
```sql
CREATE DATABASE cloud_kitchen_rbac;
```

4. **Run the application**
```bash
# Using Maven
mvn clean install
mvn spring-boot:run

# Or using the batch file (Windows)
run-maven.bat
```

5. **Access the application**
- API Base URL: `http://localhost:8081`
- Swagger UI: `http://localhost:8081/swagger-ui.html`
- Health Check: `http://localhost:8081/actuator/health`

### Docker Deployment

```bash
# Development
docker-compose up -d

# Production
docker-compose -f docker-compose.prod.yml up -d
```

---

## ⚙️ Configuration

### Environment Variables

Create a `.env` file in the root directory:

```properties
# Database Configuration
DATABASE_URL=jdbc:postgresql://localhost:5432/cloud_kitchen_rbac
DB_USERNAME=postgres
DB_PASSWORD=your_password

# JWT Configuration (minimum 32 characters)
JWT_SECRET=your-secure-jwt-secret-key-minimum-32-characters
JWT_ACCESS_EXPIRY=86400      # 24 hours in seconds
JWT_REFRESH_EXPIRY=604800    # 7 days in seconds

# CORS Configuration
CORS_ORIGINS=http://localhost:3000,https://yourdomain.com

# Application Configuration
PORT=8081
SPRING_PROFILES_ACTIVE=dev

# AWS S3 Configuration (optional)
AWS_ACCESS_KEY_ID=your_access_key
AWS_SECRET_ACCESS_KEY=your_secret_key
AWS_S3_BUCKET_NAME=your_bucket_name
AWS_REGION=us-east-1

# SMS Service Configuration (optional)
SMS_API_KEY=your_sms_api_key
SMS_API_URL=https://sms-provider.com/api
```

### Application Properties

Key configurations in `application.properties`:

```properties
# Server
server.port=${PORT:8081}

# Database
spring.datasource.url=${DATABASE_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}

# JPA
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false

# JWT
jwt.secret=${JWT_SECRET}
jwt.access-expiry=${JWT_ACCESS_EXPIRY:86400}
jwt.refresh-expiry=${JWT_REFRESH_EXPIRY:604800}

# Logging
logging.file.name=logs/security.log
logging.level.com.cloudkitchen.rbac=INFO
```

---

## 📚 API Documentation

### Base URL
```
http://localhost:8081/api
```

### Authentication Endpoints

#### 1. Customer Registration
```http
POST /api/auth/signup
Content-Type: application/json

{
  "phone": "1234567890",
  "password": "SecurePass123!",
  "firstName": "John",
  "lastName": "Doe",
  "email": "john@example.com",
  "merchantId": 1,
  "address": "123 Main St"
}

Response: 200 OK
{
  "accessToken": "eyJhbGc...",
  "refreshToken": "eyJhbGc...",
  "expiresIn": 86400,
  "userId": 1,
  "merchantId": 1,
  "phone": "1234567890",
  "roles": ["customer"]
}
```

#### 2. Merchant/Admin Login
```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "admin123",
  "merchantId": 0
}

Response: 200 OK
{
  "accessToken": "eyJhbGc...",
  "refreshToken": "eyJhbGc...",
  "expiresIn": 86400,
  "userId": 1,
  "merchantId": 0,
  "phone": "1234567890",
  "roles": ["super_admin"]
}
```

#### 3. Customer Login
```http
POST /api/auth/customer/login
Content-Type: application/json

{
  "username": "1234567890",
  "password": "SecurePass123!",
  "merchantId": 1
}
```

#### 4. Request OTP
```http
POST /api/auth/otp/request
Content-Type: application/json

{
  "phone": "1234567890",
  "merchantId": 1,
  "otpType": "login"
}

Response: 200 OK
{
  "message": "OTP sent successfully"
}
```

#### 5. Verify OTP
```http
POST /api/auth/otp/verify
Content-Type: application/json

{
  "phone": "1234567890",
  "otp": "123456",
  "merchantId": 1,
  "otpType": "login"
}

Response: 200 OK
{
  "accessToken": "eyJhbGc...",
  "refreshToken": "eyJhbGc...",
  "expiresIn": 86400,
  "userId": 1,
  "merchantId": 1,
  "phone": "1234567890",
  "roles": ["customer"]
}
```

#### 6. Refresh Token
```http
POST /api/auth/refresh
Content-Type: application/json

{
  "refreshToken": "eyJhbGc..."
}

Response: 200 OK
{
  "accessToken": "eyJhbGc...",
  "refreshToken": "eyJhbGc...",
  "expiresIn": 86400
}
```

#### 7. Logout
```http
POST /api/auth/logout
Authorization: Bearer {accessToken}

Response: 200 OK
{
  "message": "Logged out successfully"
}
```

### User Management Endpoints

#### Get All Users (Admin only)
```http
GET /api/users
Authorization: Bearer {accessToken}

Response: 200 OK
[
  {
    "userId": 1,
    "username": "admin",
    "phone": "1234567890",
    "firstName": "Admin",
    "lastName": "User",
    "userType": "super_admin",
    "active": true
  }
]
```

### Swagger UI

Access interactive API documentation at:
```
http://localhost:8081/swagger-ui.html
```

---

## 🗄️ Database Schema

### Core Tables

#### users
```sql
CREATE TABLE users (
    user_id SERIAL PRIMARY KEY,
    merchant_id INTEGER REFERENCES merchants(merchant_id),
    username VARCHAR(50) UNIQUE NOT NULL,
    phone VARCHAR(15) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    email VARCHAR(100),
    user_type VARCHAR(20) NOT NULL,
    address TEXT,
    active BOOLEAN DEFAULT TRUE,
    otp_code VARCHAR(10),
    otp_expires_at TIMESTAMP,
    otp_attempts INTEGER DEFAULT 0,
    otp_used BOOLEAN DEFAULT FALSE,
    otp_blocked_until TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### roles
```sql
CREATE TABLE roles (
    role_id SERIAL PRIMARY KEY,
    role_name VARCHAR(50) UNIQUE NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### permissions
```sql
CREATE TABLE permissions (
    permission_id SERIAL PRIMARY KEY,
    permission_name VARCHAR(100) UNIQUE NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### user_roles
```sql
CREATE TABLE user_roles (
    user_role_id SERIAL PRIMARY KEY,
    user_id INTEGER REFERENCES users(user_id),
    role_id INTEGER REFERENCES roles(role_id),
    merchant_id INTEGER REFERENCES merchants(merchant_id),
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### merchants
```sql
CREATE TABLE merchants (
    merchant_id SERIAL PRIMARY KEY,
    merchant_name VARCHAR(100) NOT NULL,
    contact_email VARCHAR(100),
    contact_phone VARCHAR(15),
    address TEXT,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### otp_logs
```sql
CREATE TABLE otp_logs (
    otp_log_id SERIAL PRIMARY KEY,
    merchant_id INTEGER REFERENCES merchants(merchant_id),
    phone VARCHAR(15) NOT NULL,
    otp_code VARCHAR(10) NOT NULL,
    otp_type VARCHAR(50),
    status VARCHAR(20),
    expires_at TIMESTAMP,
    created_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

---

## 🔒 Security

### Authentication Flow
1. User submits credentials
2. Server validates credentials
3. Server generates JWT access token (24h) and refresh token (7d)
4. Client stores tokens securely
5. Client includes access token in Authorization header for API requests
6. Server validates token and checks permissions

### Password Security
- Passwords hashed using BCrypt (cost factor: 10)
- Minimum password requirements enforced
- Password reset via OTP verification

### OTP Security
- 6-digit numeric OTP
- 10-minute expiry
- Maximum 3 attempts
- Rate limiting: 5 requests per 15 minutes
- Phone blocking after excessive failed attempts

### API Security
- CORS configuration
- Rate limiting per IP
- Input validation and sanitization
- SQL injection prevention (JPA)
- XSS protection

### Token Management
- Access tokens expire in 24 hours
- Refresh tokens expire in 7 days
- Token blacklisting on logout
- Secure token storage recommendations

---

## 🚢 Deployment

### Production Checklist

- [ ] Set strong JWT_SECRET (minimum 32 characters)
- [ ] Configure production database credentials
- [ ] Set CORS_ORIGINS to production domains only
- [ ] Enable HTTPS/SSL
- [ ] Configure firewall rules
- [ ] Set up database backups
- [ ] Configure log rotation
- [ ] Set up monitoring and alerts
- [ ] Review and update rate limits
- [ ] Configure SMS service for OTP
- [ ] Set up AWS S3 for file uploads

### Docker Production Deployment

```bash
# Build image
docker build -t cloud-kitchen-rbac:latest .

# Run with docker-compose
docker-compose -f docker-compose.prod.yml up -d

# View logs
docker-compose logs -f rbac-service
```

### Environment-specific Profiles

```bash
# Development
SPRING_PROFILES_ACTIVE=dev

# Production
SPRING_PROFILES_ACTIVE=prod
```

---

## 📊 Monitoring & Logging

### Health Check
```bash
curl http://localhost:8081/actuator/health
```

### Application Metrics
```bash
curl http://localhost:8081/actuator/metrics
```

### Security Logs
Located at: `logs/security.log`

Log format:
```
2025-01-15 10:30:45 [INFO] Login attempt for merchantId: 1
2025-01-15 10:30:46 [INFO] Login successful for user: 5 (type: customer)
```

---

## 🧪 Testing

### Run Tests
```bash
mvn test
```

### Code Coverage
```bash
mvn clean test jacoco:report
# Report: target/site/jacoco/index.html
```

### SonarQube Analysis
```bash
mvn clean verify sonar:sonar
```

---

## 📝 License

This project is proprietary software. All rights reserved.

---

## 👥 Support

For support and questions:
- Email: support@cloudkitchen.com
- Documentation: [Wiki](link-to-wiki)
- Issue Tracker: [GitHub Issues](link-to-issues)

---

## 🔄 Version History

### v1.0.0 (Current)
- Initial release
- JWT authentication
- Role-based access control
- OTP verification
- Multi-tenant support
- File upload functionality
- API documentation

---

**Built with ❤️ by Cloud Kitchen Team**
