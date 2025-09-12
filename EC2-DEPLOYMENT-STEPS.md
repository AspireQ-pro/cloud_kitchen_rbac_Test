# EC2 Deployment Steps for DevOps Team

## 🚀 Changes Required for EC2 Deployment

### 1. **Environment Variables to Set on EC2**
```bash
# Database Configuration
export DATABASE_URL="jdbc:postgresql://54.146.236.133:5432/cloud_kitchen_rbac"
export DB_USERNAME="postgres"
export DB_PASSWORD="Yogesh@1234"

# JWT Configuration
export JWT_SECRET="q2M7w1SiV5XDAXEmVqg9gZWCXfF50l11FqJR2aSC1a8="

# Server Configuration
export PORT="8081"
export SPRING_PROFILE="qa"
```

### 2. **EC2 Security Group Rules**
```
Inbound Rules:
- Port 8081: 0.0.0.0/0 (Application access)
- Port 22: Your-IP/32 (SSH access)
- Port 5432: EC2-to-RDS only (Database access)
```

### 3. **Deployment Commands**
```bash
# 1. Upload JAR to EC2
scp target/rbac-service-1.0.0.jar ec2-user@your-ec2-ip:/opt/app/

# 2. SSH to EC2 and set environment
ssh ec2-user@your-ec2-ip
export DATABASE_URL="jdbc:postgresql://54.146.236.133:5432/cloud_kitchen_rbac"
export DB_USERNAME="postgres"
export DB_PASSWORD="Yogesh@1234"
export JWT_SECRET="q2M7w1SiV5XDAXEmVqg9gZWCXfF50l11FqJR2aSC1a8="

# 3. Run application
java -jar /opt/app/rbac-service-1.0.0.jar --spring.profiles.active=qa
```

### 4. **Docker Deployment (Alternative)**
```bash
# 1. Build and push to ECR
docker build -t cloud-kitchen-rbac .
docker tag cloud-kitchen-rbac:latest your-account.dkr.ecr.region.amazonaws.com/cloud-kitchen-rbac:latest
docker push your-account.dkr.ecr.region.amazonaws.com/cloud-kitchen-rbac:latest

# 2. Run on EC2
docker run -d \
  -p 8081:8081 \
  -e DATABASE_URL="jdbc:postgresql://54.146.236.133:5432/cloud_kitchen_rbac" \
  -e DB_USERNAME="postgres" \
  -e DB_PASSWORD="Yogesh@1234" \
  -e JWT_SECRET="q2M7w1SiV5XDAXEmVqg9gZWCXfF50l11FqJR2aSC1a8=" \
  -e SPRING_PROFILE="qa" \
  --name rbac-service \
  your-account.dkr.ecr.region.amazonaws.com/cloud-kitchen-rbac:latest
```

## 🔧 **No Code Changes Required**

The application is already configured to work on EC2:
- ✅ `server.address=0.0.0.0` (accepts external connections)
- ✅ Environment variable support in QA/Prod profiles
- ✅ Database URL already points to EC2 instance (54.146.236.133)

## 📋 **Testing After Deployment**

### Health Check
```bash
curl http://your-ec2-ip:8081/actuator/health
```

### API Test
```bash
curl -X POST http://your-ec2-ip:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "phone": "9876543210",
    "email": "test@example.com",
    "firstName": "Test",
    "lastName": "User"
  }'
```

### Swagger UI
```
http://your-ec2-ip:8081/swagger-ui.html
```

## 🎯 **Summary for DevOps**

**ZERO code changes needed** - just set environment variables and deploy the existing JAR file to EC2. The application will automatically use the external database and work correctly.