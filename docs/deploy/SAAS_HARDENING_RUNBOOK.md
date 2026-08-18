# SaaS Hardening Runbook

This runbook covers the final SaaS Foundation hardening checks before a
limited production pilot.

## Identity Cutover

- Normal learner and admin clients must authenticate with `/api/v1/auth/*` and
  send `Authorization: Bearer <access-token>`.
- No production configuration or API contract may include a switch that accepts
  client-supplied user identity.
- `X-User-Key` may appear only in historical design notes or negative tests that
  prove the old header is rejected or ignored.

## Claim Existing Users

For a real legacy learner that already has profile, assessment, plan, training
or evidence rows, keep the existing `app_user.id`.

1. Take a database backup.
2. Choose the exact legacy `app_user.user_key` to claim.
3. Verify the target row has no `email_normalized` and is not disabled.
4. Generate a password hash through the same application password hasher used by
   registration.
5. Update that row with `email`, `email_normalized`, `password_hash`, locale,
   timezone and `auth_version`.
6. Insert the USER role into `app_user_role` if missing.
7. Log in through `/api/v1/auth/login` and verify `/api/v1/me`,
   `/api/v1/me/quota`, onboarding progress and today's plan.
8. Disable unused legacy rows only after confirming they are not referenced by
   retained learner data.

Do not create a new `app_user` and copy learning data unless a separate migration
plan has been reviewed. The current schema intentionally keeps learning records
attached to `app_user.id`.

## Backup And Rollback

- Take a `mysqldump --single-transaction --routines --triggers` backup before
  every deployment that changes backend code or Flyway migrations.
- Store backups outside `/opt/english-tutor-agent/releases`.
- Restore drills must run against a non-production database before public
  access.
- Flyway migrations are forward-only. Prefer a forward fix migration over
  rolling the database back.
- Restoring a pre-deployment backup is an offline recovery action and requires
  explicit acceptance of data loss after the backup timestamp.

## Secret Scan

Run a source scan before release, excluding dependency caches and build outputs:

```powershell
rg -n "sk-[A-Za-z0-9_-]{20,}|AIza[0-9A-Za-z_-]{20,}|BEGIN (RSA |EC |OPENSSH )?PRIVATE KEY|password\\s*=\\s*[^\\s<].+|api[_-]?key\\s*=\\s*[^\\s<].+" `
  --glob "!web/node_modules/**" `
  --glob "!android/.gradle/**" `
  --glob "!android/app/build/**" `
  --glob "!server/**/target/**" `
  .
```

Expected allowed hits are placeholders in templates, test-only values and UI
prototype masked examples. Any real secret must be removed and rotated before
release.

## Smoke Checks

- Anonymous learner API request returns 401.
- Learner login returns an access token and `/api/v1/me/quota` succeeds with
  bearer auth.
- A learner request with only `X-User-Key` returns 401.
- USER access to `/api/v1/admin/**` returns 403.
- Admin provider secret responses expose only masked hints.
- Quota remaining=1 concurrent reservations allow exactly one success.
- Provider failure before usable output refunds the quota reservation.
- Web learner, Web admin and Android learner smoke flows use bearer auth.
