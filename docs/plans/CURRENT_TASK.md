# Current Task

## Task

`V2.0 Dual-Core Product Rebaseline`

## Status

`DONE`

## Goal

Rebase V2.0 so the product simultaneously delivers a professional,
evidence-driven personalized AI tutor and a motivating Lin Muen immersive
learning experience.

## Implemented

- Defined Personalized Tutor Core as the teaching decision authority.
- Defined Lin Muen Season / Episode as the immersive experience layer.
- Separated Capability Graph from Experience Graph through Episode Mapping.
- Replaced fixed Episode curriculum assumptions with Daily Learning
  Prescription, Skill Unit variants and evidence-driven selection.
- Added deterministic pedagogical baselines for prerequisites, mastery,
  spacing, difficulty, interleaving, transfer and retry.
- Updated PRD, architecture, curriculum, recommendation, resource schema,
  episode, character and content-generation guidance.

## Verification Record

- Bundled Python `scripts\validate_project.py` — PASS.
- `git diff --check` — PASS.
- Markdown reference and terminology audit — PASS.

## Decision Note

Season 1 `EP001`–`EP010` remains the narrative world, while the 72 launch
items are Skill Unit variants. The tutor chooses the teaching target first and
maps it to an eligible Episode second; story order cannot override learner
needs, review urgency or access rules.

## Follow-up Task

Create the machine-readable V2.0 Episode/Skill Unit JSON Schema and regenerate
Season 1 resources only after media-generation parameters are frozen.
