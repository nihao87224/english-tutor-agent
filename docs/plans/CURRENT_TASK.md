# Current Task

> Start a new task by replacing this file. Only one development task may be active at a time.

## Task

`DOCS-T07 Dockerized Jenkins Backend Deployment`

## Status

`DONE`

## Goal

Switch backend production deployment to Docker images built by Jenkins and run on the VPS through Docker Compose, so future work can focus on business code rather than deployment mechanics.

## Related documents

- `Jenkinsfile`
- `server/Dockerfile`
- `.dockerignore`
- `scripts/deploy/docker-compose.backend.yml`
- `scripts/deploy/deploy_backend_container_with_jenkins.sh`
- `scripts/deploy/rollback_backend_container.sh`
- `scripts/deploy/production.env.example`
- `docs/deploy/BACKEND_PRODUCTION_DEPLOYMENT.md`
- `docs/process/DEFINITION_OF_DONE.md`

## In Scope

- Add a backend Docker image definition.
- Add Docker build context exclusions.
- Add a production Docker Compose backend service.
- Update Jenkins to build and deploy backend Docker images.
- Add controlled container deploy and rollback scripts.
- Rewrite backend deployment documentation around Docker Compose.
- Update changelog and task record.

## Out of Scope

- Changing backend, Web or Android implementation code.
- Deploying Jenkins or the backend from this local machine.
- Frontend containerization.
- Adding a remote container registry flow.
- Storing production secrets in the repository or Docker image.
- Committing or pushing unless separately requested.

## Acceptance Criteria

1. Jenkins builds a backend Docker image after backend verification.
2. Backend production runtime is described as Docker Compose, not Jar + systemd.
3. Production secrets remain outside Git and outside Docker image layers.
4. Deployment and rollback scripts use fixed `/opt/english-tutor-agent` conventions and validate inputs.
5. Documentation explains VPS setup, Jenkins setup, daily deployment, rollback and security risks.
6. Documentation and static checks pass.

## Verification Record

- Temporary validation venv with `scripts\requirements-ci.txt`: `scripts\validate_project.py` - PASS.
- Docker Compose backend YAML parse with PyYAML - PASS.
- Static secret scan for Jenkins, Docker, deployment scripts and docs - PASS.
- Static checks for Dockerfile, Compose health check, Jenkins Docker build stage and LF-only deployment files - PASS.
- `git diff --check` - PASS. Only Git line-ending conversion warnings were reported.
- Local `docker build`, `docker compose config`, `bash -n` and `shellcheck` were not run because this Windows environment has no Docker, bash or shellcheck; verify on the VPS/Jenkins Linux runtime during first setup.

## Review Status

Completed for Dockerized Jenkins backend deployment files and documentation.
