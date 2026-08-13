#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'USAGE'
Roll back the English Tutor Agent backend container to an existing release image.

Usage:
  rollback_backend_container.sh --release-id <id> [options]
  rollback_backend_container.sh --previous [options]
  rollback_backend_container.sh --image <image> [options]

Options:
  --deploy-root <path>    Must be /opt/english-tutor-agent
  -h, --help              Show this help
USAGE
}

die() {
  echo "ERROR: $*" >&2
  exit 1
}

backend_image=""
deploy_root="/opt/english-tutor-agent"
release_id=""
use_previous="false"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --image)
      backend_image="${2:-}"
      shift 2
      ;;
    --release-id)
      release_id="${2:-}"
      shift 2
      ;;
    --previous)
      use_previous="true"
      shift
      ;;
    --deploy-root)
      deploy_root="${2:-}"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      die "Unknown argument: $1"
      ;;
  esac
done

[[ "$deploy_root" == "/opt/english-tutor-agent" ]] || die "--deploy-root must be /opt/english-tutor-agent"

shared_dir="$deploy_root/shared"
compose_file="$shared_dir/docker-compose.backend.yml"
production_env="$shared_dir/production.env"
image_env="$shared_dir/current-image.env"
current_release_file="$shared_dir/current-release"

[[ -f "$compose_file" ]] || die "Missing Compose file: $compose_file"
[[ -f "$production_env" ]] || die "Missing environment file: $production_env"

if [[ "$use_previous" == "true" ]]; then
  mapfile -t releases < <(ls -1dt "$deploy_root"/releases/* 2>/dev/null || true)
  current_release="$(cat "$current_release_file" 2>/dev/null || true)"
  for candidate in "${releases[@]}"; do
    candidate_id="$(basename "$candidate")"
    if [[ "$candidate_id" != "$current_release" && -f "$candidate/release.env" ]]; then
      backend_image="$(sed -n 's/^BACKEND_IMAGE=//p' "$candidate/release.env" | tail -n 1)"
      release_id="$candidate_id"
      break
    fi
  done
  [[ -n "$backend_image" ]] || die "No previous release image found"
elif [[ -n "$release_id" ]]; then
  [[ "$release_id" =~ ^[A-Za-z0-9._-]+$ ]] || die "Release id contains unsupported characters: $release_id"
  release_env="$deploy_root/releases/$release_id/release.env"
  [[ -f "$release_env" ]] || die "Release metadata not found: $release_env"
  backend_image="$(sed -n 's/^BACKEND_IMAGE=//p' "$release_env" | tail -n 1)"
else
  [[ -n "$backend_image" ]] || die "--image, --release-id or --previous is required"
fi

[[ "$backend_image" =~ ^[A-Za-z0-9._/:@-]+$ ]] || die "Image contains unsupported characters: $backend_image"
docker image inspect "$backend_image" >/dev/null 2>&1 || die "Docker image not found locally: $backend_image"

printf 'BACKEND_IMAGE=%s\n' "$backend_image" > "$image_env"
if [[ -n "$release_id" ]]; then
  printf '%s\n' "$release_id" > "$current_release_file"
fi

docker compose --env-file "$production_env" --env-file "$image_env" -f "$compose_file" up -d backend

set -a
# shellcheck disable=SC1090
. "$production_env"
set +a
host_port="${BACKEND_HOST_PORT:-${SERVER_PORT:-8080}}"

for _ in $(seq 1 30); do
  if curl -fsS "http://127.0.0.1:$host_port/actuator/health" >/dev/null; then
    echo "Backend container rollback succeeded: $backend_image"
    exit 0
  fi
  sleep 2
done

docker compose --env-file "$production_env" --env-file "$image_env" -f "$compose_file" logs --tail=160 backend >&2 || true
die "Rollback health check failed"
