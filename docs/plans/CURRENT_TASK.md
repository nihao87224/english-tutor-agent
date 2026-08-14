# Current Task

> Start a new task by replacing this file. Only one development task may be active at a time.

## Task

`DOCS-T08 Independent Web Jenkins Docker Deployment`

## Status

`DONE`

## Goal

Add an independent Jenkins pipeline for building and deploying the React/Vite Web frontend as an Nginx Docker container, without changing the existing backend Jenkinsfile or backend deployment logic.

## Related documents

- `Jenkinsfile.web`
- `web/Dockerfile`
- `web/nginx.container.conf`
- `web/package.json`
- `web/pnpm-lock.yaml`
- `docs/process/DEFINITION_OF_DONE.md`

## In Scope

- Add a multi-stage Web Dockerfile using Node 22, pnpm 11.16.0, `pnpm test` and a same-origin production build.
- Add an Nginx container config listening on `18082`, serving the SPA and proxying `/api/` and `/actuator/` to the backend on host loopback.
- Add an independent Web Jenkins pipeline that builds, deploys, health-checks and rolls back the `english-tutor-web` container.
- Update changelog and task record.

## Out of Scope

- Changing Web business code.
- Changing the existing backend `Jenkinsfile`.
- Changing backend deployment logic.
- Adding a remote image registry flow.
- Deploying Jenkins or containers from this local machine.
- Committing or pushing unless separately requested.

## Acceptance Criteria

1. Web Docker image build runs `pnpm install --frozen-lockfile`, `pnpm test` and `VITE_API_BASE_URL="" pnpm run build`.
2. Runtime image uses `nginx:alpine` and serves `dist` from the Nginx static directory.
3. Container Nginx listens on `18082`, supports SPA fallback and proxies `/api/` plus `/actuator/`.
4. SSE proxying disables buffering.
5. `Jenkinsfile.web` is independent, uses commit SHA plus UTC timestamp image tags and deploys `english-tutor-web` with `--restart unless-stopped --network host`.
6. Deployment checks `/` and `/actuator/health`, and restores the previous image when startup or health checks fail.
7. Existing backend deployment files are not modified.

## Verification Record

- `pnpm test` from `web/` - PASS.
- `VITE_API_BASE_URL="" pnpm run build` from `web/` - PASS.
- Static checks for Web Dockerfile, Nginx proxy/SSE config and Web Jenkins rollback flow - PASS.
- `git diff --check` - PASS.
- Local `docker build` was not run because Docker is not available in this Windows environment; verify the full image build and deploy flow on the Jenkins Linux runtime.

## Review Status

Completed for independent Web Jenkins Docker deployment files and task documentation.
