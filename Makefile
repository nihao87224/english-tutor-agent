.PHONY: help infra-up infra-down infra-logs validate-docs validate-contracts validate backend-verify android-debug android-test quality-gate

help:
	@echo "Available commands:"
	@echo "  make infra-up            Start MySQL, Redis and MinIO"
	@echo "  make infra-down          Stop local infrastructure"
	@echo "  make infra-logs          Follow infrastructure logs"
	@echo "  make validate            Validate project structure and contracts"
	@echo "  make validate-docs       Check required project files"
	@echo "  make validate-contracts  Validate JSON and YAML contracts"
	@echo "  make backend-verify      Run backend Maven verification"
	@echo "  make android-debug       Build Android debug APK"
	@echo "  make android-test        Run Android debug unit tests"
	@echo "  make quality-gate        Run local non-Docker quality gate"

infra-up:
	docker compose --env-file .env up -d

infra-down:
	docker compose --env-file .env down

infra-logs:
	docker compose --env-file .env logs -f

validate:
	python scripts/validate_project.py

validate-docs:
	python scripts/validate_project.py --docs-only

validate-contracts:
	python scripts/validate_project.py --contracts-only

backend-verify:
	cd server && ./mvnw clean verify

android-debug:
	cd android && ./gradlew :app:assembleDebug

android-test:
	cd android && ./gradlew :app:testDebugUnitTest

quality-gate: validate backend-verify android-debug android-test
