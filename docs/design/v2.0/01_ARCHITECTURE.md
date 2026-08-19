# English Tutor Agent V2.0 总体架构设计

> 文档版本：`2.0.1`
> 状态：`V2.0 架构设计基线`
> 日期：`2026-08-19`
> 上游需求：`docs/prd/v2.0/ENGLISH_TUTOR_AGENT_PRD_v2.0.0.md`
> 适用范围：V2.0 Web-first 产品、后端模块化单体、离线内容生产，以及后续 Android 接入

---

## 1. 文档目的与审阅结论

本文把 V2.0 PRD 和设计文档中的产品约束转换为可实施、可测试的系统架构。它定义系统边界、模块职责、依赖方向、关键链路、数据所有权、AI 边界、部署形态与质量门槛；具体 REST 字段、数据库 DDL、JSON Schema 和 Prompt 全文仍由后续专项契约冻结。

本次架构审阅覆盖：

- V2.0 PRD；
- `docs/design/v2.0/` 下的角色、双图谱、资源、课程流、推荐、Evidence 和内容生产设计；
- ADR-0001 至 ADR-0016 中仍有效的架构决策；
- 当前 `server` Maven 模块、Web、Android、OpenAPI、JSON Schema、Flyway 和部署基线。

审阅结论：

1. V2.0 是 V1.x 学习闭环的增量，不重写既有评估、计划、训练、纠错、Evidence、权限和 Provider 基础；
2. 现有八个 Maven 模块足以承载 V2.0，P0 不新增微服务，也不因业务能力增加而立即新增 Maven module；
3. `Personalized Tutor Core` 是教学决策核心，`Lin Muen Immersive Experience` 是体验核心；两者通过处方和 Episode Mapping 单向衔接；
4. Capability Graph 与 Experience Graph 必须分离，Season / Episode 不是课程顺序；
5. 核心状态转换由确定性规则负责，LLM 只承担受约束的语言生成、分析、反馈和解释；
6. 教材媒体离线生产并发布到 Resource Catalog 与对象存储，课程首屏不依赖运行时模型；
7. 权限过滤发生在排序之前，计划生成、课程开始和私有媒体访问均需后端校验；
8. Web 是 V2.0 P0 主客户端；Android 继续遵循 Compose/UDF 和服务端事实源约束，作为后续兼容客户端；
9. 未发现 PRD、已接受设计与 ADR 之间需要停止架构设计的冲突。

### 1.1 当前实现与目标设计的标记

本文中的能力使用以下状态理解：

| 标记 | 含义 |
|---|---|
| `REUSE` | 仓库已有基础能力，V2.0 复用或增量扩展 |
| `V2-P0` | V2.0 必须新增或完成的能力 |
| `LATER` | P1/P2 或后续 Android/多模态演进 |

架构文档描述目标边界，不代表所有 `V2-P0` 能力已经实现。

---

## 2. 架构目标与质量属性

### 2.1 业务目标

系统必须同时兑现：

- 专业私教：基于 Goal、Skill State、Error Memory、Evidence、Review State、临时诉求和可用时间，生成可解释的 Daily Learning Prescription；
- 沉浸体验：把处方映射到 Lin Muen 的真实场景、任务、对话、图片和音频，促使用户完成主动输出；
- 学习闭环：输入、理解、Speaking、Role Play、Correction、Retry、Evidence、Memory、Review 和下一次处方可连续追踪；
- 规模化内容：从 72 个首发 Skill Unit Variant 扩展到 300、1000+ 资源时不推翻模型；
- 私有资源隔离：EngFluent 等 Private Collection 不污染公共资源和默认推荐。

### 2.2 关键质量属性

| 属性 | 架构要求 |
|---|---|
| 可解释性 | 保存处方输入摘要、规则版本、候选过滤原因、排序因子和最终选择原因 |
| 可测试性 | 同一状态与规则版本产生可重复的核心决策；模型调用可由 stub/mock 替代 |
| 一致性 | 服务端 MySQL 是长期学习事实源；Completion 不直接等于 Mastery |
| 安全性 | CurrentActor、资源归属和 Entitlement 均由后端强制校验；私有媒体不可依赖隐藏 URL |
| 可恢复性 | AI 或媒体失败不丢用户回答、Attempt 和已完成进度；可重试分析和替换未开始计划 |
| 性能与成本 | 课程首屏零 Token；图片和正式音频预生成并可缓存；实时模型只用于必要交互 |
| 可维护性 | 领域不依赖 Web、数据库或 Provider SDK；Resource Type、Provider、Access Scope 可扩展 |
| 可观测性 | 推荐、权限过滤、内容版本、模型调用、课程加载和 Evidence 更新可关联追踪 |

### 2.3 不在本架构中冻结

以下内容需要专项设计或机器契约，不在本文中擅自确定：

- 具体表名、字段、索引和 Flyway 版本号；
- REST/OpenAPI 请求与响应字段；
- 每个 JSON Schema 的完整定义；
- 推荐权重、Mastery 阈值、Spacing 公式和超时数值；
- 对象存储/CDN 厂商和生产拓扑；
- LLM、ASR、TTS 的长期最终厂商；
- Prompt 全文和模型参数；
- EngFluent 内容的法律授权结论。

---

## 3. 架构原则与不可破坏约束

### 3.1 双核心控制权

```text
Learner Model + Pedagogical Policy
                |
                v
      Daily Learning Prescription
                |
                v
        Skill Unit Variant
                |
                v
         Episode Mapping
                |
                v
 Lin Muen Story / Mission / Media / Role Play
                |
                v
       Validated Learning Evidence
                |
                +--------> next prescription
```

控制权规则：

- Planner 先决定训练目标、难度、支架、Training Type、时间预算和预期 Evidence；
- Experience 只能在处方边界内选择场景和调整表达方式；
- Lin Muen 或剧情不得更换 target skill、跳过前置条件、改变 Mastery 或推进未完成 Retry；
- 找不到合适 Episode Mapping 时，回退其他 Episode 或通用 Scenario Lesson，不降低教学匹配。

### 3.2 确定性规则与 AI 分工

确定性规则负责：

- Prerequisite、Mastery、Review due、Difficulty、Interleaving、Transfer、Retry；
- Access、Publish status、资源适配、完成状态和幂等；
- AI 结果的 Schema 与业务校验；
- Evidence 对 Skill State 的确定性影响。

AI 负责：

- 受约束的 Role Play 和对话变化；
- 用户回答的语言分析；
- Correction、Natural Expression、鼓励和推荐解释；
- 临时诉求下的受约束内容变化；
- 离线课程草稿和审核辅助。

任何参与业务状态更新的 AI 结果必须经过：

```text
Provider Response
 -> Parse
 -> JSON Schema Validation
 -> Business Validation
 -> Rule-owned State Transition
```

### 3.3 服务端事实源

- MySQL 保存长期学习、资源、权限、处方、证据和审计事实；
- Redis 只用于缓存、短期状态、幂等键、锁和限时数据；
- 对象存储保存媒体和用户录音，不保存于 MySQL BLOB；
- Web 本地状态和后续 Android Room/DataStore 只能缓存或排队，不能覆盖服务端确认事实；
- 所有持久化时间使用 UTC，展示时按用户时区转换。

---

## 4. 系统上下文

```text
                                 +----------------------+
                                 | Content Producer /   |
                                 | Reviewer / Admin     |
                                 +----------+-----------+
                                            |
                                            | import, validate,
                                            | publish, grant/revoke
                                            v
+---------------+      HTTPS/SSE     +------+-----------------------+
| Learner       | <----------------> | English Tutor Agent V2.0    |
| Web (P0)      |                    | Backend Modular Monolith     |
+---------------+                    +------+-----------------------+
                                            |
+---------------+                           | provider ports
| Android       | <-------------------------+-----------------------+
| (compatible,  |                           |                       |
| later scope)  |                           v                       v
+---------------+                    +------+-------+       +-------+-------+
                                     | LLM / ASR /  |       | Object Store |
                                     | TTS Providers|       | / CDN        |
                                     +--------------+       +---------------+
```

### 4.1 外部角色

| 角色 | 主要能力 |
|---|---|
| Learner | 今日学习、Scenario Lesson、AI 对话、随心学、进度与能力画像 |
| Administrator | 用户与角色、配额、Provider 配置、Resource、Collection、Entitlement 和审计 |
| Content Producer / Reviewer | 生成、校验、抽检、打包、导入和发布课程资源 |

### 4.2 外部系统

| 系统 | 边界 |
|---|---|
| LLM | 受约束文本生成、分析、流式对话和结构化评价 |
| ASR | PTT 录音转写，低置信度结果需允许确认或重录 |
| TTS | 运行时反馈语音或离线正式课程音频；通过项目自有端口访问 |
| Object Storage / CDN | 图片、正式音频、视频、用户录音和大型附件 |
| Content Staging Workspace | Git 外或本地 `resources/learning-content/v2.0/` 生产区，不是运行时事实源 |

---

## 5. 容器与部署视图

```text
+------------------+       +------------------------+
| React + Vite Web | ----> | Spring Boot App        |
| static delivery  | HTTPS | modular monolith       |
+------------------+  SSE  | /api/v1 + actuator    |
                           +---+-------+-------+----+
                               |       |       |
                       JDBC    |       |       | S3 API
                               v       v       v
                         +-----+--+ +--+---+ +---------+
                         | MySQL | | Redis| | Object  |
                         | 8.x   | |      | | Storage |
                         +-------+ +------+ +---------+
                               \       |       /
                                \      |      /
                                 +------v-----+
                                 | External AI|
                                 | Providers  |
                                 +------------+
```

### 5.1 部署单元

| 部署单元 | 职责 | 状态 |
|---|---|---|
| Web | V2.0 P0 学习端与现有管理端；不直接访问数据库、对象存储私有对象或 AI Provider | `REUSE + V2-P0` |
| Backend | 唯一业务 API、SSE、鉴权、规则、编排和审计入口 | `REUSE + V2-P0` |
| MySQL | 长期业务事实和 Flyway 前向迁移 | `REUSE` |
| Redis | 缓存、幂等、锁、流式/短期会话状态；不可作为长期事实源 | `REUSE` |
| Object Storage | 课程媒体、私有视频和用户录音；数据库保存 metadata/key/hash | `REUSE + V2-P0` |
| Content Pipeline | 离线脚本/CI/人工审核流程；产物经导入接口进入运行时 | `V2-P0` |

P0 不引入消息队列、向量数据库或独立内容微服务。需要延迟重试时，优先使用 MySQL 持久化任务/事件加同进程 worker；只有出现独立扩容或可靠性证据后才评估外部队列。

---

## 6. 后端模块与依赖架构

### 6.1 复用现有 Maven 模块

| Maven module | V2.0 职责 |
|---|---|
| `tutor-domain` | 纯 Java 领域模型与确定性规则：学习者、Skill、资源、权限、处方、体验、训练、Evidence、Review |
| `tutor-application` | 用例编排、事务边界、Repository/Provider/ObjectStorage/Clock 等端口 |
| `tutor-api` | REST/SSE DTO、Controller、CurrentActor 解析和协议错误映射 |
| `tutor-agent` | LLM/ASR/TTS 适配器、Prompt 版本、Provider 协议和结构化响应解析 |
| `tutor-infrastructure` | JDBC、Redis、对象存储、加密、持久化任务和外部资源实现 |
| `tutor-observability` | Metrics、trace、健康检查和脱敏观测配置 |
| `tutor-test-support` | 固定时钟、stub Provider、fixtures、容器和契约测试辅助 |
| `tutor-bootstrap` | Spring 组合根、配置、安全、Flyway 和可执行应用 |

V2.0 业务边界优先体现为 package 与应用端口，不把每个领域拆成 Maven module。

### 6.2 依赖方向

```text
                    tutor-bootstrap
                         |
      +------------------+-------------------+
      |                  |                   |
      v                  v                   v
  tutor-api         tutor-agent      tutor-infrastructure
      |                  |                   |
      +------------------+-------------------+
                         v
                  tutor-application
                         |
                         v
                    tutor-domain

  tutor-observability: independent cross-cutting configuration
```

约束：

- Domain 不依赖 Spring、JPA/JDBC、Web、AI SDK 或外层模块；
- Application 只依赖 Domain，外部能力由 Application port 表达；
- API、Agent、Infrastructure 都向内依赖 Application，彼此不得直接依赖；
- Bootstrap 只负责装配，不承载业务规则；
- Controller 不实现推荐、权限或学习状态规则；
- 模块间通过应用接口、不可变命令/结果或领域事件协作，不跨边界直接读写他域表。

现有 `ModuleArchitectureTest` 是依赖基线。V2.0 新增领域 package 后必须同步扩大 ArchUnit 覆盖，而不是绕过检查。

### 6.3 建议业务 package 边界

以下是目标逻辑边界，不代表必须一次性创建所有空 package：

```text
cn.forever24.tutor
├── profile / learner       Goal、Preference、Skill State、Memory
├── curriculum              Skill Graph、Skill Unit、Evidence Criteria
├── resource                Resource、Collection、Asset、Publish status
├── entitlement             Access Scope、Grant/Revoke、Access decision
├── planning                Pedagogical Policy、Prescription、ranking
├── experience              Season、Episode、Scene、Episode Mapping
├── training                Lesson Session、Task、Attempt、Completion
├── evidence / review       Evidence validation、state update、review due
├── conversation            Role Play、Correction、Retry orchestration
└── admin / audit           管理用例和审计
```

以上业务边界分别在 Domain 和 Application 中提供模型与用例；`cn.forever24.tutor.ai.provider` 及厂商适配器只位于 `tutor-agent`。`Character Agent` 位于 Experience/Agent 适配边界，接收已冻结的处方约束；它不是独立的 Mastery 或 Planner 领域。

---

## 7. 领域边界与数据所有权

### 7.1 领域能力地图

| 边界 | 核心对象 | 拥有的事实 | 不负责 |
|---|---|---|---|
| Identity/Profile | User、Goal Profile、Preference、Constraint | 用户身份、目标、偏好、时区、隐私设置 | 推荐排序 |
| Learner Model | UserSkillState、Error Memory、Expression Memory、Review State | 能力状态、错误、表达、遗忘风险 | 直接选择 Episode |
| Curriculum | SkillNode、SkillUnit、Variant、Prerequisite、Evidence Criteria | Capability Graph 和教学定义 | 用户权限与进度 |
| Resource Catalog | LearningResource、Collection、Asset、ResourceVersion | 资源 metadata、版本、发布状态、媒体引用 | 用户 Mastery |
| Entitlement | Entitlement、AccessDecision、Grant/Revoke Audit | 用户对 Collection/Resource 的访问权 | 推荐打分 |
| Planning | Pedagogical Policy、Candidate、Prescription | 规则版本、候选解释、处方和 fallback | 生成正式媒体 |
| Experience | Season、Episode、Scene、Story State、Episode Mapping | Experience Graph、关系连续性和场景映射 | 改写 target skill |
| Training | LessonSession、TaskAttempt、RetryRelation、Progress | 用户执行过程、Attempt 和完成状态 | 直接认定 Mastery |
| Evidence/Review | LearningEvidence、CriteriaResult、ReviewTask | 证据有效性、状态更新和复习调度 | 未校验地接受 AI 结论 |
| Provider Runtime | ProviderConfig、Capability、Usage、Trace | 加密配置、路由、调用状态和成本 | 业务决策 |

### 7.2 Resource、Collection、Asset、Entitlement 关系

```text
Provider 1 ---- * Collection 1 ---- * LearningResource
                                          |
                                          | versioned references
                                          v
                                      * Asset

User 1 ---- * Entitlement * ---- 1 Collection

DailyLearningPrescription
    -> SkillUnitVariant
    -> EpisodeMapping
    -> LearningResource@resourceVersion
    -> expected Evidence
```

关键约束：

- `LearningResource` 是可推荐和学习的统一入口；
- `Collection` 组织来源和访问范围，第三方私有内容使用独立 Provider/Collection；
- `Asset` 是不可变媒体版本，至少携带 asset key、content hash、mime type、版本和生成 metadata；
- `Entitlement` 只授予 Collection 访问权，不代表资源已发布或适合推荐；
- Evidence 必须绑定当时的 resourceVersion、skillUnitId、episodeId、taskType 和 attemptId；
- 资源更新创建新版本，不修改历史 Evidence 的语义。

### 7.3 双图谱关系

```text
Capability Graph                         Experience Graph
Goal                                     Season
  -> Skill                                 -> Episode
      -> Skill Unit                            -> Scene
          -> Difficulty Variant                    -> Story State
                    \                          /
                     \                        /
                      +--- Episode Mapping ---+
```

Episode Mapping 至少表达：

- Skill Unit Variant 与可承载 Episode/Scene；
- eligible levels、learner fit 和 contraindications；
- story transition 与 continuity 信息；
- task/media variant references；
- experience fit 输入；
- fallback mapping。

映射不保存 Mastery，也不能用 `episodeId` 顺序推断掌握度。

### 7.4 Completion 与 Mastery

```text
Lesson Progress -----> NOT_STARTED / IN_PROGRESS / COMPLETED / NEEDS_REVIEW

Validated Evidence --> mastery / confidence / trend / review schedule
```

- 完成课程表示执行了配置的输入、理解和至少一个输出任务；
- Mastery 只由达到质量门槛的 Evidence 更新；
- Retry 作为独立 Attempt 和 Evidence 保存，并引用原 Attempt；
- 播放音频、打开 Transcript 或完成 Episode 都不能单独提升 Mastery。

---

## 8. 核心运行时链路

### 8.1 Daily Learning Prescription

```text
Daily trigger / learner request
  -> load CurrentActor + Goal + Constraint
  -> load Skill State + Error/Expression Memory + Review State
  -> resolve urgent goal and due reviews
  -> apply prerequisite/mastery/difficulty/spacing policies
  -> generate Skill Unit Variant candidates
  -> filter published and available resources
  -> filter entitlement and private-plan opt-in
  -> rank by goal, gap, error, review, difficulty, transfer,
     freshness, time and preference
  -> apply interleaving/output/time-budget constraints
  -> resolve Episode Mapping and fallback
  -> persist versioned prescription + rationale + expected Evidence
  -> return cached, zero-Token first screen
```

不可变条件：

- Access Filter 发生在个性化排序之前；
- 处方至少有一个主动输出块；
- 高优先级到期复习不能被剧情新鲜度长期挤出；
- 同一输入状态和规则版本的核心选择可重复；
- LLM 可把规则原因转写成自然语言，但不能添加不存在的推荐依据；
- 处方保存 fallback，且开始任务前再次校验资源状态与权限。

### 8.2 Scenario Lesson 加载与执行

```text
Open prescription task
  -> recheck CurrentActor, publish status and entitlement
  -> resolve exact resourceVersion and asset metadata
  -> start/resume LessonSession idempotently
  -> scene-specific task_hero + context
  -> First Listen
  -> Comprehension Check
  -> Transcript + Key Expressions
  -> Guided Speaking
  -> Active Speaking / Role Play
  -> Correction
  -> Try Again when required
  -> complete configured tasks
  -> Evidence validation and state update
  -> Review scheduling and next-plan signal
```

视觉契约：

- 每个可发布 Scenario Lesson 变体有且仅有一个 `task_hero`；
- `task_hero` 清晰出现 Lin Muen，并显示她与当前地点、动作和沟通问题的关系；
- 任务卡、场景引导、Guided Speaking 和 Role Play 使用同一 master 的裁切或连续 `scene_state`；
- 头像只用于身份提示或图片失败 fallback，不能替代情景训练主图；
- 结构化文字由 Web/Native UI overlay 呈现，不依赖生成图片中文字；
- 首屏图片和正式音频来自已发布资产，不在进入课程时实时生成。

### 8.3 Speaking、Correction、Retry 与 Evidence

```text
Client creates attempt with idempotency key
  -> backend authorizes session ownership
  -> persist answer/audio reference before AI analysis
  -> ASR if needed; low confidence => confirm or re-record
  -> invoke constrained analysis/correction
  -> parse + Schema validate + business validate
  -> present focused feedback (1-3 material issues)
  -> require Retry when policy says so
  -> persist criteria results and retry relation
  -> transactionally create valid Evidence
  -> update Skill/Error/Expression/Review state by deterministic rules
```

失败边界：

- Provider 超时或非法 JSON 时保留 Attempt，状态进入可重试分析，不丢用户回答；
- 同一个 attemptId/idempotency key 不重复生成 Evidence；
- Evidence 写入与 Skill/Review 更新要么在同一事务完成，要么通过可重放的持久化事件达到最终一致；
- AI 修复重试次数有限，最终失败使用安全降级，不能把未经验证的结果写入学习状态。

### 8.4 Private Collection 访问与撤销

```text
Admin grant
  -> authorize ADMIN
  -> upsert entitlement
  -> write audit record

Learner request
  -> resolve CurrentActor
  -> check collection/resource status
  -> check active entitlement and expiry
  -> return catalog or short-lived media access

Admin revoke
  -> revoke entitlement + audit
  -> invalidate entitlement cache
  -> deny new catalog/media access
  -> replace unstarted private prescription task with public fallback
  -> preserve historical progress and Evidence
```

默认规则：

- 没有任何 Private Collection 权限时，客户端隐藏“随心学”；
- 前端隐藏不构成安全边界；
- EngFluent 默认不进入今日计划；P1 只有“有效权限 + 用户主动开启 + 资源可用”才可进入候选池；
- 已签发媒体访问不能成为长期绕过 revoke 的通道，因此私有 URL 必须短时有效或经过受保护代理。

---

## 9. 离线内容生产与发布架构

### 9.1 生产链路

```text
Skill Unit Blueprint
  -> learner fit + prerequisites
  -> difficulty/scaffolding/training variants
  -> common errors + Evidence/Retry/Review policy
  -> Episode Mapping
  -> story/dialogue/questions/expressions
  -> schema + pedagogical + safety validation
  -> TTS + task_hero/scene_state generation
  -> transcript/audio + character/asset validation
  -> immutable lesson package + manifest
  -> human sampling/review
  -> import assets and metadata
  -> publish Resource Catalog version
```

禁止先按 EP001 至 EP010 生成十节固定课程后补教学目标。正确顺序始终是 `Skill Unit -> Evidence -> Episode Mapping -> Story/Media`。

### 9.2 Staging、Catalog 与对象存储

| 区域 | 用途 | 是否运行时事实源 |
|---|---|---|
| `resources/learning-content/v2.0/` | 本地/批处理 staging、Schema、报告和待发布包 | 否 |
| Git | 文本 manifest、Schema、版本化 Prompt、生成 metadata、工具 | 否 |
| MySQL Resource Catalog | 已导入资源 metadata、版本、发布状态和关系 | 是 |
| Object Storage/CDN | 已发布图片、音频、视频和大型附件 | 是 |

媒体不得长期放入 `server/**/src/main/resources/`，也不得随 JAR 或应用镜像分发。

### 9.3 发布门禁

资源进入 `PUBLISHED` 前至少通过：

- JSON Schema（建议 `additionalProperties: false`）；
- 业务枚举、引用完整性、版本和唯一性校验；
- CEFR、communication complexity、learner fit 和 prerequisites 检查；
- 主动输出、Evidence Criteria、Retry/Review 完整性检查；
- Audio Script 与 Transcript 一致性检查；
- Lin Muen 身份、语气、视觉和场景连续性检查；
- 每个变体恰好一个符合展示面的 `task_hero`；
- asset key、hash、mime type 和必要 metadata 检查；
- AI reviewer 与人工抽检。

导入必须可重复执行：相同 resourceId + resourceVersion + contentHash 不重复创建；不同内容不得静默覆盖同一版本。

---

## 10. 接口与通信约定

### 10.1 客户端接口

- 使用现有 `/api/v1` 风格的 HTTPS REST 承载查询和命令；
- 使用 SSE 承载 AI 对话、Correction 和 Feedback 的增量输出；
- 正式媒体通过 CDN/public URL 或后端授权的短期对象地址访问；
- 写操作携带幂等键，服务端根据 CurrentActor 和资源归属处理重复提交；
- 错误响应区分认证、授权、资源不可用、输入非法、Provider 配置、Provider 暂时失败和可重试状态；
- API DTO 不直接暴露领域对象、数据库记录或 Provider SDK 类型。

具体端点与字段必须在 `contracts/openapi/english-tutor-api.yaml` 中增量冻结后再实现。

### 10.2 内部端口

Application 层至少需要表达以下能力类别：

- LearnerStateRepository / SkillGraphRepository；
- ResourceCatalogRepository / AssetRepository；
- EntitlementRepository / AccessDecisionService；
- PrescriptionRepository / Clock；
- TrainingSessionRepository / EvidenceRepository / ReviewRepository；
- ObjectStoragePort / MediaAccessPort；
- ConversationReplyStreamer / CorrectionAnalyzer / OpenAnswerEvaluator，以及后续面向 Role Play、转写和语音合成的用例级端口；
- ProviderConfigurationResolver；
- AuditPort / MetricsPort。

底层 `ChatProvider`、`SpeechToTextProvider` 和 `TextToSpeechProvider` 是 `tutor-agent` 内的项目自有 Provider 契约，用于隔离厂商协议；Application 不反向依赖 `tutor-agent`。名称由实现任务最终确定，但业务代码只能依赖项目自有、面向用例的端口。

### 10.3 领域事件

适合使用进程内领域事件或持久化事件的事实包括：

- `PrescriptionCreated`；
- `TaskAttemptRecorded`；
- `EvidenceAccepted`；
- `SkillStateChanged`；
- `ReviewScheduled`；
- `EntitlementRevoked`；
- `ResourcePublished`。

事件用于解耦后续动作，不允许绕过拥有该事实的应用服务直接修改其他领域数据。P0 不要求外部事件总线。

---

## 11. AI 与 Provider 架构

### 11.1 Provider 抽象

业务与 Application 只使用项目自有 Provider 契约，统一表达：

- provider/model/capability；
- timeout 和错误分类；
- usage/cost；
- confidence；
- trace；
- structured response；
- audio input/output metadata。

Provider SDK、HTTP 协议、认证头和厂商响应仅存在于 `tutor-agent` 适配器。

### 11.2 当前运行时决策

按 ADR-0015/0016：

- DeepSeek 是当前默认 LLM，通过 OpenAI-compatible Chat Completions adapter；
- OpenAI Responses 和 Gemini native protocol 可作为 LLM 选择；
- 当前 ASR/TTS 使用 OpenAI 类型 Provider；
- endpoint、model、default、timeout 和 API key 的运行时事实源为数据库；
- API key 以 AES-GCM 加密保存，API 与日志只暴露 masked hint；
- 环境变量只保存基础设施与系统密钥，例如解密数据库 secret 的系统密钥；
- Provider 未配置时应用可启动，调用相关能力时返回可操作的配置错误。

这些是当前已接受实现基线，不阻止以后通过同一端口替换厂商。

### 11.3 Prompt 与验证

- 每个 Prompt 有稳定 ID、版本、任务类型和预期 Schema；
- 处方规则版本、Prompt 版本、模型标识和资源版本进入 trace；
- 测试使用固定响应和 mocked transport，不调用真实付费模型；
- IELTS 结果必须明确为练习参考，不声称官方成绩；
- Character Prompt 读取 Lin Muen Character Bible，但教学目标、成功标准和 Retry policy 由处方输入，不由 Character Agent 自行推断。

---

## 12. 一致性、幂等与并发

### 12.1 事务边界

建议的原子事务：

- Grant/Revoke Entitlement + Audit；
- 创建/更新 ResourceVersion + 引用关系；
- 创建 Prescription + tasks + rationale snapshot；
- 接受 Attempt receipt，确保同一幂等键只有一个业务结果；
- 接受 Validated Evidence + 更新 Skill/Error/Expression/Review state。

对象存储上传不能与数据库形成单一 ACID 事务。采用“先上传不可变临时/版本对象，再校验 hash，再提交 metadata；失败对象由清理任务回收”的补偿方式，避免数据库引用不存在的媒体。

### 12.2 并发规则

- 用户同一天重复请求处方时按 userId + local learning date + prescription version 幂等；
- 并发提交同一 Attempt 时只接受一个结果，其余返回同一 receipt；
- Skill State 更新使用版本号或条件更新防止丢失更新；
- Revoke 与课程开始竞争时，以课程开始/媒体访问时的后端最新 AccessDecision 为准；
- 资源发布采用显式版本，不在 PUBLISHED 版本上原地覆盖内容。

---

## 13. 安全、隐私与授权

### 13.1 认证与数据隔离

- 正常学习接口从后端认证上下文解析 CurrentActor；
- 客户端不得通过自报 userId 访问或修改其他用户数据；
- 管理接口位于 `/api/v1/admin/**` 并使用 authority 校验；
- 所有用户写操作校验 session/resource/attempt 归属；
- 管理员 Grant/Revoke、Provider 配置和资源发布产生审计记录。

### 13.2 媒体与隐私

- 私有资源和用户录音使用受保护对象，不使用可永久传播的公开 URL；
- 日志不记录完整授权头、API key、录音内容或不必要的用户原文；
- 用户录音、Transcript 和原文保存遵循 PrivacySettings 与 retention policy；
- 数据库只保存对象 key、hash、长度、mime type、owner 和 retention metadata；
- 第三方 Collection 保存 source、ownershipType、licenseNote、allowedAudience 和 adminNote；技术授权不替代法律授权。

### 13.3 配额与滥用防护

- 延续 SaaS Foundation 的成功 AI 学习业务请求配额；
- 首屏静态资源读取不计作模型请求；
- Provider usage/cost 作为内部可观测数据，不替代业务配额；
- Role Play、ASR/TTS 和重试设置服务端上限、超时和速率保护。

---

## 14. 性能、缓存与降级

### 14.1 零 Token 首屏

今日处方和 Scenario Lesson 首屏只读取：

- 已持久化处方；
- Resource Catalog metadata；
- 预生成 JSON；
- 已发布 task hero、Audio 和 Transcript。

进入自由回答、Role Play、Correction 或 Evaluation 前不调用 LLM。

### 14.2 缓存策略

| 数据 | 建议缓存 | 失效条件 |
|---|---|---|
| Published catalog/skill graph | Redis + 应用只读缓存 | 发布新版本、下架 |
| Public asset | CDN，content-hash key 长缓存 | 新版本使用新 key |
| Entitlement decision | 短 TTL Redis | grant/revoke 主动失效 |
| Prescription response | user-scoped 短期缓存 | Evidence、反馈、目标或权限变化 |
| Provider config | 仅请求级/极短缓存 | 管理端修改后下一请求生效 |

Redis miss 必须回源 MySQL；Redis 丢失不能丢长期学习事实。

### 14.3 降级矩阵

| 故障 | 用户可继续做什么 | 系统动作 |
|---|---|---|
| task hero 失败 | 查看 Lin Muen fallback、场景文字、Audio/Transcript | 记录 asset/version，允许继续课程 |
| Audio 失败 | Transcript + Guided Speaking/Role Play | 提供重试，不把音频失败等同课程失败 |
| ASR 低置信度 | 确认转写或重录 | 不把低置信度文本直接作为高置信 Evidence |
| LLM/Correction 失败 | 保留回答和 Attempt | 标记待分析，可稍后重试 |
| 私有资源失效/撤权 | 公共资源 fallback | 拒绝私有访问，保留历史记录 |
| Redis 不可用 | 关键读写回源 MySQL | 降低性能，不改变长期事实 |
| Provider 未配置 | 非 AI 内容仍可访问 | AI 能力返回明确配置错误和 trace |

---

## 15. 可观测性与审计

### 15.1 统一关联标识

关键链路至少携带：

- traceId / requestId；
- currentActorId（日志中使用内部、可脱敏标识）；
- prescriptionId / prescriptionVersion；
- resourceId / resourceVersion；
- lessonSessionId / attemptId；
- skillUnitId / episodeId；
- provider/model/promptVersion；
- accessDecision reason；
- AI usage、latency、error category。

### 15.2 指标

| 类别 | 示例 |
|---|---|
| 私教质量 | prescription generated、reason coverage、review due hit、difficulty adjustment、transfer success |
| 沉浸体验 | task hero load、mission start、guided speaking、role play、retry completion |
| 学习效果 | Evidence accepted、retry improvement、expression reuse、skill trend |
| 权限 | access denied by reason、private resource ineligible、revoke latency、fallback success |
| 内容 | import validation failure、asset mismatch、resource publish/unpublish、broken asset |
| AI/成本 | provider calls、latency、schema failure、repair retry、token/audio usage、estimated cost |

推荐链路必须能回答“为何选择”和“为何排除”，但不得把完整用户原文或秘密写入日志。

### 15.3 审计事件

至少审计：

- Resource import/publish/unpublish/disable；
- Collection 创建与 Access Scope 变更；
- Entitlement grant/revoke/expire；
- 管理员用户/角色/配额变更；
- Provider 配置、默认切换和连接测试；
- 影响学习状态的规则版本变更。

---

## 16. Web 与 Android 客户端架构

### 16.1 Web-first P0

Web 使用现有 React + TypeScript + Vite 基线：

- feature 组件只通过 shared API client 访问后端；
- 服务端返回处方、访问结果、资源版本和恢复状态；
- SSE client 处理流式对话并支持断开、重试和最终状态对账；
- UI 至少覆盖 loading/content/empty/error/permission-revoked/media-fallback；
- task hero 使用 focalPoint 和响应式裁切，不能在窄屏裁掉 Lin Muen；
- 本地状态不推断 Mastery 或 Entitlement。

### 16.2 Android 兼容边界

Android 不属于当前 V2.0 Web-first 的首要交付面，但 API 与领域语义必须兼容既有 ADR：

- Compose + ViewModel + StateFlow + UDF；
- Repository/use case 分层，UI 不直接访问网络、数据库或 Provider；
- Room/DataStore 保存可恢复缓存和待同步操作；
- 半双工 Push-to-Talk；
- 写操作携带幂等键并以服务端事实为准。

因此 Web-first 不等于 Web-only，也不需要在 V2 P0 同时复制所有页面到 Android。

---

## 17. 测试架构与质量门禁

### 17.1 测试分层

| 层级 | 必测内容 |
|---|---|
| Domain unit | prerequisite、mastery、spacing、difficulty、interleaving、transfer、retry、access、completion/evidence 分离 |
| Application unit | 用例编排、权限先于排序、fallback、事务、幂等、Provider failure、非法 JSON |
| Contract | OpenAPI、AI JSON Schema、课程 Schema、manifest 与 example 一致性 |
| Infrastructure integration | MySQL/Flyway、Redis 降级、对象存储 key/hash、加密 secret、并发更新 |
| Architecture | Maven/package 依赖方向、Domain 无 Spring/AI SDK、Controller 无基础设施依赖 |
| Web component | loading/content/empty/error、权限撤销、媒体 fallback、SSE 重连、task hero 裁切语义 |
| E2E | 两个不同 Skill State 产生不同处方；完整 Scenario Lesson；Retry/Evidence；grant/revoke；公共 fallback |
| Content QA | 72 变体 Schema、CEFR、Transcript/Audio、Evidence Criteria、Lin Muen/task hero 一致性 |

### 17.2 AI 测试规则

- 默认使用本地 stub、mocked transport 或固定响应；
- 覆盖正常 JSON、缺字段、多字段、错误枚举、超长输出、非 JSON、超时和 Provider unavailable；
- 验证非法输出不改变 Skill State、Review 或权限；
- Prompt 变更运行离线评测集与关键课程回归；
- 不在单元测试或常规 CI 中调用真实付费模型。

### 17.3 发布门禁

V2.0 发布至少证明：

- 同等级不同 Skill State 用户可获得不同、可解释的处方；
- Season 顺序不会覆盖 Skill Gap 或 Review；
- 未授权资源不进入候选且无法通过 API/媒体 URL 访问；
- 完整场景课产生主动输出、反馈、Retry 和可追踪 Evidence；
- Provider/媒体失败不丢 Attempt 或进度；
- 资源与 Evidence 可追溯到版本；
- 现有 Maven、Web、ArchUnit、契约和 E2E 检查通过。

---

## 18. 实施切片与演进

### 18.1 P0 实施顺序

| 切片 | 目标 | 关键交付 |
|---|---|---|
| M1 私教与资源底座 | 能选择目标并安全读取资源 | Learner Model、Skill Graph、Policy、Prescription、Resource/Asset/Collection/Entitlement、Access Filter |
| M2 Scenario Player | 把处方转成 Lin Muen 场景输入与低压力输出 | Episode Mapping、task hero、Audio、Transcript、Questions、Guided Speaking |
| M3 Speaking 闭环 | 用户回答影响学习状态 | Role Play、Correction、Retry、Validated Evidence、Memory/Review |
| M4 内容规模 | 完整公共课程库 | 72 Skill Unit Variants、Season 1 mappings、QA/report/publish |
| M5 Private Collection | 安全随心学 | EngFluent catalog、grant/revoke、进度、受保护媒体、fallback |
| M6 发布与观测 | 证明专业性和稳定性 | 解释、指标、成本、E2E、发布验收 |

每个切片先更新 OpenAPI/Schema/Flyway/测试，再实现纵向闭环；不得一次创建未来所有领域的空壳。

### 18.2 未来演进触发器

只有出现以下证据时才评估拆服务：

- 内容导入或媒体处理需要独立扩容和发布周期；
- Provider/对话流量需要与核心事务独立伸缩；
- 团队边界长期稳定且模块耦合已受控；
- 单体部署成为可测量的可靠性或交付瓶颈。

即使拆分，领域端口、版本契约和事实所有权保持不变。P2 的实时语音、动态视觉、生成式视频或向量检索不是 V2.0 发布依赖。

---

## 19. 决策追踪

| 架构决策 | 来源 |
|---|---|
| 模块化单体、禁止随意跨模块读表 | ADR-0001 |
| 半双工 Push-to-Talk | ADR-0002 |
| 确定性规则 + 受约束 LLM | PRD 3.6、12.5；ADR-0003 |
| 服务端事实源、客户端缓存 | ADR-0004 |
| AI 输出 Parse -> Schema -> Business Validation | ADR-0005 |
| MySQL + Redis + S3-compatible storage | ADR-0006 |
| Java 21、Spring Boot 4.1、Spring AI 2.0 | ADR-0007 |
| Android Compose/UDF | ADR-0008、ADR-0012 |
| 项目自有 Provider 抽象 | ADR-0009 |
| CurrentActor、RBAC、配额、审计和加密 secrets | ADR-0014 |
| 多 LLM 协议、DeepSeek 当前默认 | ADR-0015 |
| 数据库是运行时 Provider 配置唯一来源 | ADR-0016 |
| 双核心、双图谱、Evidence 闭环 | V2.0 PRD；设计 07/08/09/11 |
| 权限先于推荐、Private Collection 隔离 | V2.0 PRD 3.4、12、13；Resource Model |
| 离线媒体、零 Token 首屏 | V2.0 PRD 3.3、14、15；Content Pipeline |
| Lin Muen scene-specific task hero | Scenario Lesson Flow、Image Spec、Course Resource Schema |

---

## 20. 后续必须补齐的专项契约

架构基线完成后，进入实现前按优先级补齐：

1. Resource、Skill Unit、Episode Mapping、Prescription、Attempt 和 Evidence 的 JSON Schema；
2. V2.0 OpenAPI：catalog、prescription、lesson session、attempt、entitlement、admin import/publish；
3. 领域数据模型、状态机、索引和 Flyway 前向迁移计划；
4. 推荐规则版本、评分归一化、阈值与可重复性测试向量；
5. 媒体上传、校验、短期访问与 revoke 失效方案；
6. 持久化 AI 分析重试任务和补偿清理方案；
7. Web Scenario Lesson UI 状态与端到端验收场景；
8. 72 Skill Unit Variant 的批量 QA 报告与发布清单；
9. EngFluent 的实际托管与分发授权结论。

第 9 项是第三方内容发布前的外部合规门禁，但不阻塞公共 Scenario Lesson 和通用 Private Collection 能力建设。

---

## 21. 架构基线结论

V2.0 的技术主线不是“用 AI 实时生成更多课”，而是把可测试的教学决策、可版本化的课程资源和可验证的学习证据连成一个稳定闭环：

```text
deterministic tutor decision
  -> versioned Skill Unit and accessible Resource
  -> Lin Muen immersive mission
  -> active learner output
  -> validated Evidence
  -> deterministic learner-state update
  -> better next prescription
```

该闭环必须在现有模块化单体中先完整成立。Lin Muen 让训练值得参与，Personalized Tutor Core 保证训练值得学习；任何一侧都不能替代另一侧。
