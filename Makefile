.PHONY: help infra-up infra-down infra-logs validate-docs validate-contracts

help:
	@echo "Available commands:"
	@echo "  make infra-up            Start MySQL, Redis and MinIO"
	@echo "  make infra-down          Stop local infrastructure"
	@echo "  make infra-logs          Follow infrastructure logs"
	@echo "  make validate-docs       Check required project files"
	@echo "  make validate-contracts  Validate JSON and YAML contracts"

infra-up:
	docker compose --env-file .env up -d

infra-down:
	docker compose --env-file .env down

infra-logs:
	docker compose --env-file .env logs -f

validate-docs:
	python scripts/validate_project.py --docs-only

validate-contracts:
	python scripts/validate_project.py --contracts-only
