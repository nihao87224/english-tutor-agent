# ADR-0015: Multi-provider LLM protocols with DeepSeek as the default

Status: Accepted

## Context

The production LLM is now DeepSeek. The existing runtime only supported the
OpenAI Responses API, which made it impossible to select DeepSeek or Gemini
without changing application code.

## Decision

Business and application code continue to use the project-owned `ChatProvider`
contract. Runtime selection is based on the configured LLM provider type:

- `OPENAI` uses the OpenAI Responses API.
- `OPENAI_COMPATIBLE` uses the OpenAI Chat Completions protocol; DeepSeek is
  the default configuration and uses this adapter.
- `GEMINI` uses Gemini's native `generateContent` REST API and `x-goog-api-key`
  authentication.

DeepSeek is seeded as the default LLM provider. OpenAI remains available for
LLM use and is the only current provider type for ASR and TTS. Gemini and
OpenAI-compatible providers cannot be selected as default ASR or TTS providers.

API keys remain encrypted at rest when stored through the admin API, masked in
responses, and sourced from environment variables only for bootstrap defaults.

## Consequences

- Switching the default LLM provider in the admin API takes effect on the next
  request without a redeploy.
- Provider protocol differences remain outside domain and application modules.
- `OPENAI_API_KEY` is no longer the default LLM bootstrap key. Production must
  provide `DEEPSEEK_API_KEY` and `DEEPSEEK_LLM_MODEL`.
- This supersedes ADR-0013 only for the default LLM provider; its OpenAI ASR and
  TTS decision remains in effect.
