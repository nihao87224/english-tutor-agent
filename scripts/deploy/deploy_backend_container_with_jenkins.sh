#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'USAGE'
Deploy the English Tutor Agent backend Docker image with Docker Compose.

This script is intended to be installed as a root-owned file on the VPS and
called by Jenkins through a narrow sudoers rule.

Usage:
  deploy_backend_container_with_jenkins.sh --image <image> --release-id <id> [options]

Options:
  --deploy-root <path>    Must be /opt/english-tutor-agent
  --metadata-dir <path>   Optional directory where release metadata is written
  -h, --help              Show this help
USAGE
}

die() {
  echo "ERROR: $*" >&2
  exit 1
}

require_command() {
  command -v "$1" >/dev/null 2>&1 || die "Missing command: $1"
}

backend_image=""
release_id=""
deploy_root="/opt/english-tutor-agent"
metadata_dir=""

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
    --deploy-root)
      deploy_root="${2:-}"
      shift 2
      ;;
    --metadata-dir)
      metadata_dir="${2:-}"
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

[[ -n "$backend_image" ]] || die "--image is required"
[[ "$backend_image" =~ ^[A-Za-z0-9._/:@-]+$ ]] || die "Image contains unsupported characters: $backend_image"
[[ -n "$release_id" ]] || die "--release-id is required"
[[ "$release_id" =~ ^[A-Za-z0-9._-]+$ ]] || die "Release id contains unsupported characters: $release_id"
[[ "$deploy_root" == "/opt/english-tutor-agent" ]] || die "--deploy-root must be /opt/english-tutor-agent"

require_command docker
require_command curl

shared_dir="$deploy_root/shared"
release_dir="$deploy_root/releases/$release_id"
compose_file="$shared_dir/docker-compose.backend.yml"
production_env="$shared_dir/production.env"
image_env="$shared_dir/current-image.env"
current_release_file="$shared_dir/current-release"

[[ -f "$compose_file" ]] || die "Missing Compose file: $compose_file"
[[ -f "$production_env" ]] || die "Missing environment file: $production_env"
docker image inspect "$backend_image" >/dev/null 2>&1 || die "Docker image not found locally: $backend_image"

previous_image=""
previous_release=""
if [[ -f "$image_env" ]]; then
  previous_image="$(sed -n 's/^BACKEND_IMAGE=//p' "$image_env" | tail -n 1)"
fi
if [[ -f "$current_release_file" ]]; then
  previous_release="$(cat "$current_release_file")"
fi

rollback_needed="false"

compose_up() {
  docker compose \
    --env-file "$production_env" \
    --env-file "$image_env" \
    -f "$compose_file" \
    up -d backend
}

write_metadata() {
  install -d -m 0755 "$release_dir"
  printf 'BACKEND_IMAGE=%s\n' "$backend_image" > "$release_dir/release.env"
  printf '%s\n' "$release_id" > "$release_dir/release_id"

  if [[ -n "$metadata_dir" ]]; then
    install -d -m 0755 "$metadata_dir"
    printf '%s\n' "$release_id" > "$metadata_dir/release_id"
    printf '%s\n' "$backend_image" > "$metadata_dir/backend_image"
    printf '%s\n' "$previous_release" > "$metadata_dir/previous_release"
    printf '%s\n' "$previous_image" > "$metadata_dir/previous_image"
  fi
}

health_check() {
  set -a
  # shellcheck disable=SC1090
  . "$production_env"
  set +a
  local host_port="${BACKEND_HOST_PORT:-${SERVER_PORT:-8080}}"

  for _ in $(seq 1 30); do
    if curl -fsS "http://127.0.0.1:$host_port/actuator/health" >/dev/null; then
      return 0
    fi
    sleep 2
  done

  docker compose --env-file "$production_env" --env-file "$image_env" -f "$compose_file" logs --tail=160 backend >&2 || true
  return 1
}

rollback_previous_image() {
  if [[ "$rollback_needed" == "true" && -n "$previous_image" ]]; then
    echo "Rolling back to previous backend image: $previous_image" >&2
    printf 'BACKEND_IMAGE=%s\n' "$previous_image" > "$image_env"
    compose_up || true
  fi
}

trap rollback_previous_image ERR

write_metadata
printf 'BACKEND_IMAGE=%s\n' "$backend_image" > "$image_env"
printf '%s\n' "$release_id" > "$current_release_file"

rollback_needed="true"
compose_up
health_check

rollback_needed="false"
echo "Backend container deployment succeeded: $release_id ($backend_image)"
