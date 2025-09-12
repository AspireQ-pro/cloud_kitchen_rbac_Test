#!/bin/bash

# Cloud Kitchen RBAC Service Deployment Script
set -e

echo "🚀 Starting deployment process..."

# Build the application
echo "📦 Building application..."
mvn clean package -DskipTests

# Build Docker image
echo "🐳 Building Docker image..."
docker build -t cloud-kitchen-rbac:latest .

# Tag for ECR (update with your ECR repository)
# docker tag cloud-kitchen-rbac:latest your-account.dkr.ecr.region.amazonaws.com/cloud-kitchen-rbac:latest

# Push to ECR (uncomment when ready)
# echo "📤 Pushing to ECR..."
# docker push your-account.dkr.ecr.region.amazonaws.com/cloud-kitchen-rbac:latest

echo "✅ Deployment preparation complete!"
echo "📋 Next steps:"
echo "   1. Update ECR repository URL in this script"
echo "   2. Configure environment variables on EC2"
echo "   3. Run docker-compose up -d on EC2 instance"