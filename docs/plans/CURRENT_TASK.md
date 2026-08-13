# Current Task

> Start a new task by replacing this file. Only one development task may be active at a time.

## Task

`DOCS-T04 Chinese README User-Facing Rewrite`

## Status

`DONE`

## Goal

Rewrite the Chinese README sections `为什么做这个项目` and `亮点` so they speak to product users, reflect the original user pain points, and avoid technical implementation details that ordinary learners do not care about.

## Related documents

- `README_zh.md`
- `CHANGELOG.md`
- `docs/modify/English_Tutor_Agent_V1.0_原始用户端产品需求.md`
- `docs/prd/ENGLISH_TUTOR_AGENT_PRD_v1.0.0.md`
- `docs/process/DEFINITION_OF_DONE.md`

## In Scope

- Read the original user-facing requirement discussion and current PRD.
- Rewrite the Chinese README motivation section in a more conversational tone.
- Rewrite the Chinese README highlights to focus on differences from common English learning apps.
- Remove user-facing technical points from these two sections.
- Update changelog and task record.

## Out of Scope

- Changing backend, Web or Android implementation code.
- Adding generated or external images.
- Rewriting the English README.
- Changing product requirements.
- Committing or pushing unless separately requested.

## Acceptance Criteria

1. The motivation section reflects the original pain points: input stronger than output, passive vocabulary, sentence organization, unnatural expression, delayed feedback and missing learning loop.
2. The highlights section explains user-visible differences from common English learning apps.
3. Technical implementation details such as Fake Provider, OpenAPI, schema validation and deployment baseline are not used as user-facing highlights.
4. Documentation-only changes pass project validation and diff checks.

## Verification Record

- Temporary validation venv with `scripts\requirements-ci.txt`: `scripts\validate_project.py` - PASS.
- `git diff --check` - PASS. Only Git line-ending conversion warnings were reported.

## Review Status

Completed for Chinese README user-facing motivation and highlights rewrite.
