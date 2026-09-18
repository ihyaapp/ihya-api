# Ihya API

Backend API for **Ihya**, a Sunnah habit-tracking application. Built with Spring Boot (Java 25).

## Stack

- Spring Boot 4.1 (Web MVC, Validation, Security, Actuator, Data JPA)
- PostgreSQL + Flyway migrations
- JWT-based authentication (`jjwt`)
- Maven (via the included wrapper, `mvnw`)

## Modules

- `identity` — auth (login/register/refresh/logout/forgot-reset password), JWT issuing/validation, users
- `profile` — user profile data
- `catalogue` — categories and Sunnahs
- `dailypractice` — the daily habit loop: assignments, practices, streak/milestone progress
- `notification` — notification preferences, push tokens, and the in-app notification feed
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

3. Run the app (the `local` profile is required — `application.yml` has no datasource
   config on its own; see [development-workflow.md §6.1](docs/development/development-workflow.md#61-running-the-app-locally-spring-bootrun) if this is skipped and startup fails with `Failed to determine a suitable driver class`):

   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=local
   ```

## Testing

```bash
mvn --batch-mode verify
```

This is the same command CI runs — reproduce it locally before pushing.

## Docs

- [docs/api-contract.md](docs/api-contract.md) — API request/response contract
- [docs/development/development-workflow.md](docs/development/development-workflow.md) — full local dev workflow, troubleshooting, and Git conventions
- [src/main/resources/openapi](src/main/resources/openapi) — OpenAPI specs for `identity`, `catalogue`, `dailypractice`, and `notification` (`profile` has no standalone HTTP surface of its own — see `profile-api.yaml`)
