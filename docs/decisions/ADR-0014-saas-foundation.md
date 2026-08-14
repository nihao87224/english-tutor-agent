# ADR-0014: SaaS Foundation is the next product platform increment

Status: Accepted for implementation planning

## Context

The v1.0 baseline proves the learner loop: onboarding, today plan, text practice,
streamed coaching, layered correction, learning evidence and daily summary.
The v1.1.0 SaaS Foundation design adds the platform capabilities needed before
public multi-user operation:

- trusted email/password identity;
- server-side CurrentActor and cross-user data isolation;
- USER and ADMIN roles with authority-based admin access;
- daily business-request quota;
- admin operations for users, quota, settings, provider configuration and audit;
- runtime AI provider configuration with encrypted secrets;
- Web learner/admin UX and later Android learner auth/quota support.

## Decision

Implement SaaS Foundation as an incremental extension of the current modular
monolith. The existing English learning domain, agent abstractions, Flyway
migrations V1-V10, and `app_user.id` internal user key remain the baseline.

Implementation must proceed milestone-by-milestone from SaaS-M1 through SaaS-M9.
Do not rewrite the learning core or introduce billing, organizations, workspaces,
team tenancy, subscriptions, coupons, invoices, or payment providers in this
foundation phase.

## Consequences

- `X-User-Key` becomes a temporary migration compatibility mechanism only.
- Normal learner APIs must resolve identity from backend authentication context.
- Admin access must use `/api/v1/admin/**` plus authorities, not caller-supplied
  target user identity on learner endpoints.
- Quota is counted by successful AI learning business requests, not raw model
  tokens. Provider token/cost telemetry remains internal observability.
- Runtime provider secrets must be encrypted at rest and masked in API responses
  and logs.
- Tests use local stubs or mocked transport. Production fake AI provider runtime
  must not be reintroduced; where the v1.1.0 source design says "Fake provider
  compatibility", implement that as deterministic test doubles only.

## References

- `docs/design/ENGLISH_TUTOR_AGENT_SAAS_FOUNDATION_DESIGN_v1.1.0.md`
- `docs/ui/ENGLISH_TUTOR_AGENT_SAAS_UI_PROTOTYPE_v1.1.0.html`
- `docs/plans/SAAS_FOUNDATION_IMPLEMENTATION_PLAN_v1.1.0.md`
- ADR-0013: Production AI uses OpenAI-backed real providers
