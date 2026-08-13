# Current Task

> Start a new task by replacing this file. Only one development task may be active at a time.

## Task

`DOCS-T03 Backend Production Deployment Guide`

## Status

`DONE`

## Goal

Regenerate a backend-only production deployment guide in Chinese with detailed step-by-step commands, explicit environment variable placeholders, systemd operation, health checks, rollback and troubleshooting.

## Related documents

- `docs/deploy/BACKEND_PRODUCTION_DEPLOYMENT.md`
- `scripts/deploy/production.env.example`
- `scripts/deploy/systemd/english-tutor-agent.service`
- `server/tutor-bootstrap/src/main/resources/application.yml`
- `docs/process/DEFINITION_OF_DONE.md`

## In Scope

- Create a backend-only production deployment document under `docs/deploy`.
- Use Chinese explanations.
- Provide copyable commands for server preparation, environment variables, build, release installation, systemd, health checks, rollback and troubleshooting.
- Leave sensitive values blank or as placeholders with explanatory comments.
- Keep frontend deployment out of scope.
- Update changelog and task record.

## Out of Scope

- Deploying production.
- Changing backend, Web or Android business code.
- Rewriting the frontend deployment guide.
- Adding or changing runtime scripts.

## Acceptance Criteria

1. Backend production deployment guide is separate from frontend deployment documentation.
2. Guide is written in Chinese.
3. Guide includes detailed step-by-step commands.
4. Sensitive values are left blank or represented by clear placeholders.
5. Documentation-only changes pass project validation and diff checks.

## Verification Record

- Temporary validation venv with `scripts\requirements-ci.txt`: `scripts\validate_project.py` - PASS after rerun.
- `git diff --check` - PASS. Only Git line-ending conversion warnings were reported.
- Trailing whitespace check for deployment guide, changelog and task record - PASS.

## Review Status

Completed for backend-only deployment guide drafting and validation.

## Follow-up Update

- Added `README_zh.md` for Chinese-speaking users and linked it from `README.md`.
- Re-ran `scripts\validate_project.py` and `git diff --check` after the README follow-up - PASS.
