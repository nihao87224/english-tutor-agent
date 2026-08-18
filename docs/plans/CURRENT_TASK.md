# Current Task

> Start a new task by replacing this file. Only one development task may be
> active at a time.

## Task

`SaaS-M9 Hardening + Legacy Cleanup`

## Status

`DONE`

## Goal

Make the SaaS Foundation production-pilot ready by removing legacy identity,
documenting claim/rollback operations, and verifying security, quota and
provider-failure guardrails.

## Related documents

- `docs/design/ENGLISH_TUTOR_AGENT_SAAS_FOUNDATION_DESIGN_v1.1.0.md`
- `docs/plans/SAAS_FOUNDATION_IMPLEMENTATION_PLAN_v1.1.0.md`
- `docs/decisions/ADR-0014-saas-foundation.md`
- `docs/process/DEFINITION_OF_DONE.md`
- `docs/deploy/PRODUCTION_DEPLOYMENT.md`
- `docs/deploy/SAAS_HARDENING_RUNBOOK.md`
- `contracts/openapi/english-tutor-api.yaml`

## In Scope

- Remove backend `X-User-Key` resolver and controller compatibility paths.
- Remove Web API client `X-User-Key` fallback.
- Remove production/test/deploy legacy identity switches.
- Update OpenAPI so learner APIs require authenticated bearer sessions only.
- Migrate integration tests to register users and call learner APIs with bearer
  tokens.
- Keep negative tests proving legacy user headers are rejected without bearer
  auth and ignored when bearer auth is present.
- Document existing-user claim strategy, backup/rollback and secret scan steps.
- Update README, deployment docs and SaaS planning docs.

## Out of Scope

- Billing, subscriptions, organizations and workspaces.
- Automatic multi-provider failover routing beyond the current provider failure
  refund guardrail.
- Database schema changes; no new Flyway migration is needed.

## Acceptance Criteria

1. No production code path trusts `X-User-Key`.
2. OpenAPI no longer exposes `X-User-Key` as a learner API parameter.
3. Web client does not send `X-User-Key` fallback headers.
4. Existing integration tests use bearer tokens for learner identity.
5. Anonymous or legacy-header-only learner requests return 401.
6. Cross-user access tests still pass.
7. Quota concurrency and provider-failure refund tests pass.
8. Secret scan and deployment hardening documentation are updated.

## Verification Record

- `.\mvnw.cmd -pl tutor-bootstrap -am test` from `server/` - PASS.

## Review Status

Completed for SaaS-M9. SaaS Foundation v1.1.0 is ready for a limited production
pilot review.
