#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'USAGE'
Deploy English Tutor Agent to a Linux production host.

Usage:
  scripts/deploy/deploy_production.sh --env-file <path> --domain <domain> [options]

Options:
  --env-file <path>       Required. Secure production environment file.
  --domain <domain>       Required. Public domain for Nginx server_name.
  --deploy-root <path>    Default: /opt/english-tutor-agent
  --service-user <user>   Default: english-tutor
  --skip-tests            Build packages without running tests.
  -h, --help              Show this help.

Run from the repository root.
USAGE
}

die() {
  echo "ERROR: $*" >&2
  exit 1
}

require_command() {
  command -v "$1" >/dev/null 2>&1 || die "Missing command: $1"
}

env_file=""
app_domain=""
deploy_root="/opt/english-tutor-agent"
service_user="english-tutor"
skip_tests="false"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --env-file)
      env_file="${2:-}"
      shift 2
      ;;
    --domain)
      app_domain="${2:-}"
      shift 2
      ;;
    --deploy-root)
      deploy_root="${2:-}"
      shift 2
      ;;
    --service-user)
      service_user="${2:-}"
      shift 2
      ;;
    --skip-tests)
      skip_tests="true"
      shift
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

[[ -n "$env_file" ]] || die "--env-file is required"
[[ -f "$env_file" ]] || die "Environment file not found: $env_file"
[[ -n "$app_domain" ]] || die "--domain is required"
[[ -f "server/mvnw" || -f "server/mvnw.cmd" ]] || die "Run this script from the repository root"
[[ -f "web/package.json" ]] || die "Run this script from the repository root"

require_command java
require_command pnpm
require_command rsync
require_command sudo
require_command curl

set -a
# shellcheck disable=SC1090
. "$env_file"
set +a

backend_port="${SERVER_PORT:-8080}"
release_id="$(date -u +%Y%m%dT%H%M%SZ)"
release_dir="$deploy_root/releases/$release_id"
shared_dir="$deploy_root/shared"
service_template="scripts/deploy/systemd/english-tutor-agent.service"
nginx_template="scripts/deploy/nginx/english-tutor-agent.conf"

[[ -n "${DB_HOST:-}${SPRING_DATASOURCE_URL:-}" ]] || die "Set DB_HOST or SPRING_DATASOURCE_URL in $env_file"
[[ -n "${DB_USERNAME:-}${SPRING_DATASOURCE_USERNAME:-}" ]] || die "Set DB_USERNAME or SPRING_DATASOURCE_USERNAME in $env_file"
[[ -n "${DB_PASSWORD:-}${SPRING_DATASOURCE_PASSWORD:-}" ]] || die "Set DB_PASSWORD or SPRING_DATASOURCE_PASSWORD in $env_file"
[[ -n "${REDIS_HOST:-}" ]] || die "Set REDIS_HOST in $env_file"
[[ -n "${S3_ENDPOINT:-}" ]] || die "Set S3_ENDPOINT in $env_file"
[[ -n "${S3_BUCKET:-}" ]] || die "Set S3_BUCKET in $env_file"
[[ -n "${S3_ACCESS_KEY:-}" ]] || die "Set S3_ACCESS_KEY in $env_file"
[[ -n "${S3_SECRET_KEY:-}" ]] || die "Set S3_SECRET_KEY in $env_file"

echo "Building backend..."
if [[ "$skip_tests" == "true" ]]; then
  (cd server && ./mvnw -pl tutor-bootstrap -am -DskipTests package)
else
  (cd server && ./mvnw -pl tutor-bootstrap -am clean verify)
fi

echo "Building web..."
(cd web && pnpm install --frozen-lockfile)
if [[ "$skip_tests" != "true" ]]; then
  (cd web && pnpm test)
fi
(cd web && VITE_API_BASE_URL="${VITE_API_BASE_URL-}" pnpm run build)

backend_jar="server/tutor-bootstrap/target/tutor-bootstrap-0.1.0-SNAPSHOT.jar"
[[ -f "$backend_jar" ]] || die "Backend jar not found: $backend_jar"
[[ -d "web/dist" ]] || die "Web dist not found"

echo "Installing release $release_id into $release_dir..."
if ! id "$service_user" >/dev/null 2>&1; then
  sudo useradd --system --home "$deploy_root" --shell /usr/sbin/nologin "$service_user"
fi

sudo install -d -o "$service_user" -g "$service_user" "$deploy_root" "$deploy_root/releases" "$shared_dir"
sudo install -d -o "$service_user" -g "$service_user" "$release_dir/server" "$release_dir/web"
sudo install -m 0644 "$backend_jar" "$release_dir/server/tutor-bootstrap.jar"
sudo rsync -a --delete web/dist/ "$release_dir/web/"
sudo cp "$env_file" "$shared_dir/production.env"
sudo chown "$service_user:$service_user" "$shared_dir/production.env"
sudo chmod 600 "$shared_dir/production.env"

sudo ln -sfn "$release_dir" "$deploy_root/current"
sudo chown -h "$service_user:$service_user" "$deploy_root/current"

tmp_service="$(mktemp)"
tmp_nginx="$(mktemp)"
sed \
  -e "s#__SERVICE_USER__#$service_user#g" \
  -e "s#__DEPLOY_ROOT__#$deploy_root#g" \
  "$service_template" > "$tmp_service"
sed \
  -e "s#__APP_DOMAIN__#$app_domain#g" \
  -e "s#__DEPLOY_ROOT__#$deploy_root#g" \
  -e "s#__BACKEND_PORT__#$backend_port#g" \
  "$nginx_template" > "$tmp_nginx"

sudo install -m 0644 "$tmp_service" /etc/systemd/system/english-tutor-agent.service
sudo install -m 0644 "$tmp_nginx" /etc/nginx/sites-available/english-tutor-agent.conf
sudo ln -sfn /etc/nginx/sites-available/english-tutor-agent.conf /etc/nginx/sites-enabled/english-tutor-agent.conf

rm -f "$tmp_service" "$tmp_nginx"

echo "Restarting services..."
sudo systemctl daemon-reload
sudo systemctl enable english-tutor-agent
sudo systemctl restart english-tutor-agent
sudo nginx -t
sudo systemctl reload nginx

echo "Waiting for backend health..."
for _ in $(seq 1 30); do
  if curl -fsS "http://127.0.0.1:$backend_port/actuator/health" >/dev/null; then
    echo "Backend health check passed."
    echo "Deployment complete: http://$app_domain"
    exit 0
  fi
  sleep 2
done

sudo journalctl -u english-tutor-agent -n 120 --no-pager >&2 || true
die "Backend health check did not pass"
