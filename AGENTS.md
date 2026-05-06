# Agents Guide

This file is a practical guide for human and AI agents working in this repository. It is intentionally repo-specific and should be kept aligned with the actual codebase.

## 1. Repository Summary

- Project name: `worktrack-v1-layered`
- Stack: Java 21, Spring Boot 3.5, Spring Security, Spring Data JPA, Flyway, MySQL/H2, Redis cache, OpenTelemetry, Prometheus, SpringDoc OpenAPI
- Architecture style: layered / n-tier
- Runtime server: Jetty
- Build tool: Gradle
- Main application: `com.worktrack.WorktrackApplication`
- Default HTTP port: `8000`
- Base API prefix: `/layered/api/v1`

This repository is a showcase project for Spring Boot best practices rather than a business-complete product.

## 2. Module Layout

- Root module: main application, unit tests, shared test fixtures
- `integration-test/`: Testcontainers-based integration tests against MySQL
- `performance-test/`: Gatling performance simulations
- `k8s/`: Kubernetes deployment manifest for the `int` profile
- `observability/`: local Grafana/Prometheus/Loki/Tempo stack

Do not edit generated or runtime directories unless the task explicitly requires it:

- `build/`
- `bin/`
- `.gradle/`
- `.bloop/`
- `.metals/`
- `logs/`
- `uploads/`

## 3. Package Conventions

Keep work inside the existing layered structure:

- `controller`: HTTP layer only
- `service`: business logic and transaction boundaries
- `repo`: Spring Data repositories and query logic
- `entity`: JPA entities
- `dto`: request/response records
- `mapper`: entity-to-DTO mapping
- `security`: JWT, auth facade, policies, handlers
- `config`: Spring configuration
- `infra`: cross-cutting infrastructure concerns such as audit, retry, cache, observability, and web logging
- `exception`: custom exceptions and global exception handling

Prefer extending existing packages over inventing parallel structures.

## 4. Domain Model

Core entities:

- `User`
- `Project`
- `Task`

Important enums:

- `Role`: `ADMIN`, `MANAGER`, `EMPLOYEE`
- `Status`: `ACTIVE`, `DELETED`
- `TaskStatus`: `TODO`, `IN_PROGRESS`, `DONE`

The application uses soft delete semantics through the `status` field. Existing repository methods explicitly filter out `Status.DELETED`. Preserve that behavior when adding new queries.

Base entity behavior already includes:

- `id`
- optimistic locking via `version`
- timestamps via `createdAt` / `updatedAt`
- audit fields via `createdBy` / `updatedBy`

Do not bypass these base classes unless there is a strong reason.

## 5. API Surface

Current controllers:

- `AuthController`
- `UserController`
- `ProjectController`
- `TaskController`

Current endpoint groups:

- `/layered/api/v1/auth`
- `/layered/api/v1/users`
- `/layered/api/v1/projects`
- `/layered/api/v1/projects/{projectId}/tasks`

If you add endpoints:

- keep them under the existing `/layered/api/v1` namespace
- validate input at the DTO layer with Jakarta Validation
- return DTOs, not entities
- let services own business rules and transactional behavior
- rely on `GlobalExceptionHandler` for consistent API errors

## 6. Security Rules

Security is stateless JWT-based.

- Public endpoints:
  - `POST /layered/api/v1/users/register`
  - `POST /layered/api/v1/auth/login`
  - health and prometheus actuator endpoints
- Everything else requires authentication
- Swagger is open only for `local` and `int` profiles

Authorization currently lives in two places:

- controller-level `@PreAuthorize` for project creation / reads
- service-level policy checks in `UserServiceImpl` through `UserPolicy`

When adding protected functionality:

- prefer keeping authorization close to the business rule
- reuse `AuthenticationFacade` and `UserPolicy` style patterns
- do not introduce ad hoc role checks in multiple layers for the same rule

## 7. Persistence and Migrations

Database schema is managed by Flyway under `src/main/resources/db/migration`.

Existing migrations:

- `V1__create_users_table.sql`
- `V2__create_project_table.sql`
- `V3__create_task_table.sql`
- `V4__create_task_attachment_table.sql`
- `V5__cleanup_indexes_and_task_attachment.sql`

Rules:

- never rewrite an existing Flyway migration that may already be applied
- add a new versioned migration for schema changes
- keep entity mappings and migrations synchronized
- preserve soft delete behavior and supporting indexes when changing queries

Note: there is no active `TaskAttachment` entity in the current codebase even though an attachment table existed historically and was later removed by migration `V5`.

## 8. Config Profiles and Local Run

Main profiles in use:

- `local`: MySQL on `localhost:3307`, Redis on `localhost:6379`, Swagger enabled
- `dev`: H2-based local/dev profile, Swagger enabled
- `int`: MySQL via env vars, used in Kubernetes/integration environments
- `test`: used by unit and integration tests

Useful commands:

```bash
./gradlew test
./gradlew :integration-test:test
./gradlew build
./runLocal.sh
./runLocal.sh -o
docker compose -f docker-compose-local.yml up -d
./performance-test/runPerformance.sh
```

`runLocal.sh` can optionally start the observability stack and then boots the app with the `local` profile.

## 9. Testing Expectations

Testing is split by purpose:

- unit tests in `src/test/java`
- shared helpers in `src/testFixtures/java`
- integration tests in `integration-test/src/test/java`
- performance tests in `performance-test/src/gatling/java`

When changing behavior:

- add or update a focused unit test for service/controller logic
- add or update an integration test when auth, persistence, transactions, or HTTP behavior changes
- use existing utilities such as `UserTestUtils` and `JsonUtils`
- keep MockMvc-based API assertions consistent with current style

Integration tests use:

- `AbstractIntegrationTest`
- `AbstractWebIntegrationTest`
- Testcontainers MySQL

Do not replace integration coverage with unit-only coverage when database or security behavior is involved.

## 10. Observability and Operations

The repository already contains hooks for production-style observability:

- actuator health/readiness/liveness
- Prometheus metrics
- OpenTelemetry spans
- correlation/request IDs in filters and interceptors
- structured logging via Logback JSON encoder
- local Grafana/Loki/Tempo/Prometheus stack under `observability/`

When adding cross-cutting behavior:

- preserve request ID / correlation ID propagation
- avoid breaking log fields used by observability tooling
- prefer adding spans around important business operations rather than noisy low-value spans

## 11. Caching and Retry

Existing infrastructure patterns:

- Redis-backed cache when `cache.enabled=true`
- `NoOpCacheManager` fallback when cache is disabled
- retry via `@TransientDbRetry`

Current cached path:

- user lookup by username

If you change user mutation flows, remember cache invalidation. `UserServiceImpl` already evicts username-based cache entries during update, role change, and soft delete.

## 12. Coding Style for This Repo

Follow the style already present in the codebase:

- use Java records for request/response DTOs
- use constructor injection
- avoid Lombok unless the repo adopts it globally later
- keep controllers thin
- keep mapping logic in mapper classes or explicit response construction
- use `@Transactional` at the service layer
- prefer explicit repository methods over hidden magic
- keep comments sparse and useful

Be careful with lazy-loading:

- fetch related entities in the service layer before mapping
- use `EntityGraph`, fetch joins, or service-layer access patterns where needed
- do not move lazy entity access into controllers

## 13. Safe Edit Boundaries

Unless the task explicitly asks for infra changes, treat these as stable boundaries that require extra care:

- `SecurityConfig`
- JWT classes under `security/jwt`
- Flyway migrations
- `GlobalExceptionHandler`
- Kubernetes manifest under `k8s/`
- observability stack files

For routine feature work, the safest path is usually:

1. DTO
2. controller
3. service
4. repository
5. mapper
6. tests

## 14. What To Verify Before Finishing Work

At minimum, choose the relevant subset:

- `./gradlew test`
- `./gradlew :integration-test:test`
- endpoint behavior with MockMvc or integration coverage
- migration compatibility if schema changed
- security access rules for new protected endpoints
- cache invalidation if user data changes

If a change affects auth, persistence, or API contracts, lack of tests should be treated as a gap, not a minor detail.

## 15. Agent Working Heuristics

- Read the existing layer before editing it; similar patterns already exist in this repo.
- Prefer consistency with current code over introducing a new abstraction.
- Preserve soft delete, audit, validation, and JWT rules unless the task explicitly changes them.
- Keep documentation and commands aligned with actual repo files and profiles.
- If a requested change conflicts with current security or migration strategy, call that out explicitly before implementing.
