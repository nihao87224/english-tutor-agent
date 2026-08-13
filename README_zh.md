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

因为很多英语学习 App 解决的是“我今天学点什么”，但没有真正解决“我怎么把英语说出来”。

很多学习者其实不是零基础。听 ESL Podcast、看英文解释、读英文资料时，大意都能懂；看到 `I was supposed to meet a friend, but something came up.` 也知道是什么意思。可轮到自己表达同样的意思，脑子里常常只剩下中文：我要怎么说“本来应该”？“临时有事”是不是 `have a thing`？最后不是卡住，就是拼出一句别人能猜懂、但自己也知道不自然的英文。

这就是最难受的地方：输入能力明明不差，输出能力却总是掉链子。

市面上常见工具大多卡在几个点上：

| 常见英语 App 做什么 | 用户真正缺什么 |
|---|---|
| 背单词、刷课程、打卡 | 把认识的词变成自己能说出口的词 |
| 给一句标准答案或翻译 | 解释为什么这样说更自然，并带你再说一遍 |
| AI 陪聊，话题一直往下走 | 记住你总犯什么错，下次专门帮你练 |
| 语法批改器指出错误 | 不打击表达欲，只挑最值得改的问题 |
| 固定课程路径 | 根据你最近的表现决定今天该练什么 |

所以这个项目不是想再做一个“陪你聊天的 AI”。它更像一个耐心的表达教练：听你先说，帮你把中式表达改成自然英语，再带你马上练一遍，直到那些原来只会“看懂”的词块和句型，慢慢变成你能主动用出来的表达。

## 亮点

- 先让你表达，再给反馈：不是先塞一堆语法规则，而是让你用自己的话说出来。说完再看哪里卡住、哪里不自然。
- 不只告诉你“错了”，还告诉你“怎么说更像英语”：例如 `My work is very busy` 会被引导成 `I've been really busy with work lately.`，同时解释为什么英语里更常这样组织。
- 纠错不打断表达欲：自由聊天时不会每句话都拉出来批改，只抓最影响理解、最值得改的点，让你还能继续说下去。
- 有 Try Again：看到修改建议后，不是点个“知道了”就结束，而是马上改写、复述或换个场景再用一次。
- 记住你的高频问题：如果你经常说 `I very like...`，后面会反复设计小练习，帮你把 `really like / really enjoy` 变成下意识能用的表达。
- 练的是“主动调用”，不是“看懂答案”：目标是把 passive vocabulary 和 passive grammar 变成 active vocabulary、language chunks 和真实句子组织能力。
- 每天少做选择：打开后直接看到今天最值得练什么，以及为什么练这个，不需要在一堆课程、难度和题型里自己挑。
- 面向真实场景：职场汇报、解释问题、表达观点、日常交流、IELTS 口语，都围绕“你真的要说什么”来训练。

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
