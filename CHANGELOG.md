# Changelog

All notable project changes are recorded here.

## [Unreleased]

### Changed
- Rebased V2.0 around a dual-core product model: an evidence-driven
  Personalized Tutor Core selects Skill Units and teaching strategies first,
  then maps them into Lin Muen Season/Episode immersive experiences.
- Added OpenAI-compatible and Gemini native LLM protocol adapters, with
  DeepSeek configured as the default LLM provider.
- Removed legacy AI provider-selection settings so stale values cannot prevent
  the OpenAI-backed runtime providers from starting.
- Replaced the legacy deterministic AI provider baseline with OpenAI-backed real LLM, ASR and TTS provider implementations configured by environment variables.
- Reworked backend production deployment around Jenkins-built Docker images and Docker Compose releases on the VPS.
- Rewrote the Chinese README user-facing motivation and highlights around original user pain points and product differences instead of technical implementation details.
- Refined the README toward a high-star open-source layout, removed the progress table, replaced the unreliable SVG hero with screenshot placeholders, and documented future screenshot paths.
- Rewrote the root README as a professional open-source landing page with demo placeholder, overview graphic, project positioning, quick start and deployment links.
- Rebaselined V1.0 direction to Web-first English Expression Coach, moving Android voice/listening to a later milestone and updating PRD, design, backlog and UI prototype docs.
- Expanded baseline validation to check JSON Schema contracts and examples.
- Strengthened M0-T02 ArchUnit module boundary coverage.
- Resolved Cursor review findings for task numbering, ADR mapping and phased gates.
- Locked Java 21, Spring Boot 4.1.0 and Spring AI 2.0.0.
- Changed Java root package to `cn.forever24.tutor`.
- Updated PRD phase metadata without changing the v1.0.0 product baseline.
- Added Cursor review and formal resolution records.
- Synced merged design doc milestone numbering with TASK_BACKLOG.
- Extended design architecture decisions through ADR-012.
- Locked Android DI to Hilt; disallowed Java/Spring stack downgrade without decision.


### Added
- Added the V2.0 Curriculum/Skill Graph vertical slice with V19 tables, deterministic graph/import validation, active-variant queries and transactional in-memory/JDBC repositories.
- Added V2.0 machine-readable content contracts for manifests, Skill Unit Variants, Episode Mappings, lesson packages and media metadata, with Lin Muen/task-hero/evidence/audio-transcript validation fixtures.
- Added an independent Web Jenkins pipeline, multi-stage Web Docker image and container Nginx config for same-origin `/api` production deployment.
- Added a backend Dockerfile, Docker Compose production backend service, Jenkins image pipeline, controlled container deploy script and container rollback script.
- Added a Chinese root README for Chinese-speaking users and linked it from the English README.
- Added a Chinese backend-only production deployment guide with step-by-step commands, environment placeholders, systemd setup, health checks, rollback and troubleshooting.
- Added the MIT license file and linked it from the README.
- Added production deployment documentation, environment template, systemd/Nginx templates and Linux/PowerShell deployment scripts.
- Added M3-T08 Playwright E2E coverage for the complete Web Expression Coach path.
- Added the M3-T07 Web daily summary and next-focus completion view.
- Added the M3-T06 Try Again retry loop in the Web coach workspace.
- Added the M3-T05 layered correction and natural expression panel for Web coach streams.
- Added the M3-T04 Web coach workspace with POST SSE text streaming replies.
- Added the M3-T03 Web today's expression coach homepage with plan loading states.
- Added the M3-T02 shortest Web onboarding flow with local user-key persistence.
- Initialized the M3-T01 Web frontend project with a typed REST/SSE API client baseline.
- Added M2-T07 next-plan auto-adjustment after evidence-backed training completion, with idempotent planning-state updates and E2E coverage.
- Added M2-T06 deterministic daily training summaries on session completion with persisted `summary_json`, idempotent completion responses and OpenAPI coverage.
- Added M2-T05 learning evidence generation for text task attempts with Flyway V10 persistence, skill-state updates and `evidenceCount` receipts.
- Added M2-T04 layered correction for conversation SSE streams with `correction_ready` events, deterministic fake analyzer coverage and OpenAPI updates.
- Added M2-T03 single-request SSE conversation streaming with fake-provider-backed `status`, `text_delta` and `done` events plus OpenAPI and integration coverage.
- Added M2-T02 text training task attempt submission with idempotent receipts, raw-text privacy handling, current-task validation and OpenAPI coverage.
- Added M2-T01 backend training-session lifecycle with state-machine rules, Flyway V9 persistence, HTTP endpoints, OpenAPI updates and integration coverage from today's plan.
- Added M1-T10 Android first-use E2E state flow for initial profile and today-plan display.
- Added M1-T09 first rule-based today-plan generation with Flyway V8 plan/task persistence and `GET /api/v1/plans/today`.
- Added M1-T08 initial learner profile completion/result flow with Flyway V7 skill-state persistence and deterministic 8-dimension profile generation.
- Added M1-T07 open text assessment answer evaluation with a versioned fake evaluator, structured validation fallback and Android open-answer feedback state.
- Added M1-T06 deterministic objective assessment answer scoring with Flyway V6 attempt persistence, answer receipt endpoint and Android receipt state.
- Added M1-T05 adaptive initial assessment session start/resume flow with Flyway V5 persistence, HTTP endpoint, duplicate recovery and Android session state.
- Added M1-T04 four-skill self-assessment submission with Flyway V4 persistence, deterministic starting estimate and Android self-rating state.
- Added M1-T03 deterministic onboarding progress recovery with backend status mapping and Android recovery state.
- Added M1-T02 learning, correction, reminder and privacy preference onboarding slice with Flyway V3 persistence and Android preference state.
- Added M1-T01 primary learning goal onboarding slice with backend persistence, HTTP endpoints, Flyway V2 profile tables and Android selection state.
- Added M0-T08 cold-start validation record for project checks, backend, Android and external infrastructure startup.
- Added M0-T05 Flyway baseline, MySQL/Redis health wiring and Testcontainers smoke-test path.
- Added M0-T05 external test-environment Flyway, MySQL, Redis and health integration test.
- Added explicit M0-T05 Flyway bootstrap configuration for Spring Boot 4.1 startup.
- Added M0-T04 external development infrastructure guidance for VPS MySQL, VPS Redis and S3-compatible object storage.
- Added the M0-T07 deterministic fake LLM, ASR and TTS provider baseline.
- Added the M0-T06 CI quality gates for contracts, backend and Android.
- Added the M0-T04 local MySQL, Redis and MinIO Compose baseline with bucket initialization.
- Initialized the M0-T03 Android Compose and Hilt debug build baseline.
- Initialized the M0-T02 backend Maven multi-module baseline.
- Added the Spring Boot bootstrap application and module boundary tests.
- Vibe Coding starter structure.
- Product, design and UI baseline documents.
- Cursor rules and reusable commands.
- Initial OpenAPI and AI JSON Schema contracts.
- Implementation plan, backlog, acceptance scenarios and review process.
