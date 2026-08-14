# Production Deployment

This guide deploys the current Web-first English Tutor Agent build to a Linux
server as:

- Nginx serving the Web static bundle.
- Nginx reverse proxying `/api/` and `/actuator/` to the Spring Boot backend on
  `127.0.0.1:8080`.
- Spring Boot running as a systemd service.
- MySQL, Redis and S3-compatible object storage supplied by managed services or
  separately operated infrastructure.

The recommended production shape is same-origin:

```text
https://<your-domain>/
├─ Web static files
├─ /api/v1/*      -> http://127.0.0.1:8080/api/v1/*
└─ /actuator/*    -> http://127.0.0.1:8080/actuator/*
```

Same-origin deployment avoids browser CORS issues. Do not expose the backend
port directly to the public internet.

## Current product limitations

This repository is currently at the M3 Web Expression Coach milestone. It can be
deployed for controlled production-like access, but the following are still not
complete product-hardening items:

- Android voice/listening UI features start in M4.
- Backend AI provider integration uses OpenAI-backed real providers.
- Long-term review, IELTS Speaking and release-hardening are later milestones.
- The first production deployment should be treated as a limited-access pilot
  until authentication, abuse controls, monitoring and data retention policies
  are reviewed for the target audience.

## Server prerequisites

Recommended baseline:

- Ubuntu 24.04 LTS or 22.04 LTS.
- Java 21 runtime.
- Nginx.
- systemd.
- MySQL 8.4-compatible database.
- Redis 7.4-compatible cache.
- S3-compatible object storage.
- A DNS record pointing your domain to the server.
- TLS certificate managed by your platform or Certbot.

Example package installation:

```bash
sudo apt-get update
sudo apt-get install -y openjdk-21-jre-headless nginx rsync curl
```

Optional Certbot installation:

```bash
sudo apt-get install -y certbot python3-certbot-nginx
```

## Environment variables

Copy the template:

```bash
cp scripts/deploy/production.env.example production.env
chmod 600 production.env
```

Then fill the blank values in `production.env`. Keep this file outside git and
only on the deployment machine or your secrets manager.

Important rules:

- Keep all database, Redis, object-storage and AI provider secrets in
  environment variables.
- Keep `FLYWAY_ENABLED=true` for forward-only schema migration.
- Use UTC-compatible database/session settings; user-facing timezone conversion
  belongs at the edge/UI layer.
- For the current M3 build, set `VITE_API_BASE_URL=` to an empty value when
  building the Web bundle for same-origin Nginx reverse proxying.
- Use `LLM_PROVIDER=openai`, `ASR_PROVIDER=openai` and `TTS_PROVIDER=openai`.
  Configure `OPENAI_API_KEY` and model variables in the VPS environment file.
- If a secret contains shell-sensitive characters such as spaces, `#`, `$`, `"`
  or `'`, quote and escape it according to the shell that will read the file.
- Keep optional `SPRING_DATASOURCE_*` overrides commented out unless they are
  intentionally used; empty Spring override variables can break datasource
  startup.

## One-server deployment flow

From the repository root on the server:

```bash
cp scripts/deploy/production.env.example /opt/english-tutor-agent/shared/production.env
sudo editor /opt/english-tutor-agent/shared/production.env
sudo chmod 600 /opt/english-tutor-agent/shared/production.env

scripts/deploy/deploy_production.sh \
  --env-file /opt/english-tutor-agent/shared/production.env \
  --domain example.com
```

The script will:

1. Build and test the backend.
2. Build and test the Web frontend.
3. Create a timestamped release under `/opt/english-tutor-agent/releases/`.
4. Update `/opt/english-tutor-agent/current`.
5. Install or update the systemd service.
6. Install or update the Nginx site config.
7. Restart the backend and reload Nginx.
8. Run health checks.

## Windows workstation to Linux server deployment

From PowerShell on your Windows workstation:

```powershell
Copy-Item scripts\deploy\production.env.example .\production.env
notepad .\production.env

.\scripts\deploy\deploy_production.ps1 `
  -EnvFile .\production.env `
  -RemoteHost example.com `
  -RemoteUser deploy `
  -Domain example.com
```

The PowerShell script builds the release locally, uploads it over SSH/SCP, then
runs the remote installation steps on the Linux host.

## TLS

After the Nginx site is installed and DNS points to the server, enable TLS:

```bash
sudo certbot --nginx -d example.com
```

Then verify:

```bash
curl -fsS https://example.com/actuator/health
curl -fsS https://example.com/
```

## Rollback

List releases:

```bash
ls -1 /opt/english-tutor-agent/releases
```

Rollback to a known release:

```bash
sudo ln -sfn /opt/english-tutor-agent/releases/<release-id> /opt/english-tutor-agent/current
sudo systemctl restart english-tutor-agent
sudo nginx -t
sudo systemctl reload nginx
```

Database migrations are forward-only. A code rollback does not automatically
rollback schema changes; review Flyway migrations before deploying changes that
alter persisted data.

## Operations checks

Backend:

```bash
sudo systemctl status english-tutor-agent --no-pager
sudo journalctl -u english-tutor-agent -n 200 --no-pager
curl -fsS http://127.0.0.1:8080/actuator/health
```

Nginx:

```bash
sudo nginx -t
sudo systemctl status nginx --no-pager
sudo tail -n 200 /var/log/nginx/english-tutor-agent.access.log
sudo tail -n 200 /var/log/nginx/english-tutor-agent.error.log
```

Smoke test through public domain:

```bash
curl -fsS https://example.com/
curl -fsS https://example.com/actuator/health
```

## Security checklist before public access

- Domain uses HTTPS.
- Only ports 80 and 443 are public.
- Backend port 8080 is bound to localhost or blocked by firewall.
- `production.env` is readable only by root/service operators.
- MySQL and Redis require passwords and are not publicly exposed.
- Object storage bucket is private unless a specific public-read path is
  intentionally required.
- Logs do not contain authorization headers, API keys or unnecessary user text.
- Database backups and restore drills are configured.
- Access to the pilot environment is restricted until release-hardening tasks
  are complete.
