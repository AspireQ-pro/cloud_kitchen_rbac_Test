# Cloud Kitchen RBAC Service

This is a production-grade Spring Boot microservice for Role-Based Access Control (RBAC) in a cloud kitchen environment.

## Features
- User and Role management
- Repository, Service, and Controller layers
- RESTful API with Swagger UI documentation
- Supports PostgreSQL (prod) and H2 (dev)
- Ready for AWS EC2 deployment

## Getting Started

### Prerequisites
- Java 11+
- Maven 3.6+
- PostgreSQL (for production)

### Running Locally (H2 In-Memory)
```
mvn clean spring-boot:run
```

### Running with PostgreSQL
1. Update `src/main/resources/application.properties` with your PostgreSQL credentials.
2. Start the application:
```
mvn clean spring-boot:run
```

### API Documentation
- Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

### H2 Console (Dev Only)
- [http://localhost:8080/h2-console](http://localhost:8080/h2-console)

## Project Structure
- `domain/` - JPA entities
- `repository/` - Spring Data repositories
- `service/` - Business logic
- `controller/` - REST endpoints
- `config/` - Configuration (Swagger, etc.)

## Deployment
- Build the JAR: `mvn clean package`
- Deploy to AWS EC2 or any cloud VM

## License
MIT
