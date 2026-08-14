# Current Task

> Start a new task by replacing this file. Only one development task may be active at a time.

## Task

`SaaS-M3 Daily Quota Engine`

## Status

`DONE`

## Goal

Atomically enforce daily AI learning request quota for quota-consuming learner
actions, expose the current authenticated user's quota state, and preserve
idempotency so retries do not double-consume quota.

## Related documents

- `docs/design/ENGLISH_TUTOR_AGENT_SAAS_FOUNDATION_DESIGN_v1.1.0.md`
- `docs/plans/SAAS_FOUNDATION_IMPLEMENTATION_PLAN_v1.1.0.md`
- `docs/decisions/ADR-0014-saas-foundation.md`
- `contracts/openapi/english-tutor-api.yaml`
- `server/tutor-bootstrap/src/main/resources/db/migration/V14__saas_daily_quota.sql`

## In Scope

- Add daily quota policy, usage and reservation persistence.
- Add quota date calculation from configured reset timezone.
- Add reserve, commit, refund and stale reservation cleanup.
- Enforce quota for conversation AI replies and open assessment answer scoring.
- Return `DAILY_QUOTA_EXCEEDED` as a 429 ProblemDetail.
- Add `GET /api/v1/me/quota`.
- Add default quota environment configuration.

## Out of Scope

- Admin quota override/reset/bonus APIs.
- Web quota UI.
- Android quota UI.
- Billing, subscription, organization or workspace features.
- Removing the temporary legacy `X-User-Key` compatibility path.

## Acceptance Criteria

1. With remaining quota = 1, 10-20 concurrent requests allow exactly one success.
2. Repeated idempotency key does not double-consume quota.
3. Provider failure before usable output refunds quota.
4. `/api/v1/me/quota` returns daily limit, used, bonus, remaining, unlimited and reset time.
5. Quota exhaustion returns `429 DAILY_QUOTA_EXCEEDED`.
6. Released Flyway V1-V13 are not modified.

## Verification Record

- `.\mvnw.cmd -pl tutor-bootstrap -am test` from `server/` - PASS, with 2 external infrastructure tests skipped because Docker/external services are not available.

## Review Status

Completed for SaaS-M3.
