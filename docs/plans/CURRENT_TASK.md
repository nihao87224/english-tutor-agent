# Current Task

> Start a new task by replacing this file. Only one development task may be active at a time.

## Task

`DOCS-T01 Professional README Refresh`

## Status

`DONE`

## Goal

Rewrite the root README into a professional open-source project landing page that quickly explains what English Tutor Agent does, what problem it solves, its advantages, current status, quick start and deployment path.

## Related documents

- `README.md`
- `docs/assets/project-overview.svg`
- `docs/plans/TASK_BACKLOG.md`
- `docs/deploy/PRODUCTION_DEPLOYMENT.md`
- `docs/process/DEFINITION_OF_DONE.md`

## In Scope

- Replace the outdated starter README.
- Add a project overview image.
- Include a demo URL placeholder for future production deployment.
- Explain current M3 Web Expression Coach status.
- Add quick-start, architecture, roadmap, security and documentation links.
- Update changelog and task record.

## Out of Scope

- Deploying production.
- Changing backend, Web or Android business code.
- Adding screenshots from a live hosted environment.

## Acceptance Criteria

1. README clearly explains product purpose, target problem, advantages and current status.
2. README includes a visual overview image stored in the repository.
3. README includes a demo URL placeholder that can be updated after production deployment.
4. Documentation-only changes pass project validation and diff checks.

## Verification Record

- Temporary validation venv with `scripts\requirements-ci.txt`: `scripts\validate_project.py` - PASS.
- `git diff --check` - PASS. Only Git line-ending conversion warnings were reported.

## Review Status

Completed for README content and repository-local overview image.
