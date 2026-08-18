# Server

M0-T02 后端工程基线。

## Baseline

- Java 21
- Spring Boot 4.1.0
- Spring AI 2.0.0
- Maven multi-module project
- Spring MVC compatible bootstrap
- ArchUnit module boundary test
- DeepSeek-default LLM with OpenAI-compatible and Gemini protocol adapters behind project-owned interfaces

## Modules

```text
tutor-bootstrap       Spring Boot entrypoint and assembly
tutor-api             HTTP controllers, DTOs, SSE and transport mapping
tutor-application     Application services and use-case orchestration
tutor-domain          Aggregates, value objects and domain rules
tutor-agent           Agent prompts, structured output and AI strategies
tutor-infrastructure  Persistence, cache, object storage and provider adapters
tutor-observability   Tracing, metrics and audit instrumentation
tutor-test-support    Shared test fixtures and local stubs
```

The module shape follows `docs/design/02_DETAILED_DESIGN_BACKEND.md`.

## Commands

From this directory:

```powershell
.\mvnw.cmd clean verify
```

On Unix-like shells:

```bash
./mvnw clean verify
```

Run the minimal Spring Boot application:

```powershell
.\mvnw.cmd -pl tutor-bootstrap spring-boot:run
```

Health endpoint:

```text
GET http://localhost:8080/actuator/health
```

## Database, Redis and Flyway

M0-T05 wires the bootstrap application to infrastructure through environment
variables:

```dotenv
DB_HOST=localhost
DB_PORT=3306
DB_NAME=english_tutor
DB_USERNAME=english_tutor
DB_PASSWORD=change_me
FLYWAY_ENABLED=true

REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=
REDIS_TIMEOUT=2s
```

When a test environment requires exact JDBC flags, use Spring Boot's standard
override variables instead of committing a URL:

```dotenv
SPRING_DATASOURCE_URL=jdbc:mysql://<db-host>:3306/<database-name>?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
SPRING_DATASOURCE_USERNAME=<database-user>
SPRING_DATASOURCE_PASSWORD=<local-secret>
```

Flyway migrations live under `tutor-bootstrap/src/main/resources/db/migration`.
The M0 baseline migration creates only a technical marker table and no business
tables.

The default test profile excludes real datasource, Redis and Flyway
auto-configuration so regular unit tests do not require external services.
`FlywayMysqlContainerSmokeTest` exercises clean MySQL startup through
Testcontainers when a Docker-compatible runtime is available; it is skipped on
machines without Docker.

## AI providers

The application starts without a configured AI provider. Use the admin API or
console to create an enabled default LLM provider, choose its endpoint and
model, then save its API key. OpenAI remains available for LLM, ASR and TTS;
Gemini can be configured as an LLM provider. Provider keys are encrypted at
rest; only the encryption key remains in the environment:

```dotenv
TUTOR_SECRET_ENCRYPTION_KEY=<32-byte-or-base64-encoded-32-byte-secret>
TUTOR_SECRET_ENCRYPTION_KEY_VERSION=v1
```

The provider implementations live in `tutor-agent` and expose project-owned interfaces:

```text
ChatProvider
SpeechToTextProvider
TextToSpeechProvider
```

`ChatProvider.completeStructured` accepts a project-owned `JsonSchema` contract so
schema validation stays behind the provider abstraction.

Runtime provider configuration is persisted in `ai_provider_config`; provider API
keys are stored in `ai_provider_secret` encrypted with AES-GCM. Admin endpoints
under `/api/v1/admin/ai-providers` return only `apiKeyConfigured` and a masked
hint, never the raw key.

## Namespace

Java 根包名固定为：`cn.forever24.tutor`。

## Current out of scope

- Business API endpoints
- Formal business database tables
