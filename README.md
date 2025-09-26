# Cloud Kitchen RBAC Service

## Overview
Enterprise-grade Role-Based Access Control (RBAC) service for Cloud Kitchen applications with multi-tenant architecture, comprehensive security features, and industry-standard practices.

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

### Project Structure
```
src/main/java/com/cloudkitchen/rbac/
├── annotation/          # Custom annotations
├── aspect/             # AOP aspects for cross-cutting concerns
├── config/             # Configuration classes
├── constants/          # Application constants and error codes
├── controller/         # REST controllers
├── domain/entity/      # JPA entities
├── dto/               # Data Transfer Objects
│   ├── auth/          # Authentication DTOs
│   ├── common/        # Common DTOs
│   ├── customer/      # Customer DTOs
│   └── merchant/      # Merchant DTOs
├── enums/             # Enumerations
├── exception/         # Exception handling
├── repository/        # Data access layer
├── security/          # Security configuration
├── service/           # Business logic
│   └── impl/          # Service implementations
├── util/              # Utility classes
└── validation/        # Input validation
```

### Security Features

#### Authentication & Authorization
- JWT tokens with RS256 signing
- Refresh token rotation
- Token blacklisting
- Role-based permissions
- Multi-tenant data isolation

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

### Authentication
- `POST /api/auth/signup` - User registration
- `POST /api/auth/login` - Password-based login
- `POST /api/auth/otp/request` - Request OTP
- `POST /api/auth/otp/verify` - Verify OTP
- `POST /api/auth/refresh` - Refresh tokens
- `POST /api/auth/logout` - Logout

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

### Production Deployment
1. Use strong JWT secrets (256-bit minimum)
2. Enable HTTPS only
3. Configure proper CORS origins
4. Set up Redis for token blacklisting
5. Enable audit logging
6. Configure rate limiting
7. Set up monitoring and alerting
8. Regular security updates

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

## Development

### Prerequisites
- Java 17+
- Maven 3.8+
- PostgreSQL 13+
- Redis 6+ (optional)

### Setup
1. Clone repository
2. Configure database
3. Set environment variables
4. Run `mvn spring-boot:run`

### Code Quality
- SpotBugs for static analysis
- OWASP dependency check
- SonarQube integration
- Checkstyle enforcement

## License
MIT License

## Support
For support and questions, please contact the development team.
