# Current Task

> Start a new task by replacing this file. Only one development task may be active at a time.

## Task

`SaaS-M6 Web Learner SaaS UX + i18n`

## Status

`DONE`

## Goal

Move the Web learner experience from local development identity to authenticated
SaaS account UX with quota visibility, route guarding and bilingual UI support.

## Related documents

- `docs/design/ENGLISH_TUTOR_AGENT_SAAS_FOUNDATION_DESIGN_v1.1.0.md`
- `docs/ui/ENGLISH_TUTOR_AGENT_SAAS_UI_PROTOTYPE_v1.1.0.html`
- `docs/plans/SAAS_FOUNDATION_IMPLEMENTATION_PLAN_v1.1.0.md`
- `docs/decisions/ADR-0014-saas-foundation.md`
- `contracts/openapi/english-tutor-api.yaml`
- `web/README.md`

## In Scope

- Auth store and learner route guard.
- Login, register, logout and refresh-token flow.
- Bearer authenticated Web API client.
- Removal of Web local user key generation from the learner app path.
- Today and account quota display using `/api/v1/me/quota`.
- Quota-exceeded UX for conversation requests.
- Account page and zh-CN/en i18n foundation.
- Email-scoped recent practice history for completed summaries.
- Learner Playwright coverage for the SaaS flow.

## Out of Scope

- Web admin console.
- Android auth/quota/i18n migration.
- Billing, subscription, organization or workspace features.
- Removing the backend temporary legacy `X-User-Key` compatibility path.
- Changing backend deployment or Web Jenkins deployment logic.

## Acceptance Criteria

1. Register -> onboarding -> practice -> quota consumed -> summary works in Web.
2. Logout/login with the same email returns to the same learner account flow.
3. Web learner API calls use Authorization Bearer tokens in the normal app path.
4. Daily quota is visible on Today and Account screens.
5. Quota exceeded errors show a learner-friendly UX.
6. zh-CN/en language switching is available.
7. Recent practice history remains visible after logout/login on the same email.

## Verification Record

- `pnpm test` from `web/` - PASS.
- `pnpm run build` from `web/` - PASS.
- `pnpm run e2e` from `web/` - PASS.

## Review Status

Completed for SaaS-M6. The next milestone is `SaaS-M7 Web Admin Console`.
