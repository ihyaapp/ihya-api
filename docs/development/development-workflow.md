# Ihya Daily Development Workflow

## Purpose

This document defines the standard local development workflow for Ihya.

The goal is to:

- keep the local development environment predictable
- catch errors before pushing to GitHub
- reproduce important CI checks locally
- avoid configuration drift
- keep database and application environments separate
- leave the repository in a known state at the end of each development session

---

# 1. Start of Day

## 1.1 Start Docker Desktop

Ensure Docker Desktop is running.

Check the Ihya containers:

```bash
docker ps
```

The PostgreSQL container should be running:

```text
ihya-postgres
```

If it is not running:

```bash
docker compose up -d
```

Verify PostgreSQL:

```bash
docker exec -it ihya-postgres pg_isready -U ihya -d ihya
```

Expected:

```text
accepting connections
```

### Important

Ihya uses PostgreSQL through Docker.

A separate PostgreSQL installation on Windows should not compete for port `5432`.

---

# 2. Open the Project

Navigate to the backend repository:

```bash
cd /d/Ihya/ihya-api
```

Check the repository state:

```bash
git status
```

Confirm:

- correct branch
- no unexpected changes
- no unfinished work from a previous session

---

# 3. Review the Current GitHub Issue

Before coding, identify:

- Issue number
- Issue title
- problem being solved
- acceptance criteria
- current implementation task
- dependencies
- today's specific goal

Work on **one GitHub Issue at a time**.

Do not introduce unrelated work into the current issue.

---

# 4. Implement in Small Steps

Use this development loop:

```text
Understand
    ↓
Make a small change
    ↓
Run relevant test
    ↓
Inspect result
    ↓
Continue
```

Avoid making many unrelated changes before testing.

---

# 5. Local Verification

## 5.1 Compile

```bash
mvn clean compile
```

## 5.2 Run the Full Verification

Before pushing:

```bash
mvn --batch-mode verify
```

Expected:

```text
Tests run: X, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

This is the primary local verification command for the backend.

---

# 6. Spring Profiles

Ihya uses Spring profiles for environment-specific configuration.

For example:

```text
application.yml
application-ci.yml
```

The CI configuration is activated with:

```text
SPRING_PROFILES_ACTIVE=ci
```

Integration tests that require the CI configuration should explicitly activate it:

```java
@ActiveProfiles("ci")
```

Do not assume that `application-ci.yml` is automatically active.

---

# 7. Database Verification

Perform additional database verification when the current change affects:

- migrations
- tables
- indexes
- constraints
- datasource configuration

Connect to PostgreSQL:

```bash
docker exec -it ihya-postgres psql -U ihya -d ihya
```

Check tables:

```sql
\dt
```

Check Flyway migration history:

```sql
SELECT version, description, success
FROM flyway_schema_history
ORDER BY installed_rank;
```

For a new migration, verify:

- migration exists
- migration succeeded
- expected tables exist
- expected columns exist
- expected constraints exist
- schema version is correct

---

# 8. Fresh Database Verification

When implementing a database migration, verify the migration against a fresh database when practical.

The expected flow is:

```text
Empty PostgreSQL database
        ↓
V1
        ↓
V2
        ↓
Expected schema
```

If local database data can safely be deleted:

```bash
docker compose down -v
docker compose up -d
```

Then verify:

```bash
docker exec -it ihya-postgres pg_isready -U ihya -d ihya
```

Run the application or tests and verify Flyway history.

### Warning

`docker compose down -v` deletes Docker volumes and therefore deletes local PostgreSQL data.

Do not use it casually.

---

# 9. CI-Equivalent Verification

Before pushing, reproduce the important CI command locally.

If CI runs:

```bash
mvn --batch-mode verify
```

run the same command locally.

The goal is to catch predictable CI failures before pushing.

Local development credentials and CI credentials do not need to be the same.

---

# 10. Secrets and Credentials

Never commit:

- passwords
- API keys
- access tokens
- private keys
- other secrets

Local credentials should be provided through environment variables or local environment configuration.

CI credentials should be separate from local development credentials.

Never put real credentials directly into:

- Java source code
- `application.yml`
- `application-ci.yml`
- GitHub workflow files
- committed documentation

---

# 11. Git Pre-Push Verification

Before committing or pushing:

## Check repository state

```bash
git status
```

## Inspect the diff

```bash
git diff
```

Verify that only intended files changed.

## Check whitespace errors

```bash
git diff --check
```

## Run full backend verification

```bash
mvn --batch-mode verify
```

## Database verification

Perform database-specific checks if the current issue changes the database.

---

# 12. Commit

Create atomic commits.

Each commit should contain one logically complete change.

Use Conventional Commits.

Examples:

```text
feat:
fix:
refactor:
test:
docs:
chore:
```

Example:

```bash
git add <specific-files>
git commit -m "fix(ci): configure Spring test profile"
```

---

# 13. Push

Push only after local verification passes:

```bash
git push origin <branch>
```

Then check GitHub Actions.

---

# 14. GitHub Actions Verification

After pushing:

1. Open the GitHub Actions run.
2. Confirm the workflow starts.
3. Confirm the correct branch triggered it.
4. Check the build job.
5. Confirm tests pass.
6. Confirm the final result is successful.

Local success does not replace CI verification.

---

# 15. End-of-Day Workflow

Before ending a development session:

## 15.1 Check Git

```bash
git status
```

Determine whether the repository is:

- clean
- committed but not pushed
- pushed and waiting for CI
- partially implemented

## 15.2 If Work Is Complete

Record:

- what was implemented
- tests performed
- CI result
- remaining work
- intentionally postponed work

Ideally leave the working tree clean.

## 15.3 If Work Is Incomplete

Record a checkpoint:

```text
Issue:

Current task:

Completed:
- ...

Not completed:
- ...

Current blocker:
- ...

Next step:
- ...

Important commands/state:
- ...
```

The next session should be able to resume without rediscovering the previous state.

---

# 16. Standard Daily Flow

```text
START DAY
    ↓
Start Docker Desktop
    ↓
docker ps
    ↓
Verify PostgreSQL
    ↓
Open repository
    ↓
git status
    ↓
Review current GitHub Issue
    ↓
Implement small change
    ↓
Run relevant tests
    ↓
Repeat implementation loop
    ↓
git diff
    ↓
git diff --check
    ↓
mvn --batch-mode verify
    ↓
Database verification if required
    ↓
Review final diff
    ↓
Atomic Conventional Commit
    ↓
Push
    ↓
GitHub Actions
    ↓
Verify CI
    ↓
Update issue / PR
    ↓
End-of-day checkpoint
```

---

# 17. Troubleshooting Principle

When something fails, do not immediately change configuration.

Isolate the failing layer first.

For database connectivity, investigate in this order:

```text
Is Docker running?
        ↓
Is PostgreSQL running?
        ↓
Is port 5432 available?
        ↓
Is the correct process listening?
        ↓
Can psql connect?
        ↓
Are credentials correct?
        ↓
Is the Spring profile active?
        ↓
Is Spring datasource configuration correct?
        ↓
Can Flyway connect?
        ↓
Can the application start?
        ↓
Can the integration test start?
```

Test one layer at a time.

This prevents unrelated configuration changes from making the original problem harder to diagnose.

---

# 18. Key Lessons From Issue #5

## Port Conflicts

Multiple PostgreSQL installations can compete for the same port.

Check port `5432` with:

```bash
netstat -ano | findstr :5432
```

On Windows, identify a process using:

```cmd
tasklist /FI "PID eq <PID>"
```

## Spring Profiles

A file named:

```text
application-ci.yml
```

does not automatically activate the `ci` profile.

Activate it through:

```text
SPRING_PROFILES_ACTIVE=ci
```

or explicitly in an integration test:

```java
@ActiveProfiles("ci")
```

## Flyway

Database migrations should be tested from a clean database when introducing schema changes.

## CI

The local developer workflow should reproduce important CI commands before pushing.

## Debugging

Diagnose from the outside inward:

```text
Infrastructure
    ↓
Network
    ↓
Database
    ↓
Configuration
    ↓
Application
    ↓
Tests
```

Do not assume the first error message identifies the root cause.

---

# 19. Definition of "Ready to Push"

A backend change is normally ready to push when:

- [ ] Code implements the current issue only
- [ ] `git diff` contains only intended changes
- [ ] No secrets are present
- [ ] `git diff --check` passes
- [ ] `mvn --batch-mode verify` passes
- [ ] Database changes have been verified if applicable
- [ ] Fresh migration has been tested when appropriate
- [ ] Commit is atomic
- [ ] Commit message follows Conventional Commits

---

# 20. Definition of "Done for the Day"

At the end of a session, I should know:

```text
What issue am I working on?
What did I finish?
What is currently broken?
What is the next task?
Is my work committed?
Is it pushed?
Did CI pass?
What should I do first tomorrow?
```

The repository and development environment should be left in a predictable state.
