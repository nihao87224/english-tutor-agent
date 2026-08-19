# English Tutor Agent V2.0 概要设计

> 文档版本：`2.0.0`
> 状态：`V2.0 概要设计基线`
> 日期：`2026-08-19`
> 上游：V2.0 PRD、`01_ARCHITECTURE.md`、V2.0 Web UI 交互稿

---

## 1. 目的

本文把 V2.0 产品需求和总体架构转换为可进入详细设计的功能方案，回答：

- 用户在 Web 中如何完成“今日处方 → Lin Muen 情景训练 → Feedback / Retry → Evidence”的闭环；
- 前后端、内容资源、AI Provider 和数据存储如何协作；
- 每项 P0 能力由哪个逻辑模块负责；
- 哪些行为必须同步完成，哪些可以降级或延迟处理；
- 详细设计和开发任务需要冻结哪些契约。

本文不定义完整字段、DDL、Prompt 和具体实现类。

## 2. 产品与交互基线

### 2.1 双核心关系

```text
Personalized Tutor Core
  决定目标、难度、支架、复习、任务和成功标准
                |
                v
Lin Muen Immersive Experience
  提供 Episode、Scene、task hero、故事、对话和陪伴
                |
                v
Learner Output
  Speaking / Role Play / Retry
                |
                v
Validated Evidence
  更新 Skill、Error、Expression、Review 和下一次处方
```

### 2.2 Web P0 信息架构

```text
今日学习
├── 今日处方
├── Scenario Lesson
└── 完成反馈

AI 对话
能力画像
复习
随心学（有 Private Collection 权限时显示）
我的
```

交互稿：`docs/ui/v2.0/ENGLISH_TUTOR_AGENT_WEB_UI_PROTOTYPE_v2.0.html`。

### 2.3 核心页面

| 页面 | 用户目标 | 核心数据 | 主操作 |
|---|---|---|---|
| 今日处方 | 立即知道今天为什么练什么 | priority goal、blocks、rationale、time、Episode context、task hero | 开始任务、跳过/反馈 |
| Scenario Intro | 1–2 秒理解人物、地点和任务 | Lin Muen task hero、Scene、Mission、communication goal | 开始听力 |
| Listen & Understand | 获得可理解输入 | Audio、Transcript、Questions、Expressions | 播放、答题、查看表达 |
| Guided Speaking | 低压力组织输出 | prompt、scaffold、criteria | 文本/语音回答 |
| Role Play | 完成真实沟通目标 | role boundary、dialogue state、criteria | PTT/文本交互 |
| Feedback & Retry | 聚焦关键问题并改进 | correction、natural expression、retry requirement | Try Again |
| Completion | 理解本次结果和下一步 | Evidence summary、skill change、review、next signal | 返回今日计划 |
| 随心学 | 访问已授权课程 | Private Collection、catalog、progress | 学习/恢复 |

## 3. P0 用例

### UC-01 生成或读取今日处方

前置：用户已完成基础 Profile/Assessment。

主流程：

1. 系统读取有效的当日处方；不存在或失效时触发生成；
2. Planner 加载 Goal、Constraint、Skill State、Error/Expression Memory 和到期 Review；
3. 规则选择 Skill Unit Variant 和训练策略；
4. Resource Access Filter 删除未发布、损坏、无权限或不适配候选；
5. 排序并组合 review/acquisition/output/transfer blocks；
6. Experience Resolver 选择 Episode Mapping、task hero 和 fallback；
7. 持久化处方快照、理由、规则版本和资源版本；
8. Web 展示零 Token 首屏。

后置：处方可重复读取；相同输入与规则版本的核心决策可重放。

### UC-02 开始或恢复 Scenario Lesson

1. Web 用 prescriptionTaskId 请求开始；
2. 后端重新校验 CurrentActor、资源发布状态和 Entitlement；
3. 创建或返回幂等的 LessonSession；
4. 返回准确 resourceVersion、步骤定义和媒体访问；
5. Web 恢复到服务端确认的当前步骤。

### UC-03 提交理解题

1. 用户提交 questionId、answer 和 idempotencyKey；
2. 后端校验 session/task 归属和当前状态；
3. 客观题使用确定性答案规则评分；
4. 保存 Attempt；
5. 返回结果、解释和下一步骤资格。

### UC-04 提交 Speaking / Role Play

1. 用户提交文本，或先上传 PTT 录音；
2. 录音上传完成后创建 Attempt，先持久化用户输入引用；
3. 低置信度 ASR 要求用户确认或重录；
4. AI 在 Skill/Evidence/Role 边界内分析；
5. 输出经 Parse、JSON Schema 和业务校验；
6. 保存 correction 和 criteria result；
7. Retry Policy 决定继续、重试或完成。

### UC-05 Retry 与 Evidence

1. 关键问题触发 Retry；
2. Retry Attempt 引用原 Attempt；
3. 系统比较 criteria improvement，不只比较文本相似度；
4. 有效结果产生 Learning Evidence；
5. 确定性规则更新 Skill/Error/Expression/Review；
6. Planner 接收 next-plan signal。

### UC-06 访问 Private Collection

1. Web 只在用户至少有一项有效 Private Collection 权限时显示“随心学”；
2. 每次目录、资源和媒体请求都由后端重新校验；
3. revoke 后停止新访问，保留历史 Progress/Evidence；
4. 未开始的私有处方任务使用公共 fallback 替换。

### UC-07 内容导入与发布

1. Content Pipeline 生成 versioned lesson package；
2. 自动执行 Schema、业务、教学、角色、音频和图片检查；
3. 上传不可变媒体并核对 content hash；
4. 导入 Catalog draft；
5. Reviewer 抽检后发布；
6. 发布事件使 Catalog cache 失效。

## 4. 逻辑组件

```text
Web Client
  -> API Facade
      -> Profile / Learner Model
      -> Personalized Tutor / Planning
      -> Curriculum / Skill Graph
      -> Resource Catalog
      -> Entitlement / Access
      -> Experience / Episode Mapping
      -> Scenario Lesson / Training
      -> Conversation / AI Feedback
      -> Evidence / Memory / Review
      -> Admin / Content Import
          -> Persistence / Cache / Object Storage
          -> LLM / ASR / TTS Adapters
```

### 4.1 组件职责

| 组件 | 输入 | 输出 | 主要规则 |
|---|---|---|---|
| Learner Model | Profile、Attempt、Evidence、Feedback | learner snapshot | Completion 不提升 Mastery |
| Pedagogical Policy | learner snapshot、time、catalog capability | teaching decision | deterministic、versioned |
| Prescription Composer | decisions、candidates | DailyLearningPrescription | 至少一个 output block |
| Resource Catalog | query、version、status | resource metadata | published/available only |
| Access Decision | actor、resource/collection | allow/deny + reason | 排序前过滤，访问时复检 |
| Experience Resolver | Skill Unit Variant、preference | Episode Mapping | Experience 不改教学目标 |
| Lesson Orchestrator | prescription task、session state | current step | 可恢复、幂等 |
| Conversation Orchestrator | task boundary、user input | streamed reply/feedback | Prompt/Schema versioned |
| Evidence Service | validated criteria result | Evidence + state changes | deterministic update |
| Content Importer | lesson package | draft/published resource | immutable version/hash |

## 5. 前端概要设计

### 5.1 状态模型

每个核心页面至少覆盖：

- `loading`：第一次加载；
- `content`：可正常交互；
- `empty`：无处方、无私有权限或无到期复习；
- `error`：网络/服务失败且可重试；
- `offline/reconnecting`：SSE 或网络中断；
- `permission_revoked`：私有内容已撤权；
- `media_fallback`：图片或 Audio 失败；
- `analysis_pending`：回答已保存但 AI 分析待重试。

### 5.2 交互约束

- 今日处方必须展示可验证的推荐原因；
- task hero 在任务卡、Scenario Intro 和训练主体保持场景连续；
- 任务主图必须清晰出现 Lin Muen，头像不能替代场景图；
- UI overlay 承载航班、菜单、会议等准确文字；
- 用户可反馈“太难、太简单、时间不足、不想练该主题”；
- 陪伴强度只改变故事和表达密度，不改变 target skill 和完成条件；
- AI 流式回复结束后以服务端最终状态对账；
- 刷新页面后根据 session currentStep 恢复，不依赖浏览器本地推断。

### 5.3 Web 模块建议

```text
web/src/features
├── prescription
├── scenario-lesson
├── speaking
├── feedback
├── learner-profile
├── review
├── free-learning
└── admin-content
```

只在对应纵向任务开始时创建模块，避免预建空壳。

## 6. 后端概要设计

### 6.1 请求处理层次

```text
Controller / SSE Endpoint
  -> CurrentActor + DTO validation
  -> Application Service transaction/use case
  -> Domain policy/entity
  -> Repository / Provider / ObjectStorage port
  -> Adapter
```

### 6.2 新增逻辑边界

| 边界 | 建议 package | 复用 |
|---|---|---|
| Curriculum | `curriculum` | 新增 Skill Graph/Unit |
| Resource | `resource` | 新增 Catalog/Asset/Collection |
| Entitlement | `entitlement` | 复用 CurrentActor/RBAC/Audit |
| Planning | `planning` | 扩展现有 Today Plan |
| Experience | `experience` | 新增 Season/Episode/Mapping |
| Training | `training` | 扩展 Session/Attempt |
| Evidence/Review | `learner` / `review` | 扩展现有 Evidence/Skill State |
| AI interaction | Application use-case ports + `tutor-agent` adapter | 复用 Provider abstractions |

### 6.3 同步与异步边界

同步完成：

- 鉴权、Access Decision；
- 处方读取；
- Session start/resume；
- Attempt receipt；
- 客观题评分；
- 已验证 Evidence 的原子状态更新；
- Grant/Revoke + Audit。

允许延迟：

- AI 分析失败后的重试；
- 非关键推荐解释润色；
- 内容媒体清理；
- 指标聚合；
- 非阻塞通知。

P0 使用 MySQL 持久化任务/事件和同进程 worker，不引入外部消息队列。

## 7. 数据概要

### 7.1 现有数据复用

- `app_user`、RBAC、refresh session；
- `user_learning_profile`、`learner_skill_state`；
- `learning_plan`、`learning_task`；
- `training_session`、`task_attempt`；
- `learning_evidence`；
- quota、admin audit、AI Provider config/secret。

### 7.2 V2 新增数据组

| 数据组 | 主要记录 |
|---|---|
| Curriculum | skill node、edge、skill unit、variant、prerequisite、criteria |
| Resource | provider、collection、resource、version、asset、resource-asset |
| Experience | season、episode、scene、mapping、story state |
| Access | entitlement、grant/revoke audit |
| Prescription | prescription、block/task、rationale/factor snapshot、fallback |
| Lesson execution | lesson session state、question result、role-play turn、retry relation |
| Learning memory | error occurrence、expression state、review schedule |
| Operations | import batch、validation issue、analysis retry job |

所有用户事实带 user ownership，所有版本化内容事实带 resourceVersion 或 ruleVersion。

## 8. API 概要

V2 在现有 `/api/v1` 下增加六组接口：

| API group | 主要用例 |
|---|---|
| `/prescriptions` | 今日处方、反馈、重新组合 |
| `/learning-resources` | 公共/有权 Catalog 与资源详情 |
| `/lesson-sessions` | start/resume/current step/complete |
| `/lesson-sessions/{id}/attempts` | 理解题、Speaking、Retry |
| `/private-collections` | 随心学目录、详情和进度 |
| `/admin/...` | 资源导入/发布、Collection、Entitlement |

保留现有 `/plans/today` 和 `/training-sessions` 的兼容期；最终是扩展现有契约还是引入上述资源化端点，由 API 设计文档冻结，禁止同时长期维护语义重复端点。

## 9. 安全与权限

- CurrentActor 来自认证上下文；learner API 不接受可替换的目标 userId；
- AccessDecision 顺序：认证 → resource status → collection scope → entitlement → ownership；
- 私有媒体使用短期签名 URL 或受保护代理；
- revoke 主动失效缓存；
- 用户录音和原文受 retention/privacy 设置控制；
- API key 加密保存，日志仅 masked hint；
- 所有管理写操作、授权和发布写入审计。

## 10. 性能与降级

| 场景 | 目标 |
|---|---|
| 今日首页 | 读取持久化处方和 Catalog，不调用 LLM |
| 课程首屏 | 预生成图片/Audio/JSON，可 CDN 缓存 |
| AI Feedback | SSE 增量输出，最终结果服务端持久化 |
| 图片失败 | Lin Muen fallback + context + Audio/Text |
| Audio 失败 | Transcript + Speaking |
| AI 失败 | Attempt 已保存，返回 analysis pending |
| 私有资源失效 | 公共 fallback，不影响公共学习闭环 |

具体性能 SLO 在实现计划的性能基线任务中冻结。

## 11. 可观测性

每条核心链路关联：

- traceId、actorId；
- prescriptionId/version；
- resourceId/version、skillUnitId、episodeId；
- lessonSessionId、attemptId；
- ruleVersion、promptVersion、provider/model；
- access decision、fallback reason；
- latency、usage/cost、schema validation result。

产品指标同时覆盖教学适配、主动输出、Retry 改善、迁移成功和持续学习，不以页面点击或 Episode 播放量替代学习效果。

## 12. 验收场景

1. 两个同为 B1、但 Skill State 不同的用户获得不同今日处方；
2. 机场确认任务展示 Lin Muen 位于登机口环境中的 task hero；
3. 用户完成输出、收到 Correction、Retry 后形成版本化 Evidence；
4. Episode 顺序不覆盖到期复习或 Skill Gap；
5. 无权限用户看不到随心学，也不能通过 API/URL 获取私有资源；
6. revoke 后未开始任务自动使用公共 fallback，历史进度保留；
7. LLM 返回非法 JSON 时 Attempt 保留且 Skill State 不变；
8. 图片、Audio 或 Redis 单点失败时仍能完成有效训练；
9. 72 个内容变体可通过自动 QA 后批量发布；
10. 所有关键状态可追溯到资源、规则、Prompt 和 Attempt 版本。

## 13. 详细设计输入

后续文档必须基于本文继续冻结：

- 后端聚合、应用服务、端口、事务、事件和状态机；
- API endpoint、DTO、错误码、幂等和 SSE event；
- 逻辑表、键、索引、约束、保留和迁移顺序；
- Codex 可单独实施且可验证的任务卡。
