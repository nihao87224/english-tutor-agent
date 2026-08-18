# ADR-0016: Database is the single runtime source for AI provider configuration

Status: Accepted

## Context

Provider endpoints, models and API keys could previously be supplied by both
environment variables and `ai_provider_config` / `ai_provider_secret`. This
made the active configuration ambiguous and prevented administrators from
reliably validating a change at runtime.

## Decision

All runtime AI provider configuration is resolved from the database through
`AiProviderConfigurationApplicationService`. This includes provider type,
endpoint, models, defaults, timeout and API key. API keys are AES-GCM encrypted
in `ai_provider_secret` and only a masked hint is exposed.

Environment variables are limited to infrastructure and system secrets,
including `TUTOR_SECRET_ENCRYPTION_KEY`, which decrypts the database secret.
The application starts when no provider is ready, logs an actionable warning and
returns a configuration error only when an AI feature or connection test needs
that provider. Administrators can use the connection-test endpoint to validate
a saved provider explicitly.

## Consequences

- Provider changes, including default switches, take effect on the next request
  without restart.
- `DEEPSEEK_*`, `OPENAI_*` and generic LLM environment configuration are not
  supported as runtime fallbacks.
- This supersedes the configuration-source portions of ADR-0013 and ADR-0015.
