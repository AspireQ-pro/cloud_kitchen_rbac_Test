# Cloud Kitchen RBAC Service - Deployment Guide

## 🚀 Quick Start for DevOps Team

### Prerequisites
- AWS EC2 instance (t3.medium or higher recommended)
- PostgreSQL database (RDS recommended)
- Docker and Docker Compose installed
- Java 17 runtime

### 1. Environment Setup

#### Option A: Docker Deployment (Recommended)
```bash
# Clone repository
git clone <repository-url>
cd cloud-kitchen-rbac-service

# Copy environment template
cp .env.template .env

# Update .env with actual values:
# - DATABASE_URL (RDS endpoint)
# - DB_USERNAME, DB_PASSWORD
# - JWT_SECRET (generate secure 32+ char string)

# Deploy with Docker Compose
docker-compose up -d
```

#### Option B: Direct JAR Deployment
```bash
# Build application
mvn clean package -DskipTests

# Copy JAR to EC2
scp target/rbac-service-1.0.0.jar ec2-user@your-ec2-ip:/opt/cloud-kitchen-rbac/

# Run with production profile
java -jar rbac-service-1.0.0.jar --spring.profiles.active=prod
```

### 2. Database Setup

#### Create Database Schema
```sql
-- Connect to PostgreSQL and run:
CREATE DATABASE cloud_kitchen_rbac;

-- Run initialization script:
\i scripts/init.sql
```

### 3. Environment Variables (Required)

| Variable | Description | Example |
|----------|-------------|---------|
| `DATABASE_URL` | PostgreSQL connection string | `jdbc:postgresql://rds-endpoint:5432/cloud_kitchen_rbac` |
| `DB_USERNAME` | Database username | `postgres` |
| `DB_PASSWORD` | Database password | `secure_password_123` |
| `JWT_SECRET` | JWT signing key (32+ chars) | `your_secure_jwt_secret_key_here` |
| `PORT` | Application port | `8081` |

### 4. Health Checks

- **Health Endpoint**: `GET /actuator/health`
- **Application Status**: `GET /actuator/info`
- **Docker Health**: Built-in container health checks

### 5. Security Configuration

#### Firewall Rules (Security Groups)
```
Inbound Rules:
- Port 8081: Application traffic
- Port 22: SSH access (restrict to your IP)
- Port 5432: Database (internal VPC only)

Outbound Rules:
- All traffic allowed
```

#### SSL/TLS (Production)
- Use Application Load Balancer with SSL certificate
- Terminate SSL at ALB level
- Internal communication over HTTP

### 6. Monitoring & Logging

#### Application Logs
```bash
# Docker logs
docker logs rbac-service -f

# Direct deployment logs
journalctl -u cloud-kitchen-rbac -f
```

#### Key Metrics to Monitor
- Application startup time
- Database connection pool usage
- JWT token generation/validation
- API response times
- Error rates

### 7. Testing Endpoints

#### Health Check
```bash
curl http://your-ec2-ip:8081/actuator/health
```

#### API Documentation (Development Only)
```bash
# Only available in dev profile
curl http://your-ec2-ip:8081/swagger-ui.html
```

#### Sample API Calls
```bash
# Customer Registration
curl -X POST http://your-ec2-ip:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "phone": "9876543210",
    "email": "test@example.com",
    "firstName": "Test",
    "lastName": "User",
    "city": "Mumbai",
    "state": "Maharashtra",
    "pincode": "400001"
  }'

# Customer Login
curl -X POST http://your-ec2-ip:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "phone": "9876543210",
    "password": "password123"
  }'
```

### 8. Troubleshooting

#### Common Issues

1. **Database Connection Failed**
   - Check DATABASE_URL format
   - Verify security group allows port 5432
   - Confirm database credentials

2. **Application Won't Start**
   - Check Java 17 is installed
   - Verify all environment variables are set
   - Check port 8081 is available

3. **JWT Token Issues**
   - Ensure JWT_SECRET is set and 32+ characters
   - Check token expiration settings

#### Log Analysis
```bash
# Check application startup
grep "Started CloudKitchenRbacApplication" /var/log/cloud-kitchen-rbac.log

# Check database connections
grep "HikariPool" /var/log/cloud-kitchen-rbac.log

# Check for errors
grep "ERROR" /var/log/cloud-kitchen-rbac.log
```

### 9. Scaling Considerations

#### Horizontal Scaling
- Application is stateless (can run multiple instances)
- Use Application Load Balancer for distribution
- Shared PostgreSQL database

#### Performance Tuning
- Adjust `DB_POOL_SIZE` based on load
- Monitor JVM heap usage
- Consider Redis for session management (future)

### 10. Backup & Recovery

#### Database Backups
```bash
# Create backup
pg_dump -h rds-endpoint -U postgres cloud_kitchen_rbac > backup.sql

# Restore backup
psql -h rds-endpoint -U postgres cloud_kitchen_rbac < backup.sql
```

## 📞 Support

For deployment issues, contact the development team with:
- EC2 instance details
- Application logs
- Database connection status
- Environment variable configuration (without sensitive values)

## 🔄 CI/CD Integration

This deployment setup is ready for:
- GitHub Actions
- AWS CodePipeline
- Jenkins
- GitLab CI/CD

See `scripts/deploy.sh` for automated deployment pipeline integration.