# Current Task

> Start a new task by replacing this file. Only one development task may be active at a time.

## Task

`DOCS-T02 README Open Source Layout Review`

## Status

`DONE`

## Goal

Refine the root README against common high-star open-source README conventions, remove milestone-progress reporting, fix the unreliable overview image usage, and add future screenshot placeholders for Web and Android.
Declare the repository license as MIT.

## Related documents

- `README.md`
- `docs/assets/screenshots/.gitkeep`
- `LICENSE`
- `docs/plans/TASK_BACKLOG.md`
- `docs/deploy/PRODUCTION_DEPLOYMENT.md`
- `docs/process/DEFINITION_OF_DONE.md`

## In Scope

- Review high-star open-source README structure.
- Replace the unreliable SVG overview image usage.
- Add screenshot placeholder section for Web, Android and summary screens.
- Include a demo URL placeholder for future production deployment.
- Remove the current-progress table from README.
- Add the MIT license file and README license link.
- Keep quick-start, architecture, roadmap, security and documentation links.
- Update changelog and task record.

## Out of Scope

- Deploying production.
- Changing backend, Web or Android business code.
- Adding screenshots from a live hosted environment.

## Acceptance Criteria

1. README clearly explains product purpose, target problem and advantages without a milestone progress table.
2. README includes screenshot placeholders with future replacement paths.
3. README includes a demo URL placeholder that can be updated after production deployment.
4. Documentation-only changes pass project validation and diff checks.
5. Repository license is declared as MIT.

## Verification Record

- Temporary validation venv with `scripts\requirements-ci.txt`: `scripts\validate_project.py` - PASS after README and MIT license updates.
- `git diff --check` - PASS. Only Git line-ending conversion warnings were reported.

## Review Status

Completed for README structure review, screenshot placeholders and removal of unreliable SVG hero usage.
MIT license declaration added.
