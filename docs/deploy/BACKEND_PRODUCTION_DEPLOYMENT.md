# 后端生产部署指南

本文档只覆盖 English Tutor Agent 后端服务的生产部署。前端 Web 静态资源、Android 安装包发布与前端反向代理细节会拆到单独文档。

后端当前形态：

- Java 21 + Spring Boot 4.1.0；
- 模块化单体，启动模块为 `server/tutor-bootstrap`；
- 构建产物为可执行 Jar：`tutor-bootstrap-0.1.0-SNAPSHOT.jar`；
- 运行方式建议为 Linux + systemd；
- 数据库使用 MySQL 8.x，通过 Flyway 前向迁移；
- 短期状态、幂等、锁和缓存使用 Redis 7.x；
- 音频/文件类资源按设计放入 S3 兼容对象存储；
- 生产环境敏感信息全部通过环境变量或 systemd `EnvironmentFile` 注入，不写入代码仓库。

推荐部署拓扑：

```text
用户 / 前端
   |
   | HTTPS
   v
Nginx / 网关
   |
   | 仅内网或本机访问
   v
Spring Boot 后端 127.0.0.1:8080
   |       |        |
   v       v        v
MySQL    Redis    S3 兼容对象存储
```

> 安全原则：不要把 `8080` 后端端口直接暴露到公网。生产公网只开放 `80/443`，由 Nginx、负载均衡或 API 网关转发到后端。

## 1. 约定和占位符

下面命令会使用这些占位符，请部署时替换成你的真实值：

| 占位符 | 示例 | 说明 |
| --- | --- | --- |
| `<APP_DOMAIN>` | `english.example.com` | 对外访问域名 |
| `<DEPLOY_ROOT>` | `/opt/english-tutor-agent` | 服务部署根目录 |
| `<SERVICE_USER>` | `english-tutor` | 运行后端的 Linux 系统用户 |
| `<DB_HOST>` | `10.0.0.10` | MySQL 地址 |
| `<DB_PASSWORD>` | 留空后自行填写 | MySQL 密码，不能提交到 Git |
| `<REDIS_HOST>` | `10.0.0.11` | Redis 地址 |
| `<REDIS_PASSWORD>` | 留空后自行填写 | Redis 密码，不能提交到 Git |
| `<S3_ENDPOINT>` | `https://s3.example.com` | S3 兼容服务 Endpoint |
| `<S3_ACCESS_KEY>` | 留空后自行填写 | 对象存储 Access Key |
| `<S3_SECRET_KEY>` | 留空后自行填写 | 对象存储 Secret Key |

如果没有特殊说明，本文默认：

```bash
APP_DOMAIN="<APP_DOMAIN>"
DEPLOY_ROOT="/opt/english-tutor-agent"
SERVICE_USER="english-tutor"
SERVER_PORT="8080"
```

## 2. 服务器准备

以下命令在生产服务器上执行。示例系统为 Ubuntu 22.04 LTS / 24.04 LTS。

### 2.1 创建部署用户

```bash
sudo useradd --system --home /opt/english-tutor-agent --shell /usr/sbin/nologin english-tutor
```

如果用户已存在，命令会提示已存在，可以忽略。

### 2.2 创建目录

```bash
sudo install -d -o english-tutor -g english-tutor /opt/english-tutor-agent
sudo install -d -o english-tutor -g english-tutor /opt/english-tutor-agent/releases
sudo install -d -o english-tutor -g english-tutor /opt/english-tutor-agent/shared
```

目录用途：

- `/opt/english-tutor-agent/releases/<release-id>/`：每次发布的后端 Jar；
- `/opt/english-tutor-agent/current`：指向当前版本的软链接；
- `/opt/english-tutor-agent/shared/production.env`：生产环境变量文件。

### 2.3 安装基础依赖

```bash
sudo apt-get update
sudo apt-get install -y openjdk-21-jre-headless curl ca-certificates rsync
```

如果你选择“在服务器上直接构建”，还需要 Git 和构建依赖：

```bash
sudo apt-get install -y git
```

确认 Java 版本：

```bash
java -version
```

应看到 Java 21，例如：

```text
openjdk version "21..."
```

## 3. 准备 MySQL

推荐使用云厂商托管 MySQL 或独立数据库服务器。后端要求 MySQL 8.x 兼容版本。

### 3.1 创建数据库和用户

在 MySQL 管理终端执行：

```sql
CREATE DATABASE english_tutor
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

CREATE USER 'english_tutor'@'%'
  IDENTIFIED BY '<DB_PASSWORD>';

GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES
  ON english_tutor.*
  TO 'english_tutor'@'%';

FLUSH PRIVILEGES;
```

说明：

- `<DB_PASSWORD>` 留空给你自己替换，不要写进仓库；
- Flyway 需要建表和迁移权限，所以至少需要 `CREATE`、`ALTER`、`INDEX`；
- 如果数据库和应用在同一台机器，可以把 `'%'` 改成 `'localhost'` 或具体内网 IP；
- 数据库时区建议统一按 UTC 处理，应用 JDBC URL 已带 UTC 相关参数。

### 3.2 从服务器测试连接

如果服务器安装了 MySQL 客户端：

```bash
sudo apt-get install -y mysql-client
mysql -h <DB_HOST> -P 3306 -u english_tutor -p english_tutor
```

登录后执行：

```sql
SELECT 1;
SHOW DATABASES;
```

确认能连接后输入：

```sql
exit;
```

## 4. 准备 Redis

推荐使用云厂商托管 Redis 或独立 Redis 7.x 服务。

### 4.1 Redis 配置要求

生产环境至少满足：

- Redis 不直接暴露公网；
- 设置密码；
- 应用服务器可以访问 Redis 内网地址；
- Redis 数据用于短期状态，不作为长期事实来源。

### 4.2 从服务器测试连接

安装 Redis 客户端：

```bash
sudo apt-get install -y redis-tools
```

如果 Redis 有密码：

```bash
redis-cli -h <REDIS_HOST> -p 6379 -a '<REDIS_PASSWORD>' ping
```

如果 Redis 暂无密码，仅限内网临时测试：

```bash
redis-cli -h <REDIS_HOST> -p 6379 ping
```

期望返回：

```text
PONG
```

> 生产环境不要使用无密码 Redis。

## 5. 准备 S3 兼容对象存储

后端设计要求音频和文件类资源进入对象存储，不写入数据库 BLOB。你可以使用 AWS S3、MinIO、七牛云 S3 兼容接口或其他兼容服务。

需要准备：

- Bucket 名称，例如 `english-tutor-prod`；
- Endpoint，例如 `https://s3.<region>.example.com`；
- Region，例如 `cn-east-1`、`ap-southeast-1`；
- Access Key；
- Secret Key；
- 可选的公开访问基础 URL，例如 CDN 域名。

权限建议：

- Bucket 默认私有；
- Access Key 只授予当前 Bucket 的必要读写权限；
- 不要把 Access Key 和 Secret Key 写入 Git、README、Issue 或日志。

## 6. 编写生产环境变量文件

在服务器上创建环境变量文件：

```bash
sudo nano /opt/english-tutor-agent/shared/production.env
```

写入下面内容。敏感值保持空位，由你在服务器上填写：

```bash
# =========================
# English Tutor Agent Backend
# Production environment
# =========================

# 应用基础信息
APP_DOMAIN=<APP_DOMAIN>
PUBLIC_BASE_URL=https://<APP_DOMAIN>
APP_ENV=production
APP_TIMEZONE=Asia/Shanghai

# 后端监听端口。不要直接开放公网，由 Nginx/网关反向代理。
SERVER_PORT=8080

# 如果你希望 Spring Boot 只绑定本机地址，可以保留这一项。
# 若你的网关与后端不在同一台机器，请改成内网监听地址或移除此项。
SERVER_ADDRESS=127.0.0.1

# MySQL
DB_HOST=<DB_HOST>
DB_PORT=3306
DB_NAME=english_tutor
DB_USERNAME=english_tutor
DB_PASSWORD=

# Flyway 前向迁移。生产默认开启。
FLYWAY_ENABLED=true

# Redis
REDIS_HOST=<REDIS_HOST>
REDIS_PORT=6379
REDIS_PASSWORD=
REDIS_TIMEOUT=2s

# S3 兼容对象存储
S3_ENDPOINT=<S3_ENDPOINT>
S3_REGION=<S3_REGION>
S3_BUCKET=<S3_BUCKET>
S3_ACCESS_KEY=
S3_SECRET_KEY=
S3_PUBLIC_BASE_URL=

# AI Provider
# 当前阶段生产试运行建议先使用 fake，等真实 Provider Adapter 完成并通过评审后再切换。
LLM_PROVIDER=fake
LLM_BASE_URL=
LLM_API_KEY=
LLM_MODEL=

ASR_PROVIDER=fake
ASR_BASE_URL=
ASR_API_KEY=
ASR_MODEL=

TTS_PROVIDER=fake
TTS_BASE_URL=
TTS_API_KEY=
TTS_MODEL=

# 隐私与保留策略
SAVE_RAW_TEXT_DEFAULT=true
SAVE_RAW_AUDIO_DEFAULT=false
RAW_AUDIO_RETENTION_DAYS=7
```

保存后设置权限：

```bash
sudo chown english-tutor:english-tutor /opt/english-tutor-agent/shared/production.env
sudo chmod 600 /opt/english-tutor-agent/shared/production.env
```

检查文件权限：

```bash
sudo ls -l /opt/english-tutor-agent/shared/production.env
```

期望类似：

```text
-rw------- 1 english-tutor english-tutor ... production.env
```

> 注意：如果密码中包含空格、`#`、`$`、引号等 shell 特殊字符，请使用安全的引号和转义方式。最稳妥的做法是生成不含 shell 特殊字符但足够长的随机密码。

## 7. 构建后端 Jar

后端可以在服务器上构建，也可以在本地构建后上传。生产推荐 CI 或本地干净环境构建，再上传 Jar 到服务器。

### 7.1 方式 A：在服务器上构建

在服务器上拉取代码：

```bash
cd /opt
sudo git clone <YOUR_GIT_REPOSITORY_URL> english-tutor-agent-source
sudo chown -R "$USER":"$USER" /opt/english-tutor-agent-source
cd /opt/english-tutor-agent-source
```

如果仓库已经存在：

```bash
cd /opt/english-tutor-agent-source
git pull
```

确保 Maven Wrapper 可执行：

```bash
chmod +x server/mvnw
```

运行后端测试并构建：

```bash
cd /opt/english-tutor-agent-source/server
./mvnw -pl tutor-bootstrap -am clean verify
```

构建成功后确认 Jar 存在：

```bash
ls -lh tutor-bootstrap/target/tutor-bootstrap-0.1.0-SNAPSHOT.jar
```

如果你必须跳过测试，仅用于紧急发布：

```bash
./mvnw -pl tutor-bootstrap -am -DskipTests package
```

> 正常发布不要跳过测试。跳过测试应记录原因，并在发布后补跑完整验证。

### 7.2 方式 B：本地构建后上传

在本地仓库根目录执行：

```bash
(cd server && ./mvnw -pl tutor-bootstrap -am clean verify)
```

上传 Jar 到服务器临时目录：

```bash
scp server/tutor-bootstrap/target/tutor-bootstrap-0.1.0-SNAPSHOT.jar <SSH_USER>@<SERVER_HOST>:/tmp/tutor-bootstrap.jar
```

登录服务器：

```bash
ssh <SSH_USER>@<SERVER_HOST>
```

确认上传成功：

```bash
ls -lh /tmp/tutor-bootstrap.jar
```

## 8. 安装一次后端 Release

以下命令在生产服务器执行。

### 8.1 创建 Release 目录

```bash
RELEASE_ID="$(date -u +%Y%m%dT%H%M%SZ)"
DEPLOY_ROOT="/opt/english-tutor-agent"
RELEASE_DIR="$DEPLOY_ROOT/releases/$RELEASE_ID"

sudo install -d -o english-tutor -g english-tutor "$RELEASE_DIR/server"
```

### 8.2 安装 Jar

如果你是在服务器上构建：

```bash
sudo install -m 0644 \
  /opt/english-tutor-agent-source/server/tutor-bootstrap/target/tutor-bootstrap-0.1.0-SNAPSHOT.jar \
  "$RELEASE_DIR/server/tutor-bootstrap.jar"
```

如果你是本地构建后上传：

```bash
sudo install -m 0644 /tmp/tutor-bootstrap.jar "$RELEASE_DIR/server/tutor-bootstrap.jar"
```

设置属主：

```bash
sudo chown -R english-tutor:english-tutor "$RELEASE_DIR"
```

切换当前版本软链接：

```bash
sudo ln -sfn "$RELEASE_DIR" "$DEPLOY_ROOT/current"
sudo chown -h english-tutor:english-tutor "$DEPLOY_ROOT/current"
```

检查当前版本：

```bash
readlink -f /opt/english-tutor-agent/current
ls -lh /opt/english-tutor-agent/current/server/tutor-bootstrap.jar
```

## 9. 配置 systemd 服务

创建 systemd unit：

```bash
sudo nano /etc/systemd/system/english-tutor-agent.service
```

写入：

```ini
[Unit]
Description=English Tutor Agent backend
After=network-online.target
Wants=network-online.target

[Service]
Type=simple
User=english-tutor
Group=english-tutor
WorkingDirectory=/opt/english-tutor-agent/current/server
EnvironmentFile=/opt/english-tutor-agent/shared/production.env
ExecStart=/usr/bin/java -jar /opt/english-tutor-agent/current/server/tutor-bootstrap.jar
SuccessExitStatus=143
Restart=on-failure
RestartSec=10
TimeoutStopSec=30
NoNewPrivileges=true
PrivateTmp=true
ProtectSystem=full
ProtectHome=true
ReadWritePaths=/opt/english-tutor-agent

[Install]
WantedBy=multi-user.target
```

加载并设置开机启动：

```bash
sudo systemctl daemon-reload
sudo systemctl enable english-tutor-agent
```

## 10. 启动后端

启动服务：

```bash
sudo systemctl restart english-tutor-agent
```

查看状态：

```bash
sudo systemctl status english-tutor-agent --no-pager
```

查看启动日志：

```bash
sudo journalctl -u english-tutor-agent -n 200 --no-pager
```

如果 Flyway 成功执行，日志中应能看到数据库迁移完成或无待执行迁移。若数据库连接、Redis 连接或环境变量有误，服务会启动失败，优先看 `journalctl` 输出。

## 11. 健康检查

在服务器上检查本机健康状态：

```bash
curl -fsS http://127.0.0.1:8080/actuator/health
```

期望返回类似：

```json
{"status":"UP"}
```

如果设置了 `SERVER_PORT` 为其他端口，请替换命令中的 `8080`。

检查端口监听：

```bash
ss -ltnp | grep 8080
```

如果设置了 `SERVER_ADDRESS=127.0.0.1`，应看到监听在本机地址，避免公网直连。

## 12. 可选：配置 Nginx 只代理后端 API

前端部署会单独写文档。这里仅给后端 API 代理示例，适合你想先验证后端域名访问。

安装 Nginx：

```bash
sudo apt-get install -y nginx
```

创建站点配置：

```bash
sudo nano /etc/nginx/sites-available/english-tutor-agent-backend.conf
```

写入：

```nginx
server {
    listen 80;
    server_name <APP_DOMAIN>;

    access_log /var/log/nginx/english-tutor-agent-backend.access.log;
    error_log /var/log/nginx/english-tutor-agent-backend.error.log;

    location /api/ {
        proxy_pass http://127.0.0.1:8080/api/;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_read_timeout 300s;
    }

    location /actuator/health {
        proxy_pass http://127.0.0.1:8080/actuator/health;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

启用配置：

```bash
sudo ln -sfn /etc/nginx/sites-available/english-tutor-agent-backend.conf /etc/nginx/sites-enabled/english-tutor-agent-backend.conf
sudo nginx -t
sudo systemctl reload nginx
```

公网检查：

```bash
curl -fsS http://<APP_DOMAIN>/actuator/health
```

生产建议配置 HTTPS。DNS 指向服务器后，可使用 Certbot：

```bash
sudo apt-get install -y certbot python3-certbot-nginx
sudo certbot --nginx -d <APP_DOMAIN>
```

HTTPS 检查：

```bash
curl -fsS https://<APP_DOMAIN>/actuator/health
```

## 13. 防火墙建议

如果使用 UFW：

```bash
sudo ufw allow OpenSSH
sudo ufw allow 'Nginx Full'
sudo ufw enable
sudo ufw status
```

不要开放后端端口：

```bash
sudo ufw deny 8080
```

如果后端只监听 `127.0.0.1`，公网本身无法直接访问 `8080`，但防火墙仍建议保留。

## 14. 常用运维命令

查看服务状态：

```bash
sudo systemctl status english-tutor-agent --no-pager
```

重启服务：

```bash
sudo systemctl restart english-tutor-agent
```

停止服务：

```bash
sudo systemctl stop english-tutor-agent
```

查看最近日志：

```bash
sudo journalctl -u english-tutor-agent -n 200 --no-pager
```

实时跟踪日志：

```bash
sudo journalctl -u english-tutor-agent -f
```

查看当前发布版本：

```bash
readlink -f /opt/english-tutor-agent/current
```

查看历史发布：

```bash
ls -1 /opt/english-tutor-agent/releases
```

## 15. 回滚

先列出历史版本：

```bash
ls -1 /opt/english-tutor-agent/releases
```

选择一个已知可用版本，例如 `<RELEASE_ID>`，切换软链接：

```bash
sudo ln -sfn /opt/english-tutor-agent/releases/<RELEASE_ID> /opt/english-tutor-agent/current
sudo chown -h english-tutor:english-tutor /opt/english-tutor-agent/current
sudo systemctl restart english-tutor-agent
```

回滚后检查：

```bash
sudo systemctl status english-tutor-agent --no-pager
curl -fsS http://127.0.0.1:8080/actuator/health
```

重要提醒：

- Flyway 是前向迁移；
- 回滚 Jar 不会自动回滚数据库结构；
- 涉及表结构或数据语义变更的发布，必须提前准备兼容策略和数据库备份。

## 16. 发布前检查清单

发布前逐项确认：

- [ ] Java 版本是 21；
- [ ] `production.env` 权限是 `600`；
- [ ] `DB_PASSWORD`、`REDIS_PASSWORD`、`S3_SECRET_KEY` 等敏感值没有进入 Git；
- [ ] MySQL 只能被应用服务器或可信内网访问；
- [ ] Redis 设置了密码，且不暴露公网；
- [ ] S3 Bucket 默认私有；
- [ ] `FLYWAY_ENABLED=true`；
- [ ] 后端端口没有直接暴露公网；
- [ ] `/actuator/health` 返回 `UP`；
- [ ] systemd 服务已设置开机启动；
- [ ] 已配置日志查看方式；
- [ ] 已准备数据库备份和回滚方案；
- [ ] 如果对公网开放，域名必须启用 HTTPS。

## 17. 故障排查

### 17.1 服务启动失败

查看状态和日志：

```bash
sudo systemctl status english-tutor-agent --no-pager
sudo journalctl -u english-tutor-agent -n 300 --no-pager
```

常见原因：

- Java 不是 21；
- `production.env` 缺少数据库或 Redis 配置；
- MySQL 用户没有建表/迁移权限；
- Redis 密码错误；
- Jar 路径不存在；
- `current` 软链接指向了错误目录。

### 17.2 数据库连接失败

在服务器上测试：

```bash
mysql -h <DB_HOST> -P 3306 -u english_tutor -p english_tutor
```

检查：

- 数据库安全组是否允许应用服务器访问；
- 用户名和密码是否正确；
- 数据库名是否为 `english_tutor`；
- MySQL 是否是 8.x 兼容版本；
- Flyway 用户是否有建表和改表权限。

### 17.3 Redis 连接失败

在服务器上测试：

```bash
redis-cli -h <REDIS_HOST> -p 6379 -a '<REDIS_PASSWORD>' ping
```

检查：

- Redis 地址和端口是否正确；
- 密码是否正确；
- 安全组/防火墙是否允许应用服务器访问；
- Redis 是否强制 TLS。如果强制 TLS，需要额外配置客户端连接参数，不能只填普通 `REDIS_HOST`。

### 17.4 健康检查不是 UP

执行：

```bash
curl -v http://127.0.0.1:8080/actuator/health
sudo journalctl -u english-tutor-agent -n 300 --no-pager
```

优先排查：

- 数据库连接；
- Redis 连接；
- Flyway 迁移；
- 端口是否被占用；
- 环境变量是否被 systemd 正确读取。

### 17.5 systemd 没有读取最新环境变量

修改 `production.env` 后重启服务：

```bash
sudo systemctl restart english-tutor-agent
```

如果修改了 service 文件本身，需要：

```bash
sudo systemctl daemon-reload
sudo systemctl restart english-tutor-agent
```

## 18. 最小可复制部署脚本

如果你已经在服务器上准备好了：

- `/opt/english-tutor-agent/shared/production.env`；
- Java 21；
- MySQL、Redis、S3；
- 已构建好的 `/tmp/tutor-bootstrap.jar`；

可以用下面脚本完成一次后端发布：

```bash
#!/usr/bin/env bash
set -euo pipefail

DEPLOY_ROOT="/opt/english-tutor-agent"
SERVICE_USER="english-tutor"
SOURCE_JAR="/tmp/tutor-bootstrap.jar"
RELEASE_ID="$(date -u +%Y%m%dT%H%M%SZ)"
RELEASE_DIR="$DEPLOY_ROOT/releases/$RELEASE_ID"

if [ ! -f "$SOURCE_JAR" ]; then
  echo "Jar 不存在：$SOURCE_JAR" >&2
  exit 1
fi

if [ ! -f "$DEPLOY_ROOT/shared/production.env" ]; then
  echo "环境变量文件不存在：$DEPLOY_ROOT/shared/production.env" >&2
  exit 1
fi

set -a
. "$DEPLOY_ROOT/shared/production.env"
set +a

if ! id "$SERVICE_USER" >/dev/null 2>&1; then
  sudo useradd --system --home "$DEPLOY_ROOT" --shell /usr/sbin/nologin "$SERVICE_USER"
fi

sudo install -d -o "$SERVICE_USER" -g "$SERVICE_USER" "$DEPLOY_ROOT" "$DEPLOY_ROOT/releases" "$DEPLOY_ROOT/shared"
sudo install -d -o "$SERVICE_USER" -g "$SERVICE_USER" "$RELEASE_DIR/server"
sudo install -m 0644 "$SOURCE_JAR" "$RELEASE_DIR/server/tutor-bootstrap.jar"
sudo chown -R "$SERVICE_USER:$SERVICE_USER" "$RELEASE_DIR"

sudo ln -sfn "$RELEASE_DIR" "$DEPLOY_ROOT/current"
sudo chown -h "$SERVICE_USER:$SERVICE_USER" "$DEPLOY_ROOT/current"

sudo tee /etc/systemd/system/english-tutor-agent.service >/dev/null <<'SERVICE'
[Unit]
Description=English Tutor Agent backend
After=network-online.target
Wants=network-online.target

[Service]
Type=simple
User=english-tutor
Group=english-tutor
WorkingDirectory=/opt/english-tutor-agent/current/server
EnvironmentFile=/opt/english-tutor-agent/shared/production.env
ExecStart=/usr/bin/java -jar /opt/english-tutor-agent/current/server/tutor-bootstrap.jar
SuccessExitStatus=143
Restart=on-failure
RestartSec=10
TimeoutStopSec=30
NoNewPrivileges=true
PrivateTmp=true
ProtectSystem=full
ProtectHome=true
ReadWritePaths=/opt/english-tutor-agent

[Install]
WantedBy=multi-user.target
SERVICE

sudo systemctl daemon-reload
sudo systemctl enable english-tutor-agent
sudo systemctl restart english-tutor-agent

for i in $(seq 1 30); do
  if curl -fsS "http://127.0.0.1:${SERVER_PORT:-8080}/actuator/health" >/dev/null; then
    echo "后端健康检查通过，发布完成：$RELEASE_ID"
    exit 0
  fi
  sleep 2
done

sudo journalctl -u english-tutor-agent -n 120 --no-pager >&2 || true
echo "后端健康检查失败，请查看日志" >&2
exit 1
```

> 这个脚本是文档内的最小后端发布脚本。仓库已有 `scripts/deploy/deploy_production.sh` 是前后端一体发布脚本，后续拆分前端文档时可以再决定是否新增正式的后端专用脚本文件。
