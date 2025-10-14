# Multi-stage build for production optimization with Java 21
FROM maven:3.9.4-eclipse-temurin-21 AS builder

# Set working directory
WORKDIR /app

# Copy pom.xml and download dependencies (for better caching)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build application
RUN mvn clean package -DskipTests -B

# Production stage with Java 21
FROM eclipse-temurin:21-jre-alpine

# Install curl for health checks and set timezone
RUN apk add --no-cache curl tzdata && \
    cp /usr/share/zoneinfo/Asia/Kolkata /etc/localtime && \
    echo "Asia/Kolkata" > /etc/timezone && \
    apk del tzdata

# Create non-root user for security
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

# Set working directory
WORKDIR /app

# Copy jar from builder stage
COPY --from=builder /app/target/rbac-service-*.jar app.jar

# Change ownership to non-root user
RUN chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# Set environment variables for S3 and performance
ENV TZ=Asia/Kolkata \
    JAVA_OPTS="-Xmx1024m -Xms512m -XX:+UseG1GC -XX:+UseContainerSupport -Duser.timezone=Asia/Kolkata -Djava.security.egd=file:/dev/./urandom" \
    SERVER_PORT=8081 \
    AWS_REGION=us-east-1

# Expose port
EXPOSE 8081

# Health check with longer timeout for S3 initialization
HEALTHCHECK --interval=30s --timeout=15s --start-period=120s --retries=3 \
    CMD curl -f http://localhost:8081/actuator/health || exit 1

# Run application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]