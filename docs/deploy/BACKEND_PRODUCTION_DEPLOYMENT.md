# 后端 Docker + Jenkins 生产部署指南

本文档说明如何用 Jenkins 构建后端 Docker 镜像，并通过 Docker Compose 在 VPS 上部署 English Tutor Agent 后端。前端 Web 静态资源和 Android 发布不在本文档范围内。

## 1. 部署文件

仓库内与后端容器化部署相关的文件：

```text
Jenkinsfile
server/Dockerfile
.dockerignore
scripts/deploy/docker-compose.backend.yml
scripts/deploy/deploy_backend_container_with_jenkins.sh
scripts/deploy/rollback_backend_container.sh
scripts/deploy/production.env.example
docs/deploy/BACKEND_PRODUCTION_DEPLOYMENT.md
```

职责说明：

- `Jenkinsfile`：Jenkins Pipeline 单一事实来源。负责后端测试、构建 Jar、构建 Docker 镜像、调用受控部署脚本。
- `server/Dockerfile`：后端运行镜像定义。镜像只包含 Java 运行时、应用 Jar 和健康检查需要的 `curl`，不包含生产密钥。
- `.dockerignore`：限制 Docker build context，避免把 `.git`、本地数据、密钥和无关构建产物带进镜像上下文。
- `scripts/deploy/docker-compose.backend.yml`：生产后端 Compose 文件，只启动后端容器。
- `scripts/deploy/deploy_backend_container_with_jenkins.sh`：受控发布脚本。切换到指定镜像、执行 `docker compose up -d`、健康检查，失败自动回滚到上一镜像。
- `scripts/deploy/rollback_backend_container.sh`：手工回滚脚本。支持回滚到上一个 release、指定 release 或指定镜像。
- `scripts/deploy/production.env.example`：生产环境变量模板，真实值只放 VPS。

推荐拓扑：

```text
Gitee/Git 仓库
   |
   | Jenkins 拉代码并执行 Jenkinsfile
   v
Jenkins on VPS
   |
   | mvn verify + docker build
   v
本地 Docker 镜像 english-tutor-agent-backend:<release-id>
   |
   | sudo 调用 root-owned 发布脚本
   v
Docker Compose backend service
   |
   v
Spring Boot 后端容器 127.0.0.1:8080
```

> 安全原则：镜像是可复现交付物，但生产密钥不进镜像、不进 Git。Jenkins 不应获得全量免密 sudo；它只允许执行固定路径的受控发布/回滚脚本。

## 2. VPS 前置准备

以下命令在 VPS 上执行。

### 2.1 安装 Java、Git 和 Docker

Jenkins 需要 Java 21 执行 Maven 构建，也需要 Docker 构建和运行镜像。

```bash
sudo apt-get update
sudo apt-get install -y openjdk-21-jdk git curl ca-certificates
```

安装 Docker Engine 和 Compose plugin。可以按 Docker 官方文档安装；Ubuntu 常见安装完成后确认：

```bash
docker version
docker compose version
```

确认 Jenkins 用户也能使用 Java 21：

```bash
sudo -u jenkins java -version
```

### 2.2 Jenkins 的 Docker 权限

最简单方式是把 Jenkins 用户加入 `docker` 组：

```bash
sudo usermod -aG docker jenkins
sudo systemctl restart jenkins
```

然后测试：

```bash
sudo -u jenkins docker version
sudo -u jenkins docker compose version
```

安全提醒：能访问 Docker daemon 基本等同于 root 权限。生产上要确保 Jenkins 只对可信管理员开放，Job 只从受保护分支发布。

### 2.3 创建部署目录

```bash
sudo install -d -o root -g root /opt/english-tutor-agent
sudo install -d -o root -g root /opt/english-tutor-agent/bin
sudo install -d -o root -g root /opt/english-tutor-agent/shared
sudo install -d -o root -g root /opt/english-tutor-agent/releases
```

目录用途：

- `/opt/english-tutor-agent/bin/`：root-owned 发布/回滚脚本。
- `/opt/english-tutor-agent/shared/production.env`：生产环境变量。
- `/opt/english-tutor-agent/shared/docker-compose.backend.yml`：root-owned Compose 文件。
- `/opt/english-tutor-agent/shared/current-image.env`：当前后端镜像，由发布脚本维护。
- `/opt/english-tutor-agent/releases/<release-id>/`：每次发布的镜像元数据。

## 3. 安装受控脚本和 Compose 文件

从仓库工作区执行：

```bash
cd <REPO_WORKSPACE>

sudo install -m 0755 -o root -g root \
  scripts/deploy/deploy_backend_container_with_jenkins.sh \
  /opt/english-tutor-agent/bin/deploy_backend_container_with_jenkins.sh

sudo install -m 0755 -o root -g root \
  scripts/deploy/rollback_backend_container.sh \
  /opt/english-tutor-agent/bin/rollback_backend_container.sh

sudo install -m 0644 -o root -g root \
  scripts/deploy/docker-compose.backend.yml \
  /opt/english-tutor-agent/shared/docker-compose.backend.yml
```

确认：

```bash
sudo ls -l /opt/english-tutor-agent/bin/
sudo ls -l /opt/english-tutor-agent/shared/docker-compose.backend.yml
```

以后如果这些脚本或 Compose 文件更新，需要管理员重新执行上面的 `install` 命令。不要让 Jenkins 从可写工作区直接 `sudo` 执行部署脚本。

## 4. 生产环境变量

Jenkins 只负责构建镜像和发布容器，不负责创建 MySQL、Redis 或对象存储。发布前需要先准备好：

- MySQL 8.x 兼容数据库和 `english_tutor` 应用用户；
- Redis 7.x 服务，建议设置密码且只允许内网访问；
- S3 兼容对象存储 Bucket，默认私有，Access Key 只授予必要权限。

创建环境变量文件：

```bash
sudo cp scripts/deploy/production.env.example /opt/english-tutor-agent/shared/production.env
sudo nano /opt/english-tutor-agent/shared/production.env
```

至少确认这些值：

```bash
APP_ENV=production
SERVER_PORT=8080
BACKEND_HOST_PORT=8080
DB_HOST=<DB_HOST>
DB_USERNAME=english_tutor
DB_PASSWORD=
REDIS_HOST=<REDIS_HOST>
REDIS_PASSWORD=
S3_ENDPOINT=<S3_ENDPOINT>
S3_BUCKET=<S3_BUCKET>
S3_ACCESS_KEY=
S3_SECRET_KEY=
```

设置权限：

```bash
sudo chmod 600 /opt/english-tutor-agent/shared/production.env
sudo chown root:root /opt/english-tutor-agent/shared/production.env
```

敏感值不得写入 Git、README、Issue、Jenkins Console Output 或聊天记录。

## 5. Jenkins sudoers

Jenkins 只需要 sudo 执行两个固定脚本：

```bash
sudo visudo -f /etc/sudoers.d/english-tutor-agent-jenkins
```

写入：

```sudoers
jenkins ALL=(root) NOPASSWD: /opt/english-tutor-agent/bin/deploy_backend_container_with_jenkins.sh, /opt/english-tutor-agent/bin/rollback_backend_container.sh
```

验证：

```bash
sudo visudo -cf /etc/sudoers.d/english-tutor-agent-jenkins
sudo -u jenkins sudo -n /opt/english-tutor-agent/bin/deploy_backend_container_with_jenkins.sh --help
sudo -u jenkins sudo -n /opt/english-tutor-agent/bin/rollback_backend_container.sh --help
```

不要给 Jenkins 配置全量免密 sudo，也不要给 Jenkins 开放通用删除权限。

## 6. Jenkins Job 配置

推荐使用 `Pipeline script from SCM`：

1. Jenkins 首页点击 `New Item`。
2. 名称：`english-tutor-agent-backend-prod`。
3. 类型选择 `Pipeline`。
4. `Definition` 选择 `Pipeline script from SCM`。
5. SCM 选择 Git。
6. Repository URL 填仓库地址，例如 `git@gitee.com:flyPanda/english-tutor-agent.git`。
7. Credentials 选择你的 Git 凭据。
8. Branch Specifier 填 `*/main`。
9. Script Path 填 `Jenkinsfile`。

`Jenkinsfile` 中的生产路径固定为：

```text
DEPLOY_ROOT=/opt/english-tutor-agent
DEPLOY_SCRIPT=/opt/english-tutor-agent/bin/deploy_backend_container_with_jenkins.sh
BACKEND_IMAGE_REPOSITORY=english-tutor-agent-backend
```

可用参数：

| 参数名 | 默认值 | 说明 |
| --- | --- | --- |
| `SKIP_TESTS` | `false` | 紧急发布才允许跳过测试 |

## 7. 首次发布

在 Jenkins Job 页面点击 `Build with Parameters`：

1. `SKIP_TESTS` 保持 `false`。
2. 点击 `Build`。

Jenkins 会执行：

```text
mvn clean verify
→ docker build -f server/Dockerfile
→ sudo /opt/english-tutor-agent/bin/deploy_backend_container_with_jenkins.sh
→ docker compose up -d backend
→ /actuator/health 健康检查
```

构建成功后，在 VPS 上检查：

```bash
docker ps --filter name=english-tutor-backend
docker compose \
  --env-file /opt/english-tutor-agent/shared/production.env \
  --env-file /opt/english-tutor-agent/shared/current-image.env \
  -f /opt/english-tutor-agent/shared/docker-compose.backend.yml \
  ps
curl -fsS http://127.0.0.1:8080/actuator/health
```

## 8. 日常发布

默认流程：

```text
push/merge 到 main
→ Jenkins Build with Parameters
→ 测试通过
→ 构建新镜像 english-tutor-agent-backend:<release-id>
→ Compose 切换后端容器
→ 健康检查通过
```

建议先手工点击发布。确认流程稳定后，再配置 Gitee WebHook 自动触发。即使启用 WebHook，也建议只允许 `main` 或受保护 tag 触发生产发布。

## 9. 回滚

发布脚本健康检查失败时，会自动把 Compose 切回上一镜像。

手工回滚到上一个 release：

```bash
sudo /opt/english-tutor-agent/bin/rollback_backend_container.sh --previous
```

手工回滚到指定 release：

```bash
ls -1dt /opt/english-tutor-agent/releases/*
sudo /opt/english-tutor-agent/bin/rollback_backend_container.sh --release-id <RELEASE_ID>
```

手工回滚到指定本地镜像：

```bash
sudo /opt/english-tutor-agent/bin/rollback_backend_container.sh --image english-tutor-agent-backend:<TAG>
```

重要提醒：

- Flyway 是前向迁移。
- 回滚镜像不会自动回滚数据库结构。
- 涉及表结构或数据语义变更的发布，必须提前准备兼容策略和数据库备份。

## 10. Nginx 后端代理

后端容器只映射到本机 `127.0.0.1:${BACKEND_HOST_PORT:-8080}`，公网通过 Nginx 或网关访问。

Compose 会让 Spring Boot 在容器内监听 `0.0.0.0:8080`，但宿主机只映射到 `127.0.0.1`，避免后端端口直接暴露公网。

```nginx
server {
    listen 80;
    server_name <APP_DOMAIN>;

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

生产建议启用 HTTPS。

## 11. 安全检查清单

- [ ] Jenkins 不允许匿名访问。
- [ ] Jenkins 管理页面使用 HTTPS，最好只允许 VPN、内网或固定 IP 访问。
- [ ] Jenkins Job 只从 `main` 或受保护 tag 发布生产。
- [ ] Jenkins 访问 Docker daemon 的风险已接受，并限制 Jenkins 管理权限。
- [ ] Jenkins 没有全量免密 sudo。
- [ ] Jenkins sudoers 只允许执行两个固定 root-owned 脚本。
- [ ] `/opt/english-tutor-agent/bin/*.sh` 属主为 `root root`，Jenkins 不能修改。
- [ ] `/opt/english-tutor-agent/shared/docker-compose.backend.yml` 属主为 `root root`，Jenkins 不能修改。
- [ ] `production.env` 权限为 `600`。
- [ ] 真实密钥没有进入 Git、镜像层或 Jenkins Console Output。
- [ ] `.dockerignore` 排除了 `.git`、本地数据、密钥和无关构建产物。
- [ ] Docker 镜像内使用非 root 用户运行应用。
- [ ] 后端端口只绑定 `127.0.0.1`，不直接暴露公网。
- [ ] 发布前已准备数据库备份和回滚策略。

## 12. 常用命令

查看容器：

```bash
docker ps --filter name=english-tutor-backend
```

查看日志：

```bash
docker logs --tail=200 english-tutor-backend
docker logs -f english-tutor-backend
```

查看当前镜像：

```bash
cat /opt/english-tutor-agent/shared/current-image.env
cat /opt/english-tutor-agent/shared/current-release
```

查看历史 release：

```bash
ls -1dt /opt/english-tutor-agent/releases/*
```

清理旧镜像和旧 release 由管理员手工执行：

```bash
docker image ls english-tutor-agent-backend
sudo rm -r -- /opt/english-tutor-agent/releases/<OLD_RELEASE_ID>
docker image rm english-tutor-agent-backend:<OLD_TAG>
```

## 13. 故障排查

### 13.1 Jenkins 构建失败

优先看 Jenkins Console Output。

常见原因：

- Jenkins 使用的 Java 不是 21。
- Jenkins 无法访问 Gitee/Git 仓库。
- Maven 下载依赖失败。
- 后端测试失败。
- Jenkins 用户不能访问 Docker daemon。

排查：

```bash
sudo -u jenkins java -version
sudo -u jenkins git ls-remote <GIT_REPOSITORY_URL>
sudo -u jenkins docker version
```

### 13.2 Docker 构建失败

检查 Jar 是否存在：

```bash
ls -lh server/tutor-bootstrap/target/tutor-bootstrap-0.1.0-SNAPSHOT.jar
```

检查 `.dockerignore` 是否误排除了 Jar。当前配置保留：

```text
!server/tutor-bootstrap/target/tutor-bootstrap-0.1.0-SNAPSHOT.jar
```

### 13.3 Jenkins sudo 权限失败

```bash
sudo visudo -cf /etc/sudoers.d/english-tutor-agent-jenkins
sudo -u jenkins sudo -n /opt/english-tutor-agent/bin/deploy_backend_container_with_jenkins.sh --help
```

### 13.4 容器启动或健康检查失败

```bash
docker ps -a --filter name=english-tutor-backend
docker logs --tail=300 english-tutor-backend
curl -v http://127.0.0.1:8080/actuator/health
```

优先排查：

- `production.env` 缺少数据库或 Redis 配置。
- MySQL 用户没有迁移权限。
- Redis 密码错误。
- 容器内 `SERVER_PORT` 是否为 `8080`。
- `BACKEND_HOST_PORT` 是否和 Nginx 代理端口一致。
