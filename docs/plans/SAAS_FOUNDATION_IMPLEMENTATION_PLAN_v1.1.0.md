# SaaS Foundation Implementation Plan v1.1.0

Status: Ready for milestone implementation

Source documents:

- `docs/design/ENGLISH_TUTOR_AGENT_SAAS_FOUNDATION_DESIGN_v1.1.0.md`
- `docs/ui/ENGLISH_TUTOR_AGENT_SAAS_UI_PROTOTYPE_v1.1.0.html`
- ADR-0014

## Review Summary

The v1.1.0 design is an incremental SaaS platform layer on top of the current
v1.0 learner loop. It does not replace the existing English learning core.

Accepted direction:

- keep the modular monolith and current module boundaries;
- keep `app_user.id` as the internal user primary key;
- add email/password identity, refresh sessions, JWT access tokens and bootstrap
  admin;
- replace client-generated identity with backend-resolved CurrentActor;
- enforce cross-user isolation in application/repository queries;
- add daily quota by AI learning business request count;
- add admin APIs and Web admin console for user, quota, provider, settings and
  audit operations;
- add runtime provider configuration with encrypted secrets;
- keep billing, subscriptions, organizations and enterprise tenant isolation out
  of this phase.

Important review notes:

- The design file header said `V1.0` while the filename is v1.1.0; repository
  indexes treat it as the v1.1.0 SaaS Foundation source.
- The UI prototype is a visual and interaction reference, not production data or
  a requirement to preserve its exact CSS.
- The design mentions "Fake provider compatibility" in SaaS-M4. That conflicts
  with ADR-0013 and the current instruction to remove production fake providers.
  Treat this as test double compatibility only.
- Existing uncommitted OpenAI provider changes are prerequisite-compatible with
  SaaS-M4, but SaaS-M4 must move secrets/configuration into encrypted runtime
  storage instead of hardcoding model names or keys.

## Non-Negotiable Rules

- Do not modify released Flyway V1-V10 migrations.
- Do not trust `userId`, `userKey` or `X-User-Key` from normal clients after
  CurrentActor is available.
- Do not allow `/api/v1/admin/**` through legacy identity compatibility.
- Do not return full API secrets after save.
- Do not store raw refresh tokens; store only token hashes.
- Do not call paid model APIs from automated tests.
- Do not implement payment, subscription, team, workspace or organization scope
  in the foundation milestones.

## Milestone Order

### SaaS-M1: Identity Schema + Auth Backend

Goal: establish real account identity and authenticated sessions.

Main work:

- Flyway V11 account auth fields on `app_user`;
- RBAC schema and seed roles/permissions for USER and ADMIN;
- password hashing and email normalization;
- register, login, refresh rotation, logout, `/api/v1/me`;
- one-time bootstrap admin via environment variables;
- auth problem mapping and security tests.

Gate:

- duplicate normalized email returns conflict;
- USER login succeeds;
- ADMIN login succeeds through bootstrap;
- invalid credentials do not reveal account existence;
- refresh token rotation and logout revocation pass tests.

### SaaS-M2: CurrentActor + Multi-User Isolation

Goal: remove client-supplied identity from learner APIs.

Main work:

- application-level CurrentActor abstraction;
- Spring Security adapter in infrastructure/bootstrap;
- learner controllers stop accepting trusted user identity headers;
- temporary legacy `X-User-Key` resolver behind
  `TUTOR_AUTH_LEGACY_USER_KEY_ENABLED`;
- repository/application ownership checks for profile, plan, training,
  conversation and assessment resources.

Gate:

- User A cannot read or mutate User B learning resources;
- anonymous learner API requests return 401;
- `/api/v1/admin/**` never accepts legacy identity.

### SaaS-M3: Daily Quota Engine

Goal: atomically enforce daily AI learning request quota.

Main work:

- Flyway quota tables;
- effective policy calculation, user override, bonus and unlimited support;
- `quota_date` based on configured reset timezone;
- reserve, commit, refund and stale reservation handling;
- idempotency integration for quota-consuming mutations;
- `/api/v1/me/quota` and quota error contract.

Gate:

- with remaining quota = 1, 10-20 concurrent requests allow exactly one success;
- repeated idempotency key does not double-consume;
- provider failure before usable output refunds quota.

### SaaS-M4: Runtime AI Provider + Secret

Goal: let admins change provider configuration without redeploying.

Main work:

- Flyway provider config and secret tables;
- AES-GCM secret encryption using `TUTOR_SECRET_ENCRYPTION_KEY`;
- provider config repository and cache;
- provider registry resolves default LLM/ASR/TTS at request time;
- secret replace, masking and audit events;
- OpenAI-backed adapter remains the real production provider.

Gate:

- changing the default provider affects the next request without restart;
- full API secrets never appear in API responses or logs;
- deterministic tests use local stubs/mocked transport, not production fake
  providers.

### SaaS-M5: Admin Backend

Goal: expose operations APIs for SaaS administration.

Main work:

- dashboard summary;
- user search/detail/status/role APIs;
- quota override/reset/bonus APIs;
- system settings APIs;
- audit log query APIs;
- authority-based permission checks.

Gate:

- USER receives 403 on admin APIs;
- all sensitive ADMIN operations produce audit records.

### SaaS-M6: Web Learner SaaS UX + i18n

Goal: move Web learner from local user key to authenticated SaaS UX.

Main work:

- AuthStore and route guards;
- login/register/logout and refresh flow;
- bearer auth API client;
- remove local userKey generation;
- quota display and quota exceeded UX;
- account page and zh-CN/en i18n foundation;
- learner Playwright coverage.

Gate:

- register -> login -> onboarding -> practice -> quota consumed -> history ->
  logout/login -> data preserved.

### SaaS-M7: Web Admin Console

Goal: implement the admin console from the v1.1.0 UI prototype.

Main work:

- admin route shell;
- overview dashboard;
- user management and quota drawer/modal;
- provider configuration;
- system settings;
- audit log;
- bilingual and responsive states;
- admin E2E tests.

Gate:

- UI is wired to real APIs and does not use production mock data.

### SaaS-M8: Android Learner Auth + Quota + i18n

Goal: align Android learner identity and quota with Web.

Main work:

- login/register/logout;
- auth repository and secure refresh token storage;
- authenticated API client and 401 refresh;
- account/quota screens;
- quota error UX;
- zh/en resources and tests.

Gate:

- Web and Android logged in with the same email see the same user, quota and
  learning data.

### SaaS-M9: Hardening + Legacy Cleanup

Goal: make the SaaS foundation production-ready.

Main work:

- disable and then remove legacy `X-User-Key` path;
- existing user claim/migration strategy;
- security review and secret scan;
- backup/rollback runbook;
- quota reconciliation tests;
- provider failover tests;
- load/concurrency smoke;
- deployment docs and README updates.

Gate:

- no legacy identity path for production;
- no plaintext secrets;
- no cross-user access;
- quota concurrency, admin audit, Web smoke and Android smoke pass.

## Current Implementation Progress

SaaS-M1 through SaaS-M4 have been implemented:

- `SaaS-M1: Identity Schema + Auth Backend`
- `SaaS-M2: CurrentActor + Multi-User Isolation`
- `SaaS-M3: Daily Quota Engine`
- `SaaS-M4: Runtime AI Provider + Secret`

The next backend milestone is `SaaS-M5: Admin Backend`.
