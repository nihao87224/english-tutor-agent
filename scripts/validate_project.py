#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

REQUIRED = [
    "AGENTS.md",
    "README.md",
    ".env.example",
    "docker-compose.yml",
    "docs/prd/ENGLISH_TUTOR_AGENT_PRD_v1.0.0.md",
    "docs/design/01_HIGH_LEVEL_DESIGN.md",
    "docs/design/02_DETAILED_DESIGN_BACKEND.md",
    "docs/design/03_DETAILED_DESIGN_ANDROID.md",
    "docs/design/04_DETAILED_DESIGN_AGENT_LEARNING_ENGINE.md",
    "docs/plans/IMPLEMENTATION_PLAN.md",
    "docs/plans/TASK_BACKLOG.md",
    "docs/plans/CURRENT_TASK.md",
    "docs/decisions/ADR_INDEX.md",
    "docs/decisions/ADR-0007-java-spring-baseline.md",
    "docs/decisions/ADR-0011-package-namespace.md",
    "docs/reviews/CURSOR_REVIEW_RESOLUTION_2026-07-21.md",
    "docs/process/DEFINITION_OF_DONE.md",
    "docs/process/REVIEW_CHECKLIST.md",
    "docs/test/ACCEPTANCE_SCENARIOS.md",
    "contracts/openapi/english-tutor-api.yaml",
]

def validate_required() -> list[str]:
    return [f"Missing required file: {p}" for p in REQUIRED if not (ROOT / p).is_file()]

def validate_json() -> list[str]:
    errors: list[str] = []
    for folder in ["contracts/schemas", "contracts/examples"]:
        for path in (ROOT / folder).glob("*.json"):
            try:
                json.loads(path.read_text(encoding="utf-8"))
            except Exception as exc:
                errors.append(f"Invalid JSON {path.relative_to(ROOT)}: {exc}")
    for path in (ROOT / "evaluation").glob("*.jsonl"):
        for lineno, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
            if not line.strip():
                continue
            try:
                json.loads(line)
            except Exception as exc:
                errors.append(f"Invalid JSONL {path.relative_to(ROOT)}:{lineno}: {exc}")
    return errors

def validate_yaml() -> list[str]:
    errors: list[str] = []
    try:
        import yaml
    except ImportError:
        return ["PyYAML is not installed; cannot validate YAML. Install with: pip install pyyaml"]
    for path in [
        ROOT / "contracts/openapi/english-tutor-api.yaml",
        ROOT / "docker-compose.yml",
    ]:
        try:
            yaml.safe_load(path.read_text(encoding="utf-8"))
        except Exception as exc:
            errors.append(f"Invalid YAML {path.relative_to(ROOT)}: {exc}")
    return errors

def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--docs-only", action="store_true")
    parser.add_argument("--contracts-only", action="store_true")
    args = parser.parse_args()

    errors: list[str] = []
    if not args.contracts_only:
        errors.extend(validate_required())
    if not args.docs_only:
        errors.extend(validate_json())
        errors.extend(validate_yaml())

    if errors:
        print("Validation FAILED")
        for error in errors:
            print(f"- {error}")
        return 1

    print("Validation PASSED")
    return 0

if __name__ == "__main__":
    sys.exit(main())
