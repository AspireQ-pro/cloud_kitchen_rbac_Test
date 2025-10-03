# ========================
# Build Stage
# ========================
FROM maven:3.9.6-eclipse-temurin-17-alpine AS build

# Set working directory
WORKDIR /app

# Copy pom.xml and download dependencies first (better caching)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests -B

# ========================
# Runtime Stage
# ========================
FROM eclipse-temurin:17-jre-alpine

# Create non-root user
RUN addgroup -S appuser && \
    adduser -S appuser -G appuser && \
    mkdir -p /app

WORKDIR /app

# Copy built JAR from build stage
COPY --from=build /app/target/rbac-service-1.0.0.jar app.jar

# Set ownership and switch to non-root user
RUN chown -R appuser:appuser /app
USER appuser

# Expose application port
EXPOSE 8081

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8081/actuator/health || exit 1

# JVM optimizations for container
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -XX:+UseStringDeduplication"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar --spring.profiles.active=${SPRING_PROFILES_ACTIVE:-prod}"]