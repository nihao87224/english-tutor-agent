# Current Task

> Start a new task by replacing this file. Only one development task may be active at a time.

## Task

`SaaS-M2 CurrentActor + Multi-user Isolation`

## Status

`DONE`

## Goal

Move learner API identity resolution from trusted client-supplied `X-User-Key`
values to backend-resolved authenticated users, while keeping a temporary
legacy compatibility switch for existing tests and controlled migration.

## Related documents

- `docs/design/ENGLISH_TUTOR_AGENT_SAAS_FOUNDATION_DESIGN_v1.1.0.md`
- `docs/plans/SAAS_FOUNDATION_IMPLEMENTATION_PLAN_v1.1.0.md`
- `docs/decisions/ADR-0014-saas-foundation.md`
- `server/tutor-api/src/main/java/cn/forever24/tutor/api/auth/CurrentUserKeyResolver.java`

## In Scope

- Resolve learner `userKey` from Spring Security `Authentication` when a bearer
  access token is present.
- Prefer authenticated identity over any `X-User-Key` header.
- Add `TUTOR_AUTH_LEGACY_USER_KEY_ENABLED` with production default `false`.
- Keep legacy `X-User-Key` behavior only when explicitly enabled.
- Cover anonymous rejection and spoofed-header isolation with integration tests.

## Out of Scope

- Removing the temporary `X-User-Key` request parameters from all controllers.
- Daily quota engine.
- Admin management APIs and Web admin console.
- Runtime provider database configuration.
- Android auth.
- Billing, subscription, organization or workspace features.

## Acceptance Criteria

1. Anonymous learner API requests return 401 when legacy mode is disabled.
2. Authenticated learner APIs use the token owner's `userKey`.
3. A spoofed `X-User-Key` does not override authenticated identity.
4. Existing legacy test paths only work when `TUTOR_AUTH_LEGACY_USER_KEY_ENABLED=true`.
5. `/api/v1/admin/**` remains outside legacy identity compatibility.
6. Released Flyway V1-V13 are not modified.

## Verification Record

- `.\mvnw.cmd -pl tutor-bootstrap -am test` from `server/` - PASS, with 2 external infrastructure tests skipped because Docker/external services are not available.

## Review Status

Completed for SaaS-M2.
