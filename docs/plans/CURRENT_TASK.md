# Current Task

> Start a new task by replacing this file. Only one development task may be active at a time.

## Task

`SaaS-M4 Runtime AI Provider + Secret`

## Status

`DONE`

## Goal

Allow runtime AI provider configuration changes without redeploying, store
provider secrets encrypted, and keep the production provider backed by real
OpenAI adapters only.

## Related documents

- `docs/design/ENGLISH_TUTOR_AGENT_SAAS_FOUNDATION_DESIGN_v1.1.0.md`
- `docs/plans/SAAS_FOUNDATION_IMPLEMENTATION_PLAN_v1.1.0.md`
- `docs/decisions/ADR-0013-openai-real-provider.md`
- `docs/decisions/ADR-0014-saas-foundation.md`
- `contracts/openapi/english-tutor-api.yaml`
- `server/tutor-bootstrap/src/main/resources/db/migration/V15__ai_provider_config.sql`

## In Scope

- Add provider config and encrypted provider secret persistence.
- Add AES-GCM secret encryption using `TUTOR_SECRET_ENCRYPTION_KEY`.
- Resolve default LLM/ASR/TTS OpenAI configuration at request time.
- Add admin provider list/detail/update and secret replacement endpoints.
- Add masked secret responses and admin audit records for provider changes.
- Keep tests deterministic with local stubs/test doubles only.

## Out of Scope

- Full admin dashboard, user management and audit query APIs.
- Web admin console.
- Android admin/configuration UI.
- Billing, subscription, organization or workspace features.
- Removing the temporary legacy `X-User-Key` compatibility path.

## Acceptance Criteria

1. Changing default provider/model configuration affects the next request without restart.
2. Full API secrets never appear in API responses.
3. Provider API keys are encrypted at rest with AES-GCM.
4. Provider secret replacement writes an admin audit record.
5. Automated tests do not call real paid model APIs.
6. Released Flyway V1-V14 are not modified.

## Verification Record

- `.\mvnw.cmd -pl tutor-bootstrap -am test` from `server/` - PASS, with 2 external infrastructure tests skipped because Docker/external services are not available.

## Review Status

Completed for SaaS-M4.
