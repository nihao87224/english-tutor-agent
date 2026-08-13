# Current Task

> Start a new task by replacing this file. Only one development task may be active at a time.

## Task

`OPS-T01 Production Deployment Documentation`

## Status

`DONE`

## Goal

Document a production deployment path for the current Web-first build and provide complete deployment scripts with sensitive values supplied through environment variables.

## Related documents

- `docs/deploy/PRODUCTION_DEPLOYMENT.md`
- `scripts/deploy/production.env.example`
- `scripts/deploy/deploy_production.sh`
- `scripts/deploy/deploy_production.ps1`
- `scripts/deploy/systemd/english-tutor-agent.service`
- `scripts/deploy/nginx/english-tutor-agent.conf`
- `docs/process/DEFINITION_OF_DONE.md`

## In Scope

- Provide a production deployment guide.
- Leave sensitive values blank and documented in an environment template.
- Provide Linux server deployment script.
- Provide Windows-to-Linux deployment script.
- Provide systemd and Nginx templates.
- Update changelog and script index.

## Out of Scope

- Executing production deployment.
- Provisioning managed MySQL, Redis, object storage, DNS or TLS certificates.
- Adding authentication, CORS support or release-hardening features.
- Changing backend or Web business code.

## Acceptance Criteria

1. Production deployment guide explains architecture, prerequisites, environment variables, deployment, TLS, rollback and operations checks.
2. Deployment scripts build backend and Web artifacts, install a timestamped release, update systemd/Nginx, and run health checks.
3. Sensitive values are not committed and are represented by blank environment variables with comments.
4. Static script parsing and project validation checks pass or documented limitations are recorded.

## Verification Record

- PowerShell parser check for `scripts/deploy/deploy_production.ps1` - PASS.
- Bash parser check for `scripts/deploy/deploy_production.sh` - PASS.
- Temporary validation venv with `scripts\requirements-ci.txt`: `scripts\validate_project.py` - PASS.
- `git diff --check` - PASS. Only Git line-ending conversion warnings were reported.

## Review Status

Completed for documentation and deployment script generation. Production deployment itself was not executed.
