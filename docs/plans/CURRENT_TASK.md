# Current Task

> Start a new task by replacing this file. Only one development task may be active at a time.

## Task

`SaaS-M5 Admin Backend`

## Status

`DONE`

## Goal

Expose authenticated, authority-gated SaaS administration APIs for dashboard
metrics, user management, quota operations, system settings and audit review.

## Related documents

- `docs/design/ENGLISH_TUTOR_AGENT_SAAS_FOUNDATION_DESIGN_v1.1.0.md`
- `docs/plans/SAAS_FOUNDATION_IMPLEMENTATION_PLAN_v1.1.0.md`
- `docs/decisions/ADR-0014-saas-foundation.md`
- `contracts/openapi/english-tutor-api.yaml`
- `server/tutor-bootstrap/src/main/resources/db/migration/V16__admin_backend.sql`

## In Scope

- Admin dashboard summary API.
- Admin user search/detail/status/role APIs.
- Admin quota policy, daily reset and bonus APIs.
- Runtime system settings API.
- Admin audit query API.
- Authority checks for `/api/v1/admin/**` and provider-admin APIs.
- Audit records for sensitive admin operations.

## Out of Scope

- Web admin console UI.
- Learner Web auth/i18n migration.
- Android auth/quota/i18n migration.
- Billing, subscription, organization or workspace features.
- Removing the temporary legacy `X-User-Key` compatibility path.

## Acceptance Criteria

1. USER receives 403 on admin APIs.
2. ADMIN can access dashboard and admin operations with the required authorities.
3. Sensitive admin operations produce audit records.
4. System settings are created through a forward-only Flyway migration.
5. Existing backend deployment and learner business flows are not changed.

## Verification Record

- `.\mvnw.cmd -pl tutor-bootstrap -am -Dtest=AdminEndpointIntegrationTest '-Dsurefire.failIfNoSpecifiedTests=false' test` from `server/` - PASS.
- `.\mvnw.cmd -pl tutor-bootstrap -am test` from `server/` - PASS, with 2 external infrastructure tests skipped because Docker/external services are not available.

## Review Status

Completed for SaaS-M5. The next milestone is `SaaS-M6 Web Learner SaaS UX + i18n`.
