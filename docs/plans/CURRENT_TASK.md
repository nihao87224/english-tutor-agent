# Current Task

## Task

`AI Provider Configuration Refactor`

## Status

`DONE`

## Goal

Make MySQL the single runtime source for AI provider endpoints, models,
defaults and encrypted API keys, while preserving Hexagonal Architecture and
allowing the application to start before a provider is configured.

## Implemented

- Removed all `DEEPSEEK_*`, `OPENAI_*` and generic LLM environment runtime
  fallbacks.
- Added V18 to clear historical defaults that have no encrypted database API
  key. Existing Flyway V1–V17 remain unchanged.
- Added a non-fatal startup warning and first-admin configuration guidance.
- Added `POST /api/v1/admin/ai-providers/{providerCode}/test`, its OpenAPI
  contract and the Admin Console connection-test action.
- Added AES-GCM database-only resolution and application/agent outbound port
  coverage without real paid API calls.

## Verification Record

- `server> .\mvnw.cmd -q -pl tutor-bootstrap -am verify` — PASS.
- `web> pnpm test -- --run` — PASS (44 tests).
- `web> pnpm run build` — PASS.
- `python scripts\validate_project.py` — PASS.

## Decision Note

The supplied plan called for `V15__ai_provider_configuration_refactor.sql`,
but V15, V16 and V17 already exist on this branch. The refactor therefore uses
the only safe forward Flyway version: V18.

## Follow-up Task

`User Learning Progress Flow Fix` — `DONE`

The learner entry now uses `GET /api/v1/users/me/progress`, which derives the
next action from server-owned onboarding state. The Web flow completes profile
setup, self-assessment and the resumable initial assessment before exposing
today's plan. Verification: Maven `verify`, Web tests (48), and Web build all
passed.
