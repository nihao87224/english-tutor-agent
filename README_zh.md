# English Tutor Agent

> 面向刻意练习、分层反馈和自适应学习计划的开源 AI 英语表达教练。

<p>
  <img alt="Java 21" src="https://img.shields.io/badge/Java-21-007396?logo=openjdk&logoColor=white">
  <img alt="Spring Boot" src="https://img.shields.io/badge/Spring%20Boot-4.1-6DB33F?logo=springboot&logoColor=white">
  <img alt="React" src="https://img.shields.io/badge/React-19-61DAFB?logo=react&logoColor=111111">
  <img alt="TypeScript" src="https://img.shields.io/badge/TypeScript-5.7-3178C6?logo=typescript&logoColor=white">
  <img alt="Playwright" src="https://img.shields.io/badge/Playwright-E2E-2EAD33?logo=playwright&logoColor=white">
  <img alt="MySQL" src="https://img.shields.io/badge/MySQL-8.4-4479A1?logo=mysql&logoColor=white">
  <img alt="Redis" src="https://img.shields.io/badge/Redis-7.4-DC382D?logo=redis&logoColor=white">
</p>

[English](README.md)

English Tutor Agent 帮助学习者走出“一次性语法纠错”的局限。它把每天的英语练习整理成可重复的闭环：规划合适任务、先表达观点、获得聚焦反馈、再次尝试，并沉淀学习证据用于下一次计划。

## 链接

- 演示地址：`https://your-production-demo-url.example.com`
- 部署指南：[docs/deploy/PRODUCTION_DEPLOYMENT.md](docs/deploy/PRODUCTION_DEPLOYMENT.md)
- 产品需求：[docs/prd/ENGLISH_TUTOR_AGENT_PRD_v1.0.0.md](docs/prd/ENGLISH_TUTOR_AGENT_PRD_v1.0.0.md)
- 架构文档：[docs/design/00_README.md](docs/design/00_README.md)
- API 契约：[contracts/openapi/english-tutor-api.yaml](contracts/openapi/english-tutor-api.yaml)

> 生产环境部署完成后，请替换上面的演示地址。

## 截图

截图会在生产演示环境部署后补充。请将真实图片放到 `docs/assets/screenshots/`，并替换下方占位内容。

| Web 表达教练 | Android 应用 | 每日总结 |
|---|---|---|
| 待补充：`docs/assets/screenshots/web-expression-coach.png` | 待补充：`docs/assets/screenshots/android-home.png` | 待补充：`docs/assets/screenshots/daily-summary.png` |

建议替换格式：

```markdown
![Web Expression Coach](docs/assets/screenshots/web-expression-coach.png)
```

## 为什么做这个项目

很多语言学习工具和通用聊天机器人擅长回答单个问题，但不擅长支持持续成长：

- 它们会纠正一句话，却不记得下一次应该练什么。
- 它们容易过度纠错，打断学习者表达想法的意愿。
- 它们依赖难以测试、难以稳定上线的非确定性 AI 输出。
- 它们缺少围绕目标、偏好、练习证据和学习计划的长期模型。

English Tutor Agent 的设计中心不是聊天窗口，而是学习闭环。

## 亮点

- 表达优先：学习者先产出语言，再获得反馈。
- 分层纠错：语法问题、解释、自然表达和严重程度分开呈现。
- 再试一次闭环：反馈会回到改写或复述练习，而不是停在点评。
- 自适应每日计划：任务重点根据学习证据调整，而不是固定课程表。
- 结构化 AI 边界：Provider 输出先解析，再做 Schema 校验和业务校验。
- Fake Provider 优先：本地演示和测试不依赖付费模型密钥。
- 契约优先后端：OpenAPI、JSON Schema、Flyway 迁移和验证脚本保持实现对齐。
- 面向生产的基线：包含 Nginx、systemd、环境模板和部署脚本。

## 工作方式

```text
Learner
  -> Today Plan
  -> Web Expression Coach
  -> Streaming AI Reply
  -> Layered Correction
  -> Try Again
  -> Learning Evidence
  -> Daily Summary
  -> Next Plan
```

关键设计选择是职责分离：

- 确定性规则负责学习优先级、复习到期、状态流转和持久化；
- AI Provider 负责受约束的教练内容生成、分析和语言反馈；
- 具体供应商 SDK 隔离在项目自有接口之后；
- 测试默认使用 Fake Provider 和固定响应。

## 架构

```text
english-tutor-agent/
|-- web/                  React + TypeScript Web Expression Coach
|-- android/              Android Compose baseline
|-- server/               Java 21 + Spring Boot modular monolith
|-- contracts/            OpenAPI, JSON Schema, examples
|-- docs/                 PRD, design, ADR, process, deployment
|-- scripts/              validation, infrastructure, deployment scripts
`-- infrastructure/       local infrastructure notes
```

后端模块：

```text
server/
|-- tutor-domain          domain rules, aggregates, value objects
|-- tutor-application     use-case orchestration
|-- tutor-api             HTTP controllers, DTOs, SSE mapping
|-- tutor-agent           AI provider abstraction and fake providers
|-- tutor-infrastructure  JDBC repositories, cache, storage adapters
|-- tutor-observability   tracing, metrics, audit extension points
`-- tutor-bootstrap       Spring Boot entrypoint and Flyway migrations
```

运行时视图：

```text
Browser / Android
      |
      v
Spring Boot API  -----> AI Provider Interface -----> Fake or real provider
      |
      +-------------> MySQL + Flyway
      +-------------> Redis
      +-------------> S3-compatible object storage
```

## 快速开始

### 前置依赖

- Java 21
- Node.js + pnpm
- Docker 和 Docker Compose
- Python 3.11+，用于验证脚本

### 启动基础设施

```bash
cp .env.example .env
docker compose --env-file .env up -d
```

本地服务：

- MySQL：`localhost:3306`
- Redis：`localhost:6379`
- MinIO：`localhost:9000`

### 运行后端

```powershell
cd server
.\mvnw.cmd clean verify
.\mvnw.cmd -pl tutor-bootstrap spring-boot:run
```

健康检查：

```text
GET http://localhost:8080/actuator/health
```

### 运行 Web 应用

```powershell
cd web
pnpm install
pnpm run dev
```

Web 应用默认使用 `http://localhost:8080` 作为 API 地址。如需切换到其他 API 源：

```powershell
$env:VITE_API_BASE_URL="https://your-api.example.com"
pnpm run dev
```

## 测试

```powershell
# 项目结构、YAML、OpenAPI、JSON Schema 和示例
python scripts\validate_project.py

# 后端
cd server
.\mvnw.cmd clean verify

# Web
cd web
pnpm test
pnpm run build
pnpm run e2e
```

## 部署

仓库包含单服务器生产部署基线：

- Nginx 托管 Web 静态构建产物。
- Nginx 将 `/api/` 和 `/actuator/` 反向代理到 Spring Boot。
- systemd 管理后端进程。
- MySQL、Redis、对象存储和 Provider 密钥通过环境变量提供。

从这里开始：

- [生产部署指南](docs/deploy/PRODUCTION_DEPLOYMENT.md)
- [环境变量模板](scripts/deploy/production.env.example)
- [Linux 部署脚本](scripts/deploy/deploy_production.sh)
- [Windows 到 Linux 部署脚本](scripts/deploy/deploy_production.ps1)

## 路线图

- 语音练习：Android 录音、上传、ASR、TTS、低置信度确认。
- 长期学习：复习调度、掌握状态、周报。
- IELTS Speaking：Part 1/2/3 练习、完整模拟、基于评分标准的练习反馈。
- 发布加固：隐私删除、监控、成本指标、发布和回滚手册。

## 安全与隐私

- 不提交 API keys、tokens、真实用户录音或私人学习数据。
- 不记录完整授权头、密钥或不必要的用户原文。
- 时间统一使用 UTC 存储，展示给用户时再转换时区。
- 数据库变更使用 Flyway 前向迁移。
- IELTS 评分必须明确标记为练习反馈，不得声称为官方成绩。

## 文档

- [PRD](docs/prd/ENGLISH_TUTOR_AGENT_PRD_v1.0.0.md)
- [设计索引](docs/design/00_README.md)
- [当前任务](docs/plans/CURRENT_TASK.md)
- [任务 backlog](docs/plans/TASK_BACKLOG.md)
- [Definition of Done](docs/process/DEFINITION_OF_DONE.md)
- [生产部署](docs/deploy/PRODUCTION_DEPLOYMENT.md)

## License

本项目基于 [MIT License](LICENSE) 开源。
