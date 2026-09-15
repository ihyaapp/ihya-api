# Ihya API

Backend API for **Ihya**, a Sunnah habit-tracking application. Built with Spring Boot (Java 25).

## Stack

- Spring Boot 4.1 (Web MVC, Validation, Security, Actuator, Data JPA)
- PostgreSQL + Flyway migrations
- JWT-based authentication (`jjwt`)
- Maven (via the included wrapper, `mvnw`)

## Modules

- `identity` — auth (login/register/refresh), JWT issuing/validation, users
- `profile` — user profile data
- `catalogue` — categories and Sunnahs
- `dailypractice` — placeholder, not yet implemented
- `common` — shared web/error-handling infrastructure

## Running locally

1. Start Docker Desktop, then start Postgres:

   ```bash
   docker compose up -d
   ```

   Requires a `.env` file (gitignored) next to `docker-compose.yml`:

   ```text
   DB_PASSWORD=your_password
   ```

2. Export the same password for the Spring process (Docker Compose reads `.env` automatically, but the JVM needs it in the shell environment too):

   ```bash
   export DB_PASSWORD=your_password
   ```

3. Run the app:

   ```bash
   ./mvnw spring-boot:run
   ```

## Testing

```bash
mvn --batch-mode verify
```

This is the same command CI runs — reproduce it locally before pushing.

## Docs

- [docs/api-contract.md](docs/api-contract.md) — API request/response contract
- [docs/development/development-workflow.md](docs/development/development-workflow.md) — full local dev workflow, troubleshooting, and Git conventions
- [src/main/resources/openapi](src/main/resources/openapi) — OpenAPI specs for `identity` and `profile`
