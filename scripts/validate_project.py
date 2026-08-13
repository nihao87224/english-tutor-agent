#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from typing import Any

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
    "scripts/requirements-ci.txt",
    ".github/workflows/baseline-validation.yml",
    "contracts/openapi/english-tutor-api.yaml",
    "server/pom.xml",
    "server/mvnw.cmd",
    "android/settings.gradle.kts",
    "android/gradlew.bat",
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

def validate_yaml() -> tuple[list[str], dict[str, Any]]:
    errors: list[str] = []
    loaded: dict[str, Any] = {}
    try:
        import yaml
    except ImportError:
        return ["PyYAML is not installed; install dependencies from scripts/requirements-ci.txt"], loaded
    for path in [
        ROOT / "contracts/openapi/english-tutor-api.yaml",
        ROOT / "docker-compose.yml",
        ROOT / ".github/workflows/baseline-validation.yml",
    ]:
        try:
            loaded[path.relative_to(ROOT).as_posix()] = yaml.safe_load(path.read_text(encoding="utf-8"))
        except Exception as exc:
            errors.append(f"Invalid YAML {path.relative_to(ROOT)}: {exc}")
    return errors, loaded

def validate_openapi(loaded_yaml: dict[str, Any]) -> list[str]:
    errors: list[str] = []
    path = "contracts/openapi/english-tutor-api.yaml"
    spec = loaded_yaml.get(path)
    if not isinstance(spec, dict):
        return [f"Invalid OpenAPI {path}: root must be an object"]
    if not str(spec.get("openapi", "")).startswith("3."):
        errors.append(f"Invalid OpenAPI {path}: openapi must declare version 3.x")
    if not isinstance(spec.get("info"), dict):
        errors.append(f"Invalid OpenAPI {path}: missing info object")
    if not isinstance(spec.get("paths"), dict):
        errors.append(f"Invalid OpenAPI {path}: missing paths object")
    return errors

def validate_json_schemas() -> list[str]:
    errors: list[str] = []
    try:
        from jsonschema import Draft202012Validator
    except ImportError:
        return ["jsonschema is not installed; install dependencies from scripts/requirements-ci.txt"]

    schemas: dict[str, dict[str, Any]] = {}
    for schema_path in sorted((ROOT / "contracts/schemas").glob("*.schema.json")):
        try:
            schema = json.loads(schema_path.read_text(encoding="utf-8"))
            Draft202012Validator.check_schema(schema)
            schemas[schema_path.name.removesuffix(".schema.json")] = schema
        except Exception as exc:
            errors.append(f"Invalid JSON Schema {schema_path.relative_to(ROOT)}: {exc}")

    for example_path in sorted((ROOT / "contracts/examples").glob("*.example.json")):
        contract_name = example_path.name.removesuffix(".example.json")
        schema = schemas.get(contract_name)
        if schema is None:
            errors.append(f"Missing schema for example {example_path.relative_to(ROOT)}")
            continue
        try:
            example = json.loads(example_path.read_text(encoding="utf-8"))
            validator = Draft202012Validator(schema)
            for violation in sorted(validator.iter_errors(example), key=lambda item: list(item.path)):
                location = ".".join(str(part) for part in violation.path) or "$"
                errors.append(
                    f"Example {example_path.relative_to(ROOT)} violates {contract_name}.schema.json at {location}: "
                    f"{violation.message}"
                )
        except Exception as exc:
            errors.append(f"Invalid schema example {example_path.relative_to(ROOT)}: {exc}")

    for contract_name in sorted(schemas):
        example_path = ROOT / "contracts/examples" / f"{contract_name}.example.json"
        if not example_path.is_file():
            errors.append(f"Missing example for schema {Path('contracts/schemas') / (contract_name + '.schema.json')}")
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
        yaml_errors, loaded_yaml = validate_yaml()
        errors.extend(yaml_errors)
        if not yaml_errors:
            errors.extend(validate_openapi(loaded_yaml))
        errors.extend(validate_json_schemas())

    if errors:
        print("Validation FAILED")
        for error in errors:
            print(f"- {error}")
        return 1

    print("Validation PASSED")
    return 0

if __name__ == "__main__":
    sys.exit(main())
