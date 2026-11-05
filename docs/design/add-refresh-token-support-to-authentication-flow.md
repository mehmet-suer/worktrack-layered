# Add Refresh Token Support to Authentication Flow

Status: FINAL

## Request
Add refresh token support so users can obtain a new access token without logging in again when the current token expires.

Acceptance criteria:
- Login returns both access token and refresh token
- A refresh endpoint is available to issue a new access token
- Logout invalidates the refresh token
- Expired or invalid refresh tokens are rejected
- Relevant tests are added

## Current State
- `AuthController` currently exposes only `POST /layered/api/v1/auth/login` and `GET /layered/api/v1/auth/me`.
- `AuthServiceImpl` validates username/password and returns `LoginResponse(token, type, expiresAt)` built from `JwtService.generateToken(User)`.
- Current authentication resolves users through active-user lookups in `UserService` / `UserRepository`, so soft-deleted users are already excluded from normal login and bearer-token authentication paths.
- `JwtService` and `JwtAuthenticationFilter` handle only signed JWT bearer access tokens passed in the `Authorization` header.
- `SecurityConfig` permits `POST /layered/api/v1/auth/login` publicly, disables Spring Security logout, and requires authentication for everything else under the API.
- `JwtProperties` exposes a single `jwt.expiration` value for access token lifetime; there is no refresh token lifetime configuration.
- The base-entity model now separates `AuditableBaseEntity` from `StatusAwareAuditableBaseEntity`, so new entities can opt into audit fields without inheriting soft-delete status behavior.
- There is no server-side token store, revocation list, or refresh endpoint, so logout invalidation cannot be enforced today.
- Existing integration coverage verifies login and use of `/auth/me`, but there is no refresh/logout coverage.

## Goal
Introduce refresh token support while preserving the existing JWT bearer access-token flow for protected endpoints. Clients should receive an access token plus a refresh token at login, exchange the refresh token for a fresh token pair when needed, and revoke the current refresh token on logout.

## Non-Goals
- Switching the application to cookie-based auth or server-side HTTP sessions
- Replacing JWT access tokens with opaque access tokens
- Adding "logout all devices" or admin session revocation
- Adding scheduled background cleanup for expired refresh tokens in this first change

## Constraints
- Keep the implementation inside the existing layered structure: controller, service, repo, entity, dto, mapper/config/exception as appropriate.
- Keep all new endpoints under `/layered/api/v1/auth` and validate request DTOs with Jakarta Validation.
- Preserve stateless JWT access-token authentication for existing protected endpoints and keep controllers thin.
- This task touches stable boundaries called out in `AGENTS.md`: `SecurityConfig`, JWT classes, Flyway migrations, and `GlobalExceptionHandler`.
- Because logout invalidation introduces server-side state, unit-only coverage is not enough; integration coverage is required.
- New persistence should follow the current base-entity split: `refresh_token` should use `AuditableBaseEntity` for id/version/timestamps/audit fields, but it should not be status-aware or rely on Hibernate soft-delete filters.

## Risks
- A pure self-contained JWT refresh token would not satisfy the logout invalidation requirement. Some server-side state is required.
- The login response contract changes, so current tests and any client code must keep a stable access-token field to avoid unnecessary breakage.
- Refresh tokens are long-lived secrets and must not be stored in plaintext or written to logs.
- `POST /auth/refresh` and `POST /auth/logout` must be publicly reachable because access tokens may already be expired, which increases the importance of strict validation and clear 401 handling.
- Reusing the status-aware base class for `refresh_token` would introduce the wrong persistence semantics after the base-entity refactor and would add unnecessary filter behavior to session-token lookups.
- If two refresh requests race on the same token, a naive read-then-write implementation could issue multiple token pairs from one refresh token. Rotation must therefore be guarded by a single-winner revocation/update step.
- Refresh-token validation cannot trust token persistence alone; it must also verify that the owning user still satisfies the same active/authenticatable rules as the rest of the auth flow.
- Expired refresh-token rows will accumulate over time. Cleanup remains out of scope for this first implementation.

## Proposed Design
- Keep access tokens as signed JWTs and continue using them only through the `Authorization: Bearer ...` header for protected endpoints.
- Add an opaque refresh token flow backed by persistence. The raw refresh token returned to the client will be a cryptographically random string generated with `SecureRandom`.
- Persist only a SHA-256 hash of the refresh token, never the raw token value. Validation will hash the incoming token and compare against stored records.
- Support multiple concurrent refresh tokens per user so separate logins/devices do not invalidate each other implicitly.
- Use refresh-token rotation on `POST /auth/refresh`: once a valid refresh token is exchanged, set `revoked_at` on the current token row and create a replacement refresh token. This reduces replay risk if an old refresh token leaks.
- Make refresh rotation single-use under race: if two refresh attempts hit the same token concurrently, only the first successful request may revoke the old token and issue a new token pair. Any later attempt using that same old token must fail as already revoked/invalid.
- Enforce the single-winner rule with an atomic revocation step, such as a conditional update on the refresh-token row where `revoked_at IS NULL` and `expires_at > now`, wrapped in a transaction. Token rotation must proceed only when that update affects exactly one row.
- On `POST /auth/logout`, revoke only the submitted refresh token/session by setting `revoked_at`. Global logout remains out of scope.
- Refresh-token validation must load the owning user and reject the token when that user is no longer active or otherwise fails the current business rules for an authenticatable user. The implementation should reuse the existing user-auth lookup path or a shared helper so refresh eligibility does not drift from login behavior.
- Reject refresh or logout attempts when the token is missing, malformed, expired, already revoked, unknown, or belongs to a user that is no longer valid for authentication.
- Keep refresh tokens in JSON request bodies, not cookies or custom headers, to stay aligned with the current API style.
- Keep current access-token response fields stable by retaining `token`, `type`, and `expiresAt` for the access token, and add refresh-specific fields rather than renaming the existing access-token fields.
- Add dedicated spans for refresh and logout, and avoid logging raw refresh-token values anywhere in the flow.

## Data Model / Schema Changes
- Add Flyway migration `V6__create_refresh_token_table.sql`.
- Create a new `refresh_token` table with at least:
  - `id`
  - `user_id` foreign key to `users(id)`
  - `token_hash` unique, not null
  - `expires_at` not null
  - `revoked_at` nullable
  - `version`
  - `created_at` / `updated_at`
  - `created_by` / `updated_by`
- Add indexes for:
  - `token_hash` uniqueness
  - active token lookup by `user_id` and `revoked_at`
  - expiry-based cleanup/querying on `expires_at`
- Add a `RefreshToken` entity under `entity.auth` extending `AuditableBaseEntity`. This keeps id/version/timestamps/audit fields but intentionally avoids the status-aware base class.
- Add a `RefreshTokenRepository` under `repo.auth` with explicit lookup and revocation methods based on `token_hash`, `revoked_at`, and `expires_at`.
- Add a repository method that supports atomic revocation for rotation, for example updating `revoked_at` only when the token is still unrevoked and unexpired, so concurrent refresh requests cannot both succeed.

## API Changes
- `POST /layered/api/v1/auth/login`
  - Response expands to include refresh-token data while keeping current access-token fields stable.
- `POST /layered/api/v1/auth/refresh`
  - Request body: `{"refreshToken":"..."}`
  - Response returns a fresh access token and a rotated refresh token.
- `POST /layered/api/v1/auth/logout`
  - Request body: `{"refreshToken":"..."}`
  - Response: `204 No Content`
- Add a dedicated request DTO such as `RefreshTokenRequest` with `@NotBlank refreshToken`.
- Prefer a shared auth-token response DTO shape for login and refresh so token-pair formatting stays consistent.
- Update `SecurityConfig` so `POST /auth/refresh` and `POST /auth/logout` are `permitAll`.
- Add a dedicated 401 error path for refresh token failures with a specific error code such as `INVALID_REFRESH_TOKEN` so API clients can distinguish bad refresh tokens from bad username/password login attempts.

## Implementation Steps
1. Extend auth configuration and DTOs.
   Add refresh-token expiration config (for example `jwt.refresh-expiration`), expand the login response shape, and add request/response DTOs for refresh/logout.
2. Add persistence for refresh tokens.
   Create the Flyway migration, the `RefreshToken` entity extending `AuditableBaseEntity`, and the repository methods needed for lookup, atomic rotation, and `revoked_at`-based revocation.
3. Introduce a refresh-token service.
   Encapsulate random token generation, hashing, persistence, expiry checks, owning-user validation against current authentication rules, single-winner token rotation, and logout revocation in a dedicated service instead of overloading `JwtService`.
4. Update the auth service and controller.
   Have login issue both token types, add `refresh` and `logout` methods/endpoints, and keep business rules inside the service layer.
5. Update security and error handling.
   Permit the new public auth endpoints, add refresh-specific 401 handling, and make sure existing JWT access-token behavior for protected endpoints remains unchanged.
6. Add observability and documentation touchpoints.
   Add refresh/logout span names and update README/config examples to include refresh-token behavior and the new expiration property.
7. Add tests.
   Cover service logic, controller validation, and end-to-end auth flow behavior including token rotation and revocation.

## Test Plan
- unit:
  - `AuthServiceImpl` returns both access and refresh tokens on successful login
  - refresh token service hashes tokens before persistence
  - refresh with a valid token rotates the refresh token and returns a new access token
  - concurrent or repeated refresh attempts on the same old token produce only one successful rotation
  - revoked, expired, malformed, or unknown refresh tokens are rejected
  - refresh is rejected when the owning user is soft-deleted or otherwise no longer valid for authentication under current business rules
- integration:
  - login response contains both token types
  - `POST /auth/refresh` works without an access token and returns a fresh token pair
  - the previously used refresh token cannot be reused after rotation
  - if the same refresh token is submitted again after a successful refresh, the later attempt returns `401`
  - `POST /auth/logout` invalidates the submitted refresh token
  - invalid or expired refresh tokens return `401`
  - existing protected endpoints still require a valid access token
- edge cases:
  - blank/missing refresh token request body yields `400`
  - logout after refresh rotation only revokes the current token
  - refresh token tied to a user that no longer passes active/authenticatable checks is rejected
  - existing performance test flow keeps working if the access token remains at `$.token`

## Rollback / Failure Notes
- If the new flow causes issues, the safe operational rollback is a forward fix that stops clients from using refresh endpoints while preserving normal login.
- The Flyway migration should not be rewritten after application, so rollback should assume the `refresh_token` table remains in place even if the feature is temporarily disabled in code.

## Open Questions
None at this time.

Resolved decisions:
- Refresh tokens rotate on every successful refresh. The previously used refresh token is revoked immediately, and a newly issued refresh token is returned together with the new access token.
- Logout revokes only the submitted refresh token. Revoking all active refresh tokens for the user is a separate concern and should be handled by a dedicated logout-all operation if needed later.
- Refresh tokens are sent in JSON request bodies to stay aligned with the current API style.
