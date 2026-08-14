# ADR-0013: OpenAI real provider for production AI

Status: Accepted

## Context

The production deployment now needs real LLM, ASR and TTS behavior. The earlier
fake-provider-first baseline was useful for deterministic milestone delivery,
but it is no longer acceptable for production runtime.

## Decision

The backend uses the project-owned Provider interfaces with OpenAI-backed
implementations by default:

- `ChatProvider` uses the OpenAI Responses API.
- `SpeechToTextProvider` uses OpenAI audio transcriptions.
- `TextToSpeechProvider` uses OpenAI audio speech generation.
- `OpenAnswerEvaluator` uses `ChatProvider.completeStructured` and validates the
  provider output before mapping it into domain feedback.

Secrets and runtime model choices are provided only through environment
variables. The repository must not contain real API keys.

Required production variable:

```dotenv
OPENAI_API_KEY=
```

Recommended configurable variables:

```dotenv
OPENAI_BASE_URL=https://api.openai.com/v1
OPENAI_LLM_MODEL=
OPENAI_ASR_MODEL=
OPENAI_TTS_MODEL=
OPENAI_TTS_VOICE=
OPENAI_TIMEOUT=30s
```

## Consequences

- Missing `OPENAI_API_KEY` is a startup configuration error.
- Unit tests must use local stubs or mocked transports, not paid model calls.
- The previous fake provider implementation is removed from production code.
- Provider selection remains behind project-owned interfaces, so future provider
  changes do not leak into domain or application services.
