# Deployment Scripts

Production deployment helpers:

- `production.env.example` - copy to a secure location and fill production
  environment variables. Real secrets must not be committed.
- `deploy_production.sh` - build and install a release directly on a Linux
  server with systemd and Nginx.
- `deploy_production.ps1` - build on a Windows workstation, upload through
  SSH/SCP, and install on a Linux server.
- `systemd/english-tutor-agent.service` - backend service template.
- `nginx/english-tutor-agent.conf` - same-origin Web/API reverse-proxy template.

See `docs/deploy/PRODUCTION_DEPLOYMENT.md` for the full deployment workflow.
