# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

Ihya API — Spring Boot 4.1 / Java 25 backend for a Sunnah habit-tracking app. PostgreSQL + Flyway, hand-rolled JWT auth (no Cognito/Auth0). The canonical API contract and the ordered build roadmap live in [docs/api-contract.md](docs/api-contract.md) (section 3, "Reconciliation") — read it before planning new work; it records which endpoints are shipped vs. planned and in what order. Full local setup: [README.md](README.md). Daily dev loop, troubleshooting, and Git conventions: [docs/development/development-workflow.md](docs/development/development-workflow.md).

## Commands

```bash
# Start Postgres (needs a .env file with DB_PASSWORD next to docker-compose.yml)
docker compose up -d

# Export the same password for the JVM (Docker Compose reads .env; Spring/Maven don't)
export DB_PASSWORD=your_password

# Compile only
./mvnw clean compile

# Full verification — same command CI runs; reproduce this before pushing
./mvnw --batch-mode verify

# Run a single test class
./mvnw test -Dtest=CategoryServiceTest

# Run a single test method
./mvnw test -Dtest=AuthControllerIntegrationTest#login_wrongPassword_returns401WithInvalidCredentialsShape

# Run the app locally — the `local` profile is required; application.yml has no
# datasource config on its own (see dev-workflow.md §6.1)
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

There is no configured linter/formatter plugin (no Checkstyle/Spotless in `pom.xml`) — `verify` is the whole local gate.

**Test profiles matter.** Integration tests (e.g. `AuthControllerIntegrationTest`) explicitly declare `@ActiveProfiles("ci")`, which points at Postgres on `localhost:5432` (`application-ci.yml`) — the same `ihya-postgres` Docker container serves both local runs and CI's Postgres service container. A `-Dspring.profiles.active=...` flag on the outer `mvn` command does **not** reach the forked Surefire test JVM; profile activation must happen via `@ActiveProfiles` on the test class or the `pom.xml` Surefire config. See dev-workflow.md §6 and §18 before touching test/profile config — it documents a real incident (stale Docker volume credentials look identical to a genuine password mismatch).

## Architecture

**Modules are domain packages, not technical layers.** Under `com.ihya.api`: `identity` (auth, users, JWT), `profile` (display name, prefs), `catalogue` (categories/Sunnahs), `dailypractice` (assignments, practices, streak/milestone progress), `notification` (notification preferences, push tokens, the in-app notification feed), `common.web` (shared HTTP infrastructure). Each module owns its entities, repository, service, and controller together; there's no repo-wide `controllers`/`services` split.

**Two-tier exception handling.** `common.web.GlobalExceptionHandler` (`@Order(LOWEST_PRECEDENCE)`) is module-agnostic: it handles `IllegalArgumentException` → 400, `@Valid` binding failures → 400 (all field errors joined, not just the first), unparseable JSON → 400, and a catch-all `Exception` → 500 (logged server-side, never echoed to the client). Each module additionally owns its own `@RestControllerAdvice` at `@Order(HIGHEST_PRECEDENCE)` (`IdentityExceptionHandler`, `CatalogueExceptionHandler`) for its own domain exceptions (e.g. `EmailAlreadyRegisteredException` → 409). Both tiers render the same `ErrorResponse` shape (`status`/`error`/`message`/`timestamp`). Adding a new module's HTTP surface means adding its own advice class, not editing the shared one.

**Auth pipeline.** `JwtAuthenticationFilter` runs before `UsernamePasswordAuthenticationFilter`, reads `Authorization: Bearer <jwt>`, and populates the `SecurityContext` — it never rejects a request itself (a missing/invalid token just leaves the request unauthenticated). `SecurityConfig` permits `/v1/auth/**` and requires authentication on everything else; `RestAuthenticationEntryPoint` renders unauthenticated hits in the same `ErrorResponse` shape instead of Spring Security's default. Access tokens are signed JWTs (15 min). Refresh tokens are opaque 64-byte random strings, SHA-256-hashed at rest, rotated on every use; presenting an already-revoked refresh token is treated as theft and revokes every token for that user (`RefreshTokenService.validateAndRotate` / `revokeAllForUser`).

**Route prefix.** `common.web.WebConfig` applies `/v1` to every `@RestController` via `WebMvcConfigurer.configurePathMatch` + `HandlerTypePredicate.forAnnotation(RestController.class)` — deliberately not `server.servlet.context-path`, which would also move Actuator's `/actuator/health` under `/v1`. This prefixing happens at handler-mapping registration, not URL rewriting, so `SecurityConfig`'s `requestMatchers` must independently reference the real prefixed path (`/v1/auth/**`) — Spring Security sees the raw incoming request path before MVC dispatch applies the prefix.

**Persist-and-remap, not check-then-insert.** Services that enforce uniqueness (`UserService.register` on email, `CategoryService` on category name) call `saveAndFlush` inside a `try`, catch `DataIntegrityViolationException`, and remap it to a typed domain exception only when the cause matches the specific Postgres constraint name (e.g. `users_email_key`) — anything else propagates. This closes the race where two concurrent requests both pass an application-level existence check. Follow this pattern for any new uniqueness constraint rather than checking-then-inserting.

**Cross-module composition.** `UserService.register` is `@Transactional` and calls `ProfileService.createProfile(userId)` as part of the same transaction — a profile row always exists for every user; there is no lazy-create path and no case where profile lookup can 404.

**Migrations.** Flyway, versioned SQL under `src/main/resources/db/migration` (`V1__init.sql` onward). Migrations are additive/forward-only — never edit a shipped migration file; add a new `Vn__description.sql`. Verify against a fresh database (`docker compose down -v && docker compose up -d`) when a migration changes shape, per dev-workflow.md §8.

**OpenAPI specs** under `src/main/resources/openapi/` (`identity-api.yaml`, `profile-api.yaml`, `catalogue-api.yaml`, `dailypractice-api.yaml`, `notification-api.yaml`) are hand-maintained documentation, not codegen input — keep them in sync with controllers by hand when routes change. `profile-api.yaml` documents that the module has no standalone HTTP surface of its own (superseded by identity's composite `/v1/me`).

## Working with this repo

The owner is a beginner, building this as a portfolio project they need to defend in interviews. That changes how to help, not just what to build:

- **Teach before/while building.** Explain what a step does and *why*, in plain terms, before touching code — not just what to type.
- **Don't make code changes unless explicitly told to proceed.** Default mode is explaining a phase/step and then handing over **step-by-step manual instructions** (exact files, exact methods, no pasted-in solution) for the owner to type themselves in IntelliJ. Only write code directly when they explicitly ask for that instead (e.g. "just implement it").
- **Phase-gated progress.** The build is broken into phases (tracked in a live roadmap artifact — ask the owner for the link if you don't have it, or check `Artifact` list). Do not start the next phase's work until the owner explicitly says something like "let's do phase N." Answering questions about later phases is fine; building them isn't.
- **Keep the roadmap artifact in sync.** When a phase's status changes (started/finished), update that artifact rather than leaving the conversation as the only record.
- Every commit in this repo follows Conventional Commits and stays atomic (dev-workflow.md §12) — suggest commit messages but don't commit unless asked.
