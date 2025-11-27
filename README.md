# Cloud Kitchen RBAC Service

Enterprise-grade Role-Based Access Control (RBAC) service for Cloud Kitchen application with JWT authentication, OTP verification, and multi-tenant support.

## 📋 Overview

Authentication and authorization microservice providing secure user management, role-based access control, and JWT token generation for the Cloud Kitchen platform.

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    Spring Boot Application                       │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │              Controller Layer                             │  │
│  │  - AuthController                                         │  │
│  │  - UserController                                         │  │
│  │  - MerchantController                                     │  │
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
│  │  - FileUploadService                                      │  │
│  └────────────────────┬─────────────────────────────────────┘  │
│                       │                                          │
│  ┌────────────────────▼─────────────────────────────────────┐  │
│  │              Repository Layer                             │  │
│  │  - UserRepository                                         │  │
│  │  - RoleRepository                                         │  │
│  │  - MerchantRepository                                     │  │
│  └────────────────────┬─────────────────────────────────────┘  │
└────────────────────────┼────────────────────────────────────────┘
                         │
                         ▼
                  ┌──────────────┐
                  │  PostgreSQL  │
                  └──────────────┘
```

## 📁 Project Structure

```
src/main/java/com/cloudkitchen/rbac/
├── config/                      # Configuration
├── controller/                  # REST Controllers
├── domain/entity/              # JPA Entities
├── dto/                        # Data Transfer Objects
├── repository/                 # Data Access Layer
├── service/                    # Business Logic
├── security/                   # Security & JWT
├── exception/                  # Exception Handling
└── util/                       # Utilities
```

## 🛠️ Technology Stack

- **Java 21**
- **Spring Boot 3.3.13**
- **Spring Security** - Authentication & Authorization
- **Spring Data JPA** - Database ORM
- **PostgreSQL** - Database
- **JWT (JJWT 0.12.6)** - Token-based authentication
- **BCrypt** - Password hashing
- **SpringDoc OpenAPI 2.6.0** - API documentation
- **AWS S3** - File storage
- **Maven** - Build tool

## 🚀 Getting Started

### Prerequisites

- Java 21+
- Maven 3.8+
- PostgreSQL 14+

### Installation

1. **Clone repository**

```bash
git clone <repository-url>
cd cloud-kitchen-rbac-service
```

2. **Configure environment**

```bash
cp .env.example .env
# Edit .env with your configuration
```

3. **Create database**

```sql
CREATE DATABASE cloud_kitchen_rbac;
```

4. **Run application**

```bash
mvn clean install
mvn spring-boot:run
```

5. **Access**

- API: `http://localhost:8081`
- Swagger: `http://localhost:8081/swagger-ui.html`
- Health: `http://localhost:8081/actuator/health`

## ⚙️ Configuration

### Environment Variables (.env)

```properties
# Database
DATABASE_URL=jdbc:postgresql://localhost:5432/cloud_kitchen_rbac
DB_USERNAME=postgres
DB_PASSWORD=your_password

# JWT (minimum 32 characters)
JWT_SECRET=your-secure-jwt-secret-key-minimum-32-characters
JWT_ACCESS_EXPIRY=86400
JWT_REFRESH_EXPIRY=604800

# AWS S3
AWS_ACCESS_KEY=your_access_key
AWS_SECRET_KEY=your_secret_key
AWS_S3_BUCKET=your_bucket_name
AWS_REGION=us-east-1

# Application
PORT=8081
```

## 📚 API Endpoints

### Authentication

```http
POST   /api/auth/signup          # Customer registration
POST   /api/auth/login           # Merchant/Admin login
POST   /api/auth/otp/request     # Request OTP
POST   /api/auth/otp/verify      # Verify OTP
POST   /api/auth/refresh         # Refresh token
POST   /api/auth/logout          # Logout
```

### User Management

```http
GET    /api/users                # Get all users (Admin)
GET    /api/users/{id}           # Get user by ID
PUT    /api/users/{id}           # Update user
DELETE /api/users/{id}           # Delete user
```

### Merchant Management

```http
POST   /api/merchants            # Create merchant
GET    /api/merchants            # Get all merchants
GET    /api/merchants/{id}       # Get merchant by ID
PUT    /api/merchants/{id}       # Update merchant
```

### File Upload

```http
POST   /api/files/upload         # Upload file to S3
```

## 🗄️ Database Schema

### Core Tables

- `users` - User accounts
- `roles` - System roles
- `permissions` - System permissions
- `user_roles` - User-role mapping
- `role_permissions` - Role-permission mapping
- `merchants` - Merchant information
- `otp_logs` - OTP audit trail

## 🔒 Security

- JWT-based authentication
- BCrypt password hashing
- Role-based access control
- OTP verification (10-minute expiry, 3 attempts)
- Rate limiting (5 requests per 15 minutes)
- Token blacklisting on logout
- CORS configuration
- Input validation

## 📊 Monitoring

```bash
# Health check
curl http://localhost:8081/actuator/health

# Metrics
curl http://localhost:8081/actuator/metrics
```

## 🧪 Testing

```bash
mvn test
```

## 📝 Documentation

- Swagger UI: `http://localhost:8081/swagger-ui.html`
- API Docs: `http://localhost:8081/v3/api-docs`

## 🚢 Deployment

### Docker

```bash
docker-compose up -d
```

### Production Checklist

- [ ] Set strong JWT_SECRET (32+ characters)
- [ ] Configure production database
- [ ] Set CORS_ORIGINS to production domains
- [ ] Enable HTTPS/SSL
- [ ] Configure firewall rules
- [ ] Set up database backups
- [ ] Configure log rotation
- [ ] Set up monitoring

## 📄 License

Proprietary - All rights reserved

## 👥 Support

For support: support@cloudkitchen.com

---

**Built with ❤️ by Cloud Kitchen Team**
