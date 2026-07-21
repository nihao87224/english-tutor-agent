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

## 4. Validate starter package

```bash
python scripts/validate_project.py
```

## 5. Backend

后端工程在 M0-T02 生成后更新本节，至少包含：

```bash
./mvnw test
./mvnw spring-boot:run
```

健康检查预期：

```text
GET http://localhost:8080/actuator/health
```

## 6. Android

Android 工程在 M0-T03 生成后更新本节。

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

M0/M1 不应要求真实付费密钥。

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
