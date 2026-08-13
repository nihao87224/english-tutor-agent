param(
    [Parameter(Mandatory = $true)]
    [string]$EnvFile,

    [Parameter(Mandatory = $true)]
    [string]$RemoteHost,

    [Parameter(Mandatory = $true)]
    [string]$RemoteUser,

    [Parameter(Mandatory = $true)]
    [string]$Domain,

    [string]$DeployRoot = "/opt/english-tutor-agent",
    [string]$ServiceUser = "english-tutor",
    [switch]$SkipTests
)

$ErrorActionPreference = "Stop"

function Fail($Message) {
    throw "ERROR: $Message"
}

function Require-Command($Name) {
    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        Fail "Missing command: $Name"
    }
}

function Read-DotEnv($Path) {
    $result = @{}
    Get-Content $Path | ForEach-Object {
        $line = $_.Trim()
        if ($line -and -not $line.StartsWith("#")) {
            $parts = $line.Split("=", 2)
            if ($parts.Count -eq 2) {
                $result[$parts[0].Trim()] = $parts[1].Trim().Trim('"').Trim("'")
            }
        }
    }
    return $result
}

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..")
Set-Location $repoRoot

if (-not (Test-Path $EnvFile)) {
    Fail "Environment file not found: $EnvFile"
}
if (-not (Test-Path "server\mvnw.cmd")) {
    Fail "Run from a repository that contains server\mvnw.cmd"
}
if (-not (Test-Path "web\package.json")) {
    Fail "Run from a repository that contains web\package.json"
}

Require-Command java
Require-Command pnpm
Require-Command ssh
Require-Command scp
Require-Command tar

$envValues = Read-DotEnv $EnvFile
if (-not ($envValues["DB_HOST"] -or $envValues["SPRING_DATASOURCE_URL"])) {
    Fail "Set DB_HOST or SPRING_DATASOURCE_URL in $EnvFile"
}
if (-not ($envValues["DB_USERNAME"] -or $envValues["SPRING_DATASOURCE_USERNAME"])) {
    Fail "Set DB_USERNAME or SPRING_DATASOURCE_USERNAME in $EnvFile"
}
if (-not ($envValues["DB_PASSWORD"] -or $envValues["SPRING_DATASOURCE_PASSWORD"])) {
    Fail "Set DB_PASSWORD or SPRING_DATASOURCE_PASSWORD in $EnvFile"
}
foreach ($required in @("REDIS_HOST", "S3_ENDPOINT", "S3_BUCKET", "S3_ACCESS_KEY", "S3_SECRET_KEY")) {
    if (-not $envValues[$required]) {
        Fail "Set $required in $EnvFile"
    }
}

$releaseId = (Get-Date).ToUniversalTime().ToString("yyyyMMddTHHmmssZ")
$stagingRoot = Join-Path $repoRoot ".codex\runtime\deploy\$releaseId"
$archive = Join-Path $repoRoot ".codex\runtime\deploy\english-tutor-agent-$releaseId.tar.gz"
New-Item -ItemType Directory -Force $stagingRoot | Out-Null
New-Item -ItemType Directory -Force (Join-Path $stagingRoot "server") | Out-Null
New-Item -ItemType Directory -Force (Join-Path $stagingRoot "web") | Out-Null
New-Item -ItemType Directory -Force (Join-Path $stagingRoot "systemd") | Out-Null
New-Item -ItemType Directory -Force (Join-Path $stagingRoot "nginx") | Out-Null

Write-Host "Building backend..."
Push-Location "server"
if ($SkipTests) {
    .\mvnw.cmd -pl tutor-bootstrap -am -DskipTests package
} else {
    .\mvnw.cmd -pl tutor-bootstrap -am clean verify
}
Pop-Location

Write-Host "Building web..."
Push-Location "web"
pnpm install --frozen-lockfile
if (-not $SkipTests) {
    pnpm test
}
$env:VITE_API_BASE_URL = if ($envValues.ContainsKey("VITE_API_BASE_URL")) { $envValues["VITE_API_BASE_URL"] } else { "" }
pnpm run build
Pop-Location

$backendJar = Join-Path $repoRoot "server\tutor-bootstrap\target\tutor-bootstrap-0.1.0-SNAPSHOT.jar"
if (-not (Test-Path $backendJar)) {
    Fail "Backend jar not found: $backendJar"
}
if (-not (Test-Path "web\dist")) {
    Fail "Web dist not found"
}

Copy-Item $backendJar (Join-Path $stagingRoot "server\tutor-bootstrap.jar")
Copy-Item "web\dist\*" (Join-Path $stagingRoot "web") -Recurse -Force
Copy-Item "scripts\deploy\systemd\english-tutor-agent.service" (Join-Path $stagingRoot "systemd\english-tutor-agent.service")
Copy-Item "scripts\deploy\nginx\english-tutor-agent.conf" (Join-Path $stagingRoot "nginx\english-tutor-agent.conf")

if (Test-Path $archive) {
    Remove-Item -LiteralPath $archive -Force
}
tar -czf $archive -C $stagingRoot .

$remote = "$RemoteUser@$RemoteHost"
$remoteArchive = "/tmp/english-tutor-agent-$releaseId.tar.gz"
$remoteEnvFile = "/tmp/english-tutor-agent-$releaseId.env"

Write-Host "Uploading release archive to $remote..."
scp $archive "${remote}:$remoteArchive"
scp $EnvFile "${remote}:$remoteEnvFile"

$remoteScript = @'
set -euo pipefail

require() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "ERROR: Missing command on remote host: $1" >&2
    exit 1
  }
}

require sudo
require tar
require sed
require curl

release_dir="$DEPLOY_ROOT/releases/$RELEASE_ID"
shared_dir="$DEPLOY_ROOT/shared"

if ! id "$SERVICE_USER" >/dev/null 2>&1; then
  sudo useradd --system --home "$DEPLOY_ROOT" --shell /usr/sbin/nologin "$SERVICE_USER"
fi

sudo install -d -o "$SERVICE_USER" -g "$SERVICE_USER" "$DEPLOY_ROOT" "$DEPLOY_ROOT/releases" "$shared_dir"
sudo install -d -o "$SERVICE_USER" -g "$SERVICE_USER" "$release_dir"
sudo tar -xzf "$REMOTE_ARCHIVE" -C "$release_dir"

sudo cp "$REMOTE_ENV_FILE" "$shared_dir/production.env"
sudo chown "$SERVICE_USER:$SERVICE_USER" "$shared_dir/production.env"
sudo chmod 600 "$shared_dir/production.env"
sudo chown -R "$SERVICE_USER:$SERVICE_USER" "$release_dir"
sudo ln -sfn "$release_dir" "$DEPLOY_ROOT/current"
sudo chown -h "$SERVICE_USER:$SERVICE_USER" "$DEPLOY_ROOT/current"

backend_port="$(grep -E '^SERVER_PORT=' "$shared_dir/production.env" | tail -n 1 | cut -d= -f2-)"
backend_port="${backend_port:-8080}"

tmp_service="$(mktemp)"
tmp_nginx="$(mktemp)"
sed \
  -e "s#__SERVICE_USER__#$SERVICE_USER#g" \
  -e "s#__DEPLOY_ROOT__#$DEPLOY_ROOT#g" \
  "$release_dir/systemd/english-tutor-agent.service" > "$tmp_service"
sed \
  -e "s#__APP_DOMAIN__#$APP_DOMAIN#g" \
  -e "s#__DEPLOY_ROOT__#$DEPLOY_ROOT#g" \
  -e "s#__BACKEND_PORT__#$backend_port#g" \
  "$release_dir/nginx/english-tutor-agent.conf" > "$tmp_nginx"

sudo install -m 0644 "$tmp_service" /etc/systemd/system/english-tutor-agent.service
sudo install -m 0644 "$tmp_nginx" /etc/nginx/sites-available/english-tutor-agent.conf
sudo ln -sfn /etc/nginx/sites-available/english-tutor-agent.conf /etc/nginx/sites-enabled/english-tutor-agent.conf

rm -f "$tmp_service" "$tmp_nginx"
sudo rm -f "$REMOTE_ARCHIVE"
sudo rm -f "$REMOTE_ENV_FILE"

sudo systemctl daemon-reload
sudo systemctl enable english-tutor-agent
sudo systemctl restart english-tutor-agent
sudo nginx -t
sudo systemctl reload nginx

for _ in $(seq 1 30); do
  if curl -fsS "http://127.0.0.1:$backend_port/actuator/health" >/dev/null; then
    echo "Backend health check passed."
    echo "Deployment complete: http://$APP_DOMAIN"
    exit 0
  fi
  sleep 2
done

sudo journalctl -u english-tutor-agent -n 120 --no-pager >&2 || true
echo "ERROR: Backend health check did not pass" >&2
exit 1
'@

Write-Host "Installing release on remote host..."
$remoteEnv = "APP_DOMAIN='$Domain' DEPLOY_ROOT='$DeployRoot' SERVICE_USER='$ServiceUser' RELEASE_ID='$releaseId' REMOTE_ARCHIVE='$remoteArchive' REMOTE_ENV_FILE='$remoteEnvFile'"
$remoteScript | ssh $remote "$remoteEnv bash -s"

Write-Host "Deployment complete: http://$Domain"
