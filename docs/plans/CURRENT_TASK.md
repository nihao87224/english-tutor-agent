# Current Task

## Task

`SaaS-M10 Multi-provider LLM Protocols`

## Status

`DONE`

## Goal

Make DeepSeek the default LLM while preserving OpenAI and Gemini protocol
compatibility behind the project-owned provider contracts.

## In Scope

- DeepSeek OpenAI Chat Completions-compatible LLM adapter.
- OpenAI Responses and Gemini generateContent LLM adapters.
- Runtime provider-type routing, provider configuration validation and Flyway
  migration.
- Admin/API contract, Web Admin and deployment configuration updates.

## Out of Scope

- Automatic provider failover.
- Gemini or DeepSeek ASR/TTS adapters.
- Changes to learning-domain business rules.

## Acceptance Criteria

1. DeepSeek is the default LLM configuration.
2. OpenAI, OpenAI-compatible and Gemini LLM protocols are selectable at runtime.
3. Unsupported ASR/TTS provider combinations are rejected.
4. Provider secrets remain encrypted/masked and tests use local HTTP stubs.

## Verification Record

- `server> .\mvnw.cmd -pl tutor-bootstrap -am test` - PASS.
- `web> pnpm test` - PASS.
- `web> pnpm build` - PASS.

## Review Status

Completed for SaaS-M10. The default LLM is DeepSeek, and OpenAI/Gemini protocol
selection is available at runtime without changing learning-domain code.
