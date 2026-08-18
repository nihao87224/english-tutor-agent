# AI Provider Configuration Refactor Plan

## Execution Record

Status: DONE

The existing branch already contained V15–V17, so the required forward-only
migration was implemented as `V18__ai_provider_configuration_refactor.sql`;
V1–V14 and all existing migrations were left unchanged. The implementation
removed environment runtime fallbacks, added non-fatal startup guidance,
database-only runtime resolution, a secured provider connection-test API,
Admin Console support, ADR-0016 and automated coverage. Verification passed
with Maven `verify`, Web tests/build and `scripts/validate_project.py`.

## 1. Background

The system already supports Runtime AI Provider Configuration:

- AI Provider database configuration
- Encrypted API Key storage
- Admin management API
- Permission controlled updates
- Runtime provider resolution

However, AI provider configuration currently has two possible sources:

1. Environment variables
2. Database runtime configuration

This creates ambiguity and operational complexity.

The goal of this refactor is:

> Make database the single source of truth for all AI Provider runtime configuration.

---

# 2. Architecture Decision

## 2.1 Environment Variables Responsibility

Environment variables should only contain system-level secrets and infrastructure configuration.

Allowed:

```env
DATABASE_PASSWORD=
JWT_SIGNING_SECRET=
TUTOR_SECRET_ENCRYPTION_KEY=
```

Not allowed:

```env
OPENAI_API_KEY=
DEEPSEEK_API_KEY=
LLM_MODEL=
LLM_BASE_URL=
```

---

## 2.2 Database Responsibility

All AI provider configuration must be stored in database.

## ai_provider_config

Stores:

- provider_code
- provider_type
- display_name
- enabled
- default_llm
- default_asr
- default_tts
- base_url
- llm_model
- asr_model
- tts_model
- tts_voice
- timeout_seconds
- timestamps

## ai_provider_secret

Stores:

- provider_code
- encrypted_api_key
- encryption_version
- timestamps

Requirements:

- AES-GCM encryption
- Never return raw API key
- Return masked hint only

Example:

```json
{
  "apiKeyConfigured": true,
  "apiKeyMaskedHint": "sk-****abcd"
}
```

---

# 3. Remove Environment Based AI Configuration

Remove:

```env
DEEPSEEK_BASE_URL=
DEEPSEEK_API_KEY=
DEEPSEEK_LLM_MODEL=
```

Remove:

```env
OPENAI_API_KEY=
OPENAI_MODEL=
```

Application code should not directly read model configuration from environment variables.

All runtime configuration must go through:

```
AiProviderConfigurationService
```

---

# 4. Bootstrap Strategy

The application should not fail when no AI Provider is configured.

On startup:

```
Application Started

Database: READY
Redis: READY

AI Provider:
WARNING - NOT CONFIGURED

Please configure AI Provider from Admin Console.
```

The system should continue running.

Only administrator access and AI features should indicate missing configuration.

---

# 5. First Admin Experience

When administrator enters the system for the first time:

Show:

```
System Initialization Required

AI Provider is not configured.

Please configure your first LLM provider.
```

Navigate to:

```
Admin -> AI Provider Management
```

---

# 6. Provider Connection Test

Add API:

```
POST /api/v1/admin/ai-providers/{providerCode}/test
```

Purpose:

Allow administrators to verify:

- API Key validity
- Base URL availability
- Model availability
- Network connectivity

Success example:

```json
{
  "success": true,
  "latencyMs": 500
}
```

Failure example:

```json
{
  "success": false,
  "error": "INVALID_API_KEY"
}
```

---

# 7. Runtime Resolution

All AI requests must follow:

```
User Request

    ↓

AiProviderResolver

    ↓

Database Configuration

    ↓

Active Provider

    ↓

ChatProvider

    ↓

External LLM API
```

No runtime dependency on environment model configuration.

---

# 8. Default Provider Strategy

Multiple providers are supported:

Example:

```
DeepSeek
OpenAI
Gemini
```

Only one provider can be default LLM.

When changing default:

- Automatically unset previous default
- Persist new default
- Apply immediately

No application restart required.

---

# 9. Migration Plan

## Step 1

Create new migration:

```
V15__ai_provider_configuration_refactor.sql
```

Do not modify existing migrations.

---

## Step 2

Remove:

- DeepSeekProperties
- OpenAIProperties
- Environment based model configuration

---

## Step 3

Add:

```
AiProviderStartupCheck
```

---

## Step 4

Add Provider Connection Test API.

---

## Step 5

Update Admin UI:

Features:

- Provider list
- Enable/disable provider
- Change model
- Update API Key
- Test connection
- View masked secret status

---

# 10. Acceptance Criteria

## Configuration

- [ ] Database is the only source of AI Provider configuration
- [ ] No AI API key stored in environment variables

## Security

- [ ] API keys encrypted with AES-GCM
- [ ] Raw secrets never returned
- [ ] Permission controlled by administrator roles

## Runtime

- [ ] Provider changes take effect without restart
- [ ] Default provider switching works immediately

## Operations

- [ ] Application starts without AI configuration
- [ ] Administrator receives configuration guidance

## Testing

- [ ] No real paid API calls in automated tests
- [ ] Use mock providers

---

# Codex Execution Rules

1. Keep existing Hexagonal Architecture.
2. Do not modify Flyway V1-V14.
3. New database migrations start from V15.
4. Controller must not access repository directly.
5. Secret operations must use encryption service.
6. Add automated tests.
7. Keep backward compatibility.

---

# Final Architecture

```
Admin UI

    ↓

Admin API

    ↓

AiProviderConfigurationService

    ↓

+-----------------------+
|                       |
v                       v

ai_provider_config   ai_provider_secret

                         |
                         |
                    AES-GCM decrypt

                         |
                         v

                    ChatProvider

                         |
                         v

              DeepSeek / OpenAI / Gemini
```
