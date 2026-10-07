# VerySmartBus — Spring Boot Starter

## Stack
| Layer | Technology |
|---|---|
| Framework | Spring Boot 3.3.4 |
| Language | Java 25 |
| Database | PostgreSQL |
| Migrations | Flyway |
| ORM | Spring Data JPA / Hibernate |
| Security | Spring Security + JWT (JJWT 0.12) |
| Validation | Jakarta Bean Validation |
| Mapping | MapStruct |
| Boilerplate | Lombok |
| Docs | SpringDoc OpenAPI (Swagger UI) |
| Monitoring | Spring Actuator |

## Quick Start

### 1. Create the PostgreSQL database
```sql
CREATE DATABASE verysmartbus_db;
```

### 2. Configure credentials
Edit `src/main/resources/application.yml`:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/verysmartbus_db
    username: YOUR_USER
    password: YOUR_PASSWORD
```

### 3. Run
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### 4. Swagger UI
Open: http://localhost:8080/swagger-ui.html

### 5. Actuator health
Open: http://localhost:8080/actuator/health

## Flyway Migrations
Add new migration files under:
```
src/main/resources/db/migration/
```
Naming convention: `V{version}__{description}.sql`  
Example: `V2__add_bus_table.sql`

## Project Structure
```
src/main/java/com/verysmartbus/
├── VerySmartBusApplication.java
├── common/
│   └── ApiResponse.java          # Unified response wrapper
├── config/
│   └── SecurityConfig.java
└── exception/
    ├── GlobalExceptionHandler.java
    └── ResourceNotFoundException.java
```
