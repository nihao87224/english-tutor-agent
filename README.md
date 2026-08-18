# English Tutor Agent

> An open-source AI English Expression Coach for deliberate practice, layered feedback, and adaptive learning plans.

[中文说明](README_zh.md)

<p>
  <img alt="Java 21" src="https://img.shields.io/badge/Java-21-007396?logo=openjdk&logoColor=white">
  <img alt="Spring Boot" src="https://img.shields.io/badge/Spring%20Boot-4.1-6DB33F?logo=springboot&logoColor=white">
  <img alt="React" src="https://img.shields.io/badge/React-19-61DAFB?logo=react&logoColor=111111">
  <img alt="TypeScript" src="https://img.shields.io/badge/TypeScript-5.7-3178C6?logo=typescript&logoColor=white">
  <img alt="Playwright" src="https://img.shields.io/badge/Playwright-E2E-2EAD33?logo=playwright&logoColor=white">
  <img alt="MySQL" src="https://img.shields.io/badge/MySQL-8.4-4479A1?logo=mysql&logoColor=white">
  <img alt="Redis" src="https://img.shields.io/badge/Redis-7.4-DC382D?logo=redis&logoColor=white">
</p>

English Tutor Agent helps learners move beyond one-off grammar correction. It turns daily English practice into a repeatable loop: plan the right task, express an idea, receive focused feedback, try again, and preserve learning evidence for the next plan.

## Links

- Demo: `https://your-production-demo-url.example.com`
- Deployment guide: [docs/deploy/PRODUCTION_DEPLOYMENT.md](docs/deploy/PRODUCTION_DEPLOYMENT.md)
- SaaS hardening runbook: [docs/deploy/SAAS_HARDENING_RUNBOOK.md](docs/deploy/SAAS_HARDENING_RUNBOOK.md)
- Product requirements: [docs/prd/ENGLISH_TUTOR_AGENT_PRD_v1.0.0.md](docs/prd/ENGLISH_TUTOR_AGENT_PRD_v1.0.0.md)
- Architecture docs: [docs/design/00_README.md](docs/design/00_README.md)
- SaaS Foundation v1.1.0: [docs/design/ENGLISH_TUTOR_AGENT_SAAS_FOUNDATION_DESIGN_v1.1.0.md](docs/design/ENGLISH_TUTOR_AGENT_SAAS_FOUNDATION_DESIGN_v1.1.0.md)
- API contract: [contracts/openapi/english-tutor-api.yaml](contracts/openapi/english-tutor-api.yaml)

> Replace the demo URL after production deployment.

## Screenshots

Screenshots are intentionally left as placeholders until the production demo is deployed. Add real images under `docs/assets/screenshots/` and replace the placeholder text below.

| Web Expression Coach | Android App | Daily Summary |
|---|---|---|
| Coming soon: `docs/assets/screenshots/web-expression-coach.png` | Coming soon: `docs/assets/screenshots/android-home.png` | Coming soon: `docs/assets/screenshots/daily-summary.png` |

Suggested replacement format:

```markdown
![Web Expression Coach](docs/assets/screenshots/web-expression-coach.png)
```

## Why this project

Most language-learning tools and general-purpose chatbots are good at isolated answers, but weak at sustained skill growth:

- They correct a sentence but do not remember what should be practiced next.
- They over-correct and interrupt the learner's willingness to express ideas.
- They depend on non-deterministic AI output that is hard to test or ship safely.
- They lack a durable model of goals, preferences, practice evidence, and planning.

English Tutor Agent is designed around a learning loop rather than a chat window.

## Highlights

- Expression-first coaching: the learner produces language before seeing feedback.
- Layered correction: grammar issue, explanation, natural expression, and severity are separated.
- Try Again loop: feedback leads back to rewritten or retold output.
- Adaptive daily plans: task focus can change from learning evidence instead of static lessons.
- Structured AI boundary: provider output is parsed, schema-validated, and business-validated.
- OpenAI-backed providers: production runtime uses real LLM, ASR, and TTS integrations configured by environment variables.
- SaaS identity baseline: Web and Android clients use email/password sessions,
  bearer tokens, daily quota and role-protected admin APIs.
- Legacy client-supplied user identity headers are not part of the production
  contract.
- Contract-first backend: OpenAPI, JSON Schema, Flyway migrations, and validation scripts keep implementation aligned.
- Production-oriented baseline: Nginx, systemd, environment template, and deployment scripts are included.

## How it works

```text
Learner
  -> Today Plan
  -> Web Expression Coach
  -> Streaming AI Reply
  -> Layered Correction
  -> Try Again
  -> Learning Evidence
  -> Daily Summary
  -> Next Plan
```

The key design choice is separation of responsibility:

- deterministic rules decide learning priority, due review, state transitions, and persistence;
- AI providers generate constrained coaching content, analysis, and language feedback;
- provider-specific SDKs stay behind project-owned interfaces;
- automated tests use local stubs or fixed responses instead of calling paid model APIs.

## Architecture

```text
english-tutor-agent/
├── web/                  React + TypeScript Web Expression Coach
├── android/              Android Compose baseline
├── server/               Java 21 + Spring Boot modular monolith
├── contracts/            OpenAPI, JSON Schema, examples
├── docs/                 PRD, design, ADR, process, deployment
├── scripts/              validation, infrastructure, deployment scripts
└── infrastructure/       local infrastructure notes
```

Backend modules:

```text
server/
├── tutor-domain          domain rules, aggregates, value objects
├── tutor-application     use-case orchestration
├── tutor-api             HTTP controllers, DTOs, SSE mapping
├── tutor-agent           AI provider abstraction and OpenAI adapters
├── tutor-infrastructure  JDBC repositories, cache, storage adapters
├── tutor-observability   tracing, metrics, audit extension points
└── tutor-bootstrap       Spring Boot entrypoint and Flyway migrations
```

Runtime view:

```text
Browser / Android
      |
      v
Spring Boot API  -----> AI Provider Interface -----> OpenAI provider
      |
      +-------------> MySQL + Flyway
      +-------------> Redis
      +-------------> S3-compatible object storage
```

## Quickstart

### Prerequisites

- Java 21
- Node.js + pnpm
- Docker and Docker Compose
- Python 3.11+ for validation scripts

### Start infrastructure

```bash
cp .env.example .env
docker compose --env-file .env up -d
```

Local services:

- MySQL: `localhost:3306`
- Redis: `localhost:6379`
- MinIO: `localhost:9000`

### Run the backend

```powershell
cd server
.\mvnw.cmd clean verify
.\mvnw.cmd -pl tutor-bootstrap spring-boot:run
```

Health check:

```text
GET http://localhost:8080/actuator/health
```

### Run the web app

```powershell
cd web
pnpm install
pnpm run dev
```

The Web app defaults to `http://localhost:8080`. To use another API origin:

```powershell
$env:VITE_API_BASE_URL="https://your-api.example.com"
pnpm run dev
```

## Testing

```powershell
# Project structure, YAML, OpenAPI, JSON Schema, examples
python scripts\validate_project.py

# Backend
cd server
.\mvnw.cmd clean verify

# Web
cd web
pnpm test
pnpm run build
pnpm run e2e
```

## Deployment

The repository includes a single-server production deployment baseline:

- Nginx serves the Web static bundle.
- Nginx reverse-proxies `/api/` and `/actuator/` to Spring Boot.
- systemd manages the backend process.
- MySQL, Redis, object storage, and provider secrets are supplied through environment variables.

Start here:

- [Production deployment guide](docs/deploy/PRODUCTION_DEPLOYMENT.md)
- [Environment template](scripts/deploy/production.env.example)
- [Linux deploy script](scripts/deploy/deploy_production.sh)
- [Windows-to-Linux deploy script](scripts/deploy/deploy_production.ps1)

## Roadmap

- SaaS Foundation: email/password auth, CurrentActor isolation, daily quota, admin console, runtime AI provider configuration, and audit.
- Voice practice: Android recording, upload, ASR, TTS, low-confidence confirmation.
- Long-term learning: review scheduler, mastery state, weekly reports.
- IELTS Speaking: Part 1/2/3 practice, full simulation, rubric-based practice feedback.
- Release hardening: privacy deletion, monitoring, cost metrics, rollout and rollback playbooks.

## Security and privacy

- Do not commit API keys, tokens, real user recordings, or private learning data.
- Do not log full authorization headers, secrets, or unnecessary raw user text.
- Store time in UTC and convert for user-facing display.
- Use Flyway forward migrations for database changes.
- IELTS scoring must be clearly labeled as practice feedback, not an official score.

## Documentation

- [PRD](docs/prd/ENGLISH_TUTOR_AGENT_PRD_v1.0.0.md)
- [Design index](docs/design/00_README.md)
- [SaaS Foundation design v1.1.0](docs/design/ENGLISH_TUTOR_AGENT_SAAS_FOUNDATION_DESIGN_v1.1.0.md)
- [SaaS Foundation implementation plan](docs/plans/SAAS_FOUNDATION_IMPLEMENTATION_PLAN_v1.1.0.md)
- [Current task](docs/plans/CURRENT_TASK.md)
- [Task backlog](docs/plans/TASK_BACKLOG.md)
- [Definition of Done](docs/process/DEFINITION_OF_DONE.md)
- [Production deployment](docs/deploy/PRODUCTION_DEPLOYMENT.md)

## License

This project is licensed under the [MIT License](LICENSE).
