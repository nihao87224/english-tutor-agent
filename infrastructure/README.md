# Infrastructure

The default local infrastructure is provided by root `docker-compose.yml`:

- MySQL 8.x
- Redis 7.x
- MinIO
- MinIO bucket initialization

Production deployment must not copy the local Compose setup directly. A formal
deployment design will be created before M6.

## Docker Compose Mode

From the repository root:

```powershell
docker compose --env-file .env.example config
docker compose --env-file .env.example up -d
docker compose --env-file .env.example ps
docker compose --env-file .env.example down
```

For daily local development, copy `.env.example` to `.env`, change local
passwords, then use:

```powershell
docker compose --env-file .env up -d
docker compose --env-file .env ps
```

## Services

| Service | Container | Default endpoint | Health |
|---|---|---|---|
| MySQL | `english-tutor-mysql` | `localhost:3306` | `mysqladmin ping` |
| Redis | `english-tutor-redis` | `localhost:6379` | `redis-cli ping` |
| MinIO API | `english-tutor-minio` | `http://localhost:9000` | `/minio/health/live` |
| MinIO Console | `english-tutor-minio` | `http://localhost:9001` | same MinIO container |
| MinIO init | `english-tutor-minio-init` | one-shot | creates `S3_BUCKET` |

The default local bucket is `english-tutor-local`.

## External Infrastructure Mode

Use this mode when Docker is not available on the development machine.

Compatible services:

- MySQL 8.x compatible endpoint
- Redis 7.x compatible endpoint
- S3-compatible object storage endpoint

Required environment variables:

```dotenv
DB_HOST=<external-db-host>
DB_PORT=3306
DB_NAME=<database-name>
DB_USERNAME=<database-user>
DB_PASSWORD=<local-secret>

REDIS_HOST=<external-redis-host>
REDIS_PORT=6379
REDIS_PASSWORD=<local-secret>

S3_ENDPOINT=https://s3.<region>.qiniucs.com
S3_REGION=<qiniu-region-id>
S3_BUCKET=<qiniu-s3-bucket-name>
S3_ACCESS_KEY=<local-secret>
S3_SECRET_KEY=<local-secret>
S3_PUBLIC_BASE_URL=<public-read-base-url>
```

Qiniu Kodo can be used through its AWS S3-compatible endpoints. Signed requests
are required for Qiniu S3 service domains, so TCP connectivity alone does not
prove bucket authorization.

For the current external development environment, the confirmed non-secret
Qiniu values are:

```dotenv
S3_ENDPOINT=https://s3.cn-east-1.qiniucs.com
S3_REGION=cn-east-1
S3_BUCKET=my-photos
S3_PUBLIC_BASE_URL=http://static.forever24.cn
```

Run the non-destructive checker:

```powershell
.\scripts\verify_external_infra.ps1
```

The checker reads secrets from environment variables and does not print them.

## M0-T05 Limitation

External VPS services can be used for Flyway and Spring Boot health checks, but
they do not replace Testcontainers. M0-T05 still needs a Docker-compatible
runtime on the machine or CI runner that executes Testcontainers tests.

## Constraints

- Do not commit `.env`, `.env.*`, real hosts, passwords, access keys or secret keys.
- Local Compose data volumes live under `.infrastructure-data/` and must not be committed.
- Business tables are created by Server Flyway migrations.
- Redis keys must follow the detailed design.
- External databases and buckets must be disposable development resources, not production data.
- Object storage buckets must not contain real user recordings during M0.
- M0-T05 Flyway checks must use an empty or disposable schema because migrations
  are forward-only.
