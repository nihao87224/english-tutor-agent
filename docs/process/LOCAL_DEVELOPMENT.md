# Local Development

> 本文档在 M0 完成后必须由实际命令校准，不能长期保留未经验证的占位内容。

## 1. Prerequisites

预计需要：

- Git
- Docker Desktop / Docker Engine + Compose
- Java 21
- Android Studio 与项目确定的 Android SDK
- Python 3.11+（契约与项目检查脚本）
- 可选：Make

## 2. Initialize

```bash
cp .env.example .env
```

修改 `.env` 中本地密码。不要提交 `.env`。

## 3. Start infrastructure

```bash
docker compose --env-file .env up -d
docker compose ps
```

服务：

- MySQL: `localhost:3306`
- Redis: `localhost:6379`
- MinIO API: `localhost:9000`
- MinIO Console: `localhost:9001`
- MinIO bucket: `english-tutor-local`

首次启动会运行 `minio-init` 一次性服务，用于创建本地对象存储 bucket。检查状态：

```bash
docker compose --env-file .env ps
docker compose --env-file .env logs minio-init
```

停止本地基础设施：

```bash
docker compose --env-file .env down
```

### External infrastructure mode

If Docker is unavailable, use external development services instead of local
Compose. Keep real values in your local shell or ignored `.env`; never commit
them.

Required variables:

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

For Qiniu Kodo, use its AWS S3-compatible endpoint and the S3 bucket name shown
in the Qiniu console. Signed S3 requests are required for object storage
authorization checks.

Current external development object storage values:

```dotenv
S3_ENDPOINT=https://s3.cn-east-1.qiniucs.com
S3_REGION=cn-east-1
S3_BUCKET=my-photos
S3_PUBLIC_BASE_URL=http://static.forever24.cn
```

Check non-destructive connectivity:

```powershell
.\scripts\verify_external_infra.ps1
```

M0-T05 note: external VPS services can be used for Flyway and Spring Boot health
checks, but Testcontainers still needs a Docker-compatible runtime on the
machine or CI runner that executes those tests.

## 4. Validate starter package

```bash
pip install -r scripts/requirements-ci.txt
python scripts/validate_project.py
```

本地非 Docker 质量门：

```bash
make quality-gate
```

Windows PowerShell 可分别执行：

```powershell
python scripts\validate_project.py
cd server
.\mvnw.cmd clean verify
cd ..\android
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
```

## 5. Backend

后端工程基线在 M0-T02 初始化。进入 `server/` 后执行：

```powershell
cd server
.\mvnw.cmd clean verify
.\mvnw.cmd -pl tutor-bootstrap spring-boot:run
```

Unix-like shell 可使用 `./mvnw clean verify` 和 `./mvnw -pl tutor-bootstrap spring-boot:run`。

健康检查预期：

```text
GET http://localhost:8080/actuator/health
```

### Backend with real infrastructure

For M0-T05 and later infrastructure checks, provide these variables through the
local shell or ignored `.env`:

```dotenv
DB_HOST=<db-host>
DB_PORT=3306
DB_NAME=<database-name>
DB_USERNAME=<database-user>
DB_PASSWORD=<local-secret>
FLYWAY_ENABLED=true

REDIS_HOST=<redis-host>
REDIS_PORT=6379
REDIS_PASSWORD=<local-secret>
REDIS_TIMEOUT=2s
```

If the environment requires exact MySQL JDBC parameters, set Spring Boot's
standard datasource overrides locally:

```dotenv
SPRING_DATASOURCE_URL=jdbc:mysql://<db-host>:3306/<database-name>?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
SPRING_DATASOURCE_USERNAME=<database-user>
SPRING_DATASOURCE_PASSWORD=<local-secret>
```

Regular `test` profile runs exclude real datasource, Redis and Flyway
auto-configuration. The Testcontainers MySQL smoke test runs only when Docker is
available; without Docker it is skipped and the task verification must record
that limitation.

Run the external test-environment check only with local environment variables:

```powershell
cd server
.\mvnw.cmd -pl tutor-bootstrap -am -Dtest=ExternalInfrastructureHealthTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Set `TUTOR_RUN_EXTERNAL_INFRA_TESTS=true` in the same process to enable the test.
Actual Testcontainers execution is split to a Docker/CI follow-up task.

## 6. Android

Android 工程基线在 M0-T03 初始化。进入 `android/` 后执行：

```powershell
cd android
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
```

Unix-like shell 可使用 `./gradlew :app:assembleDebug` 和 `./gradlew :app:testDebugUnitTest`。

如果未设置 `ANDROID_HOME` 或 `ANDROID_SDK_ROOT`，在 `android/local.properties` 中配置本机 SDK 路径。该文件不得提交。

模拟器访问宿主机后端使用：

```text
http://10.0.2.2:8080
```

真机调试需要使用局域网地址或 `adb reverse`。

## 7. Fake provider mode

本地默认：

```dotenv
LLM_PROVIDER=fake
ASR_PROVIDER=fake
TTS_PROVIDER=fake
```

M0/M1 不应要求真实付费密钥。默认 fake provider 由后端 `tutor-agent` 模块装配：

- `ChatProvider` 返回固定结构化 JSON 和固定 stream chunks；
- `SpeechToTextProvider` 返回固定英文转写和置信度；
- `TextToSpeechProvider` 返回固定 fake audio bytes；
- 所有 fake 结果都包含 trace、provider/model、promptVersion、schemaVersion 和 usage metadata。

## 8. Common failures

### Port conflict

修改 `.env` 暴露端口，并同步 Android/后端配置。

### MySQL schema mismatch

停止应用，在确认没有需要保留的数据后删除本地卷：

```bash
docker compose down
rm -rf .infrastructure-data/mysql
docker compose up -d
```

仅适用于本地开发，不可用于生产。

### Android cannot reach localhost

Android 模拟器不能使用宿主机 `localhost`，请使用 `10.0.2.2`。
