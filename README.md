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

