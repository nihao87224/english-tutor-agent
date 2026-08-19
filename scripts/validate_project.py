#!/usr/bin/env python3
from __future__ import annotations

import argparse
import copy
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
        for path in (ROOT / folder).rglob("*.json"):
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


def _json_pointer_parent(document: Any, pointer: str) -> tuple[Any, str | int]:
    if not pointer.startswith("/"):
        raise ValueError(f"JSON Pointer must start with '/': {pointer}")
    parts = [part.replace("~1", "/").replace("~0", "~") for part in pointer[1:].split("/")]
    current = document
    for part in parts[:-1]:
        current = current[int(part)] if isinstance(current, list) else current[part]
    last: str | int = int(parts[-1]) if isinstance(current, list) else parts[-1]
    return current, last


def _apply_fixture_mutations(base: dict[str, Any], mutations: list[dict[str, Any]]) -> dict[str, Any]:
    document = copy.deepcopy(base)
    for mutation in mutations:
        operation = mutation.get("operation")
        parent, key = _json_pointer_parent(document, mutation.get("path", ""))
        if operation in {"add", "replace"}:
            parent[key] = copy.deepcopy(mutation.get("value"))
        elif operation == "removeMatching":
            values = parent[key]
            expected = mutation.get("match", {})
            if not isinstance(values, list) or not isinstance(expected, dict):
                raise ValueError("removeMatching requires an array target and object match")
            parent[key] = [
                item for item in values
                if not isinstance(item, dict) or any(item.get(field) != value for field, value in expected.items())
            ]
        else:
            raise ValueError(f"Unsupported fixture mutation operation: {operation}")
    return document


def _validate_v2_business_rules(manifest: dict[str, Any]) -> list[tuple[str, str]]:
    violations: list[tuple[str, str]] = []
    lesson = manifest.get("lessonPackage", {})
    variants = manifest.get("skillUnitVariants", [])
    mappings = manifest.get("episodeMappings", [])
    assets = manifest.get("assets", [])

    if lesson.get("character") != "Lin Muen":
        violations.append(("LIN_MUEN_REQUIRED", "lessonPackage.character must be Lin Muen"))
    if lesson.get("resourceId") != manifest.get("resourceId"):
        violations.append(("RESOURCE_ID_MISMATCH", "manifest and lessonPackage resourceId must match"))
    if lesson.get("resourceVersion") != manifest.get("resourceVersion"):
        violations.append(("RESOURCE_VERSION_MISMATCH", "manifest and lessonPackage resourceVersion must match"))

    variant_ids = [item.get("skillUnitVariantId") for item in variants if isinstance(item, dict)]
    mapping_ids = [item.get("episodeMappingId") for item in mappings if isinstance(item, dict)]
    asset_ids = [item.get("assetId") for item in assets if isinstance(item, dict)]
    for identifier, values, rule in [
        ("Skill Unit Variant", variant_ids, "DUPLICATE_SKILL_UNIT_VARIANT_ID"),
        ("Episode Mapping", mapping_ids, "DUPLICATE_EPISODE_MAPPING_ID"),
        ("asset", asset_ids, "DUPLICATE_ASSET_ID"),
    ]:
        if len(values) != len(set(values)):
            violations.append((rule, f"{identifier} ids must be unique"))

    for variant in variants:
        criteria = variant.get("evidenceCriteria", []) if isinstance(variant, dict) else []
        if not criteria:
            violations.append(("EVIDENCE_CRITERIA_REQUIRED", "every Skill Unit Variant needs evidence criteria"))
            continue
        criterion_id_list = [item.get("criterionId") for item in criteria if isinstance(item, dict)]
        criterion_ids = set(criterion_id_list)
        if len(criterion_id_list) != len(criterion_ids):
            violations.append(("DUPLICATE_EVIDENCE_CRITERION_ID", "evidence criterion ids must be unique within a variant"))
        required_ids = set(variant.get("completionPolicy", {}).get("requiredCriterionIds", []))
        if not required_ids.issubset(criterion_ids):
            violations.append(("COMPLETION_CRITERIA_REFERENCE", "completion policy references unknown evidence criteria"))

    lesson_variant_ids = set(lesson.get("skillUnitVariantIds", []))
    lesson_mapping_ids = set(lesson.get("episodeMappingIds", []))
    if not lesson_variant_ids.issubset(set(variant_ids)):
        violations.append(("SKILL_UNIT_REFERENCE", "lessonPackage references an unknown Skill Unit Variant"))
    if not lesson_mapping_ids.issubset(set(mapping_ids)):
        violations.append(("EPISODE_MAPPING_REFERENCE", "lessonPackage references an unknown Episode Mapping"))

    asset_id_set = set(asset_ids)
    lesson_asset_ids = set(lesson.get("imageAssetIds", [])) | set(lesson.get("audioAssetIds", []))
    if not lesson_asset_ids.issubset(asset_id_set):
        violations.append(("ASSET_REFERENCE", "lessonPackage references an unknown asset"))

    task_heroes = [
        asset for asset in assets
        if isinstance(asset, dict) and asset.get("mediaType") == "IMAGE" and asset.get("purpose") == "task_hero"
    ]
    if len(task_heroes) != 1:
        violations.append(("TASK_HERO_COUNT", "a publishable lesson must have exactly one task_hero"))
    else:
        hero = task_heroes[0]
        required_surfaces = {"scenario_intro", "scenario_training"}
        if not required_surfaces.issubset(set(hero.get("displaySurfaces", []))):
            violations.append(("TASK_HERO_SURFACES", "task_hero must appear in intro and training surfaces"))
        if hero.get("assetId") not in set(lesson.get("imageAssetIds", [])):
            violations.append(("TASK_HERO_REFERENCE", "lessonPackage must reference its task_hero"))
        if hero.get("sceneId") != lesson.get("sceneId"):
            violations.append(("TASK_HERO_SCENE", "task_hero sceneId must match the lesson scene"))
        if "Lin Muen" not in hero.get("generationPrompt", ""):
            violations.append(("TASK_HERO_PROMPT_CHARACTER", "task_hero prompt must explicitly name Lin Muen"))
        if not any(reference.startswith("lin-muen-") for reference in hero.get("characterReferenceIds", [])):
            violations.append(("TASK_HERO_CHARACTER_REFERENCE", "task_hero must use a Lin Muen character reference"))

    for mapping in mappings:
        if not isinstance(mapping, dict):
            continue
        if mapping.get("skillUnitVariantId") not in set(variant_ids):
            violations.append(("MAPPING_SKILL_REFERENCE", "Episode Mapping references an unknown Skill Unit Variant"))
        refs = mapping.get("assetVariantRefs", {})
        mapping_assets = {refs.get("taskHeroAssetId")} | set(refs.get("sceneStateAssetIds", [])) | set(refs.get("audioAssetIds", []))
        if not mapping_assets.issubset(asset_id_set):
            violations.append(("MAPPING_ASSET_REFERENCE", "Episode Mapping references an unknown asset"))

    transcript = lesson.get("transcript", {})
    transcript_id = transcript.get("transcriptId")
    transcript_sentences = transcript.get("sentences", [])
    if lesson.get("dialogue") != transcript_sentences:
        violations.append(("DIALOGUE_TRANSCRIPT_MISMATCH", "dialogue and transcript must match sentence by sentence"))
    for asset in assets:
        if not isinstance(asset, dict) or asset.get("mediaType") != "AUDIO":
            continue
        if asset.get("transcriptRef") != transcript_id:
            violations.append(("AUDIO_TRANSCRIPT_REFERENCE", "audio transcriptRef must target the lesson transcript"))
        if asset.get("audioScript") != transcript_sentences:
            violations.append(("AUDIO_TRANSCRIPT_MISMATCH", "audioScript and transcript must match sentence by sentence"))
    return violations


def validate_v2_content_contracts() -> list[str]:
    errors: list[str] = []
    try:
        from jsonschema import Draft202012Validator
        from referencing import Registry, Resource
    except ImportError:
        return ["jsonschema/referencing is not installed; install dependencies from scripts/requirements-ci.txt"]

    schema_dir = ROOT / "contracts/schemas/v2"
    schemas: list[tuple[Path, dict[str, Any]]] = []
    for schema_path in sorted(schema_dir.glob("*.schema.json")):
        try:
            schema = json.loads(schema_path.read_text(encoding="utf-8"))
            Draft202012Validator.check_schema(schema)
            schemas.append((schema_path, schema))
        except Exception as exc:
            errors.append(f"Invalid V2 JSON Schema {schema_path.relative_to(ROOT)}: {exc}")
    required_schema_names = {
        "asset-metadata.schema.json",
        "episode-mapping.schema.json",
        "learning-resource-manifest.schema.json",
        "lesson-package.schema.json",
        "skill-unit-variant.schema.json",
    }
    missing_schema_names = required_schema_names.difference(path.name for path, _ in schemas)
    errors.extend(f"Missing V2 JSON Schema contracts/schemas/v2/{name}" for name in sorted(missing_schema_names))
    if errors:
        return errors

    registry = Registry().with_resources(
        (schema["$id"], Resource.from_contents(schema)) for _, schema in schemas
    )
    manifest_schema = next(
        (schema for path, schema in schemas if path.name == "learning-resource-manifest.schema.json"),
        None,
    )
    if manifest_schema is None:
        return ["Missing V2 learning-resource-manifest.schema.json"]
    validator = Draft202012Validator(manifest_schema, registry=registry)

    valid_dir = ROOT / "contracts/examples/v2/valid"
    valid_paths = sorted(valid_dir.glob("*.manifest.json"))
    if not valid_paths:
        errors.append("Missing valid V2 manifest examples")
    for example_path in valid_paths:
        manifest = json.loads(example_path.read_text(encoding="utf-8"))
        schema_violations = list(validator.iter_errors(manifest))
        business_violations = _validate_v2_business_rules(manifest)
        for violation in sorted(schema_violations, key=lambda item: list(item.path)):
            location = ".".join(str(part) for part in violation.path) or "$"
            errors.append(f"Valid V2 example {example_path.relative_to(ROOT)} violates JSON Schema at {location}: {violation.message}")
        for rule, message in business_violations:
            errors.append(f"Valid V2 example {example_path.relative_to(ROOT)} violates {rule}: {message}")

    invalid_dir = ROOT / "contracts/examples/v2/invalid"
    fixture_paths = sorted(invalid_dir.glob("*.fixture.json"))
    if not fixture_paths:
        errors.append("Missing invalid V2 contract fixtures")
    for fixture_path in fixture_paths:
        try:
            fixture = json.loads(fixture_path.read_text(encoding="utf-8"))
            base_path = (fixture_path.parent / fixture["baseExample"]).resolve()
            if valid_dir.resolve() not in base_path.parents:
                raise ValueError("baseExample must resolve inside contracts/examples/v2/valid")
            base = json.loads(base_path.read_text(encoding="utf-8"))
            manifest = _apply_fixture_mutations(base, fixture["mutations"])
            triggered_rules = {"JSON_SCHEMA"} if list(validator.iter_errors(manifest)) else set()
            triggered_rules.update(rule for rule, _ in _validate_v2_business_rules(manifest))
            expected_rule = fixture["expectedRule"]
            if expected_rule not in triggered_rules:
                errors.append(
                    f"Invalid V2 fixture {fixture_path.relative_to(ROOT)} did not trigger {expected_rule}; "
                    f"triggered: {sorted(triggered_rules) or ['none']}"
                )
        except Exception as exc:
            errors.append(f"Invalid V2 fixture {fixture_path.relative_to(ROOT)}: {exc}")
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
        errors.extend(validate_v2_content_contracts())

    if errors:
        print("Validation FAILED")
        for error in errors:
            print(f"- {error}")
        return 1

    print("Validation PASSED")
    return 0

if __name__ == "__main__":
    sys.exit(main())
