# Current Task

> Start a new task by replacing this file. Only one development task may be active at a time.

## Task

`SaaS-M1 Identity Schema + Auth Backend`

## Status

`DONE`

## Goal

Establish real SaaS account identity with email/password registration, login,
JWT access tokens, opaque refresh token sessions, `/api/v1/me`, RBAC seed data
and one-time bootstrap admin support.

## Related documents

- `docs/design/ENGLISH_TUTOR_AGENT_SAAS_FOUNDATION_DESIGN_v1.1.0.md`
- `docs/plans/SAAS_FOUNDATION_IMPLEMENTATION_PLAN_v1.1.0.md`
- `docs/decisions/ADR-0014-saas-foundation.md`
- `server/tutor-bootstrap/src/main/resources/db/migration/V11__saas_account_identity.sql`
- `server/tutor-bootstrap/src/main/resources/db/migration/V12__saas_rbac.sql`
- `server/tutor-bootstrap/src/main/resources/db/migration/V13__saas_auth_session.sql`

## In Scope

- Add auth account fields to existing `app_user`.
- Add USER/ADMIN roles and ADMIN permissions.
- Add refresh token session persistence with hashed tokens.
- Add register, login, refresh, logout and `/me` APIs.
- Add bootstrap admin environment configuration.
- Add Spring Security bearer JWT support for `/api/v1/me`.
- Keep existing learner `X-User-Key` APIs temporarily available for SaaS-M2 migration.

## Out of Scope

- CurrentActor migration for all learner APIs.
- Daily quota engine.
- Admin management APIs and Web admin console.
- Runtime provider database configuration.
- Android auth.
- Billing, subscription, organization or workspace features.

## Acceptance Criteria

1. Normalized email is unique.
2. USER registration and login succeed.
3. ADMIN login succeeds through bootstrap admin.
4. Invalid credentials do not reveal whether the account exists.
5. Refresh token rotation works and rejects the old token.
6. Logout revokes the current refresh token.
7. `/api/v1/me` requires a valid bearer access token.
8. Released Flyway V1-V10 are not modified.

## Verification Record

- `.\mvnw.cmd -pl tutor-bootstrap -am test` from `server/` - PASS, with 2 external infrastructure tests skipped because Docker/external services are not available.

## Review Status

Completed for SaaS-M1.
