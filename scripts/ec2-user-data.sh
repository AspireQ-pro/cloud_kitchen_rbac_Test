#!/bin/bash

# EC2 User Data Script for Cloud Kitchen RBAC Service
# This script sets up the EC2 instance for deployment

# Update system
yum update -y

# Install Docker
yum install -y docker
systemctl start docker
systemctl enable docker
usermod -a -G docker ec2-user

# Install Docker Compose
curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
chmod +x /usr/local/bin/docker-compose

# Install Java 17 (for direct deployment if needed)
yum install -y java-17-amazon-corretto

# Install PostgreSQL client (for database operations)
yum install -y postgresql15

# Create application directory
mkdir -p /opt/cloud-kitchen-rbac
chown ec2-user:ec2-user /opt/cloud-kitchen-rbac

# Create environment file template
cat > /opt/cloud-kitchen-rbac/.env << 'EOF'
# Database Configuration
DATABASE_URL=jdbc:postgresql://your-rds-endpoint:5432/cloud_kitchen_rbac
DB_USERNAME=postgres
DB_PASSWORD=your_secure_password
DB_POOL_SIZE=20

# JWT Configuration
JWT_SECRET=your_jwt_secret_minimum_32_characters_long
JWT_ACCESS_EXPIRY=3600
JWT_REFRESH_EXPIRY=86400
JWT_EXPIRATION=3600000

# Server Configuration
PORT=8081
EOF

# Set proper permissions
chown ec2-user:ec2-user /opt/cloud-kitchen-rbac/.env
chmod 600 /opt/cloud-kitchen-rbac/.env

# Create systemd service for the application
cat > /etc/systemd/system/cloud-kitchen-rbac.service << 'EOF'
[Unit]
Description=Cloud Kitchen RBAC Service
After=network.target

[Service]
Type=simple
User=ec2-user
WorkingDirectory=/opt/cloud-kitchen-rbac
EnvironmentFile=/opt/cloud-kitchen-rbac/.env
ExecStart=/usr/bin/java -jar rbac-service-1.0.0.jar --spring.profiles.active=prod
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

# Enable the service
systemctl daemon-reload
systemctl enable cloud-kitchen-rbac

echo "EC2 instance setup complete!"
echo "Next steps:"
echo "1. Update .env file with actual values"
echo "2. Upload application JAR to /opt/cloud-kitchen-rbac/"
echo "3. Start service: sudo systemctl start cloud-kitchen-rbac"