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

## CI/CD (deploy to VPS)
`.github/workflows/ci-cd.yml`:
- **Pull requests to `main`** → `mvn verify` on JDK 17.
- **Push/merge to `main`** → tests → Docker image pushed to `ghcr.io/<owner>/<repo>:<sha>` (+ `latest`) → SSH to the VPS → `deploy/deploy.sh` pulls the image, restarts it with Docker Compose next to a local PostGIS container (`verysmartbus-db`), waits for `/actuator/health`, and rolls back to the previous image if it never becomes healthy.

### One-time VPS setup
1. Install Docker + the Compose plugin and `curl`; create a deploy user in the `docker` group.
2. Create the deploy directory (default `/opt/verysmartbus`) owned by that user.
3. Create `/opt/verysmartbus/.env` from [`deploy/.env.example`](deploy/.env.example) and set `POSTGRES_PASSWORD`, `JWT_SECRET` and `CORS_ALLOWED_ORIGINS` (the frontend URL). `POSTGRES_PASSWORD` is only applied when the database volume is first created.
4. Add the deploy public key to the user's `~/.ssh/authorized_keys`.

### GitHub settings (Settings → Secrets and variables → Actions)
| Name | Kind | Required | Value |
|---|---|---|---|
| `VPS_HOST` | secret | yes | VPS IP / hostname |
| `VPS_USER` | secret | yes | deploy user |
| `VPS_SSH_KEY` | secret | yes | private key (e.g. `ssh-keygen -t ed25519`) |
| `VPS_PORT` | secret | no | SSH port, default `22` |
| `VPS_KNOWN_HOSTS` | secret | recommended | output of `ssh-keyscan -p <port> <host>`; falls back to keyscan at deploy time |
| `DEPLOY_PATH` | variable | no | default `/opt/verysmartbus` |

The deploy job runs in the `production` environment, so you can add required reviewers there to gate deployments.

### Production database
PostgreSQL + PostGIS runs in the `verysmartbus-db` container (port bound to `127.0.0.1` only); data lives in the `verysmartbus_postgres_data` Docker volume and survives redeploys. PostGIS is installed in the `extensions` schema, as on Supabase. Memory is tuned for a 2 GB server (app heap `-Xmx512m`, Postgres `shared_buffers=256MB`).
- Copy data from Supabase (one-time, replaces the local `public` schema): `cd /opt/verysmartbus && bash migrate-from-supabase.sh`
- psql on the VPS: `docker exec -it verysmartbus-db psql -U postgres -d smart_bus_db`
- From your PC (DBeaver/pgAdmin): `ssh -L 5432:localhost:5432 deploy@<vps-ip>`, then connect to `localhost:5432`
- Backup: `docker exec verysmartbus-db pg_dump -U postgres -Fc smart_bus_db > backup.dump`

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
