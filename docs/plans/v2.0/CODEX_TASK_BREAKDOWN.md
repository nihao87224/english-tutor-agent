# English Tutor Agent V2.0 Codex 开发任务拆解

> 文档版本：`2.0.0`
> 状态：`可执行任务基线`
> 日期：`2026-08-19`
> 上游：V2.0 PRD、架构、概要设计、后端详细设计、API/数据库设计

---

## 1. 执行规则

### 1.1 一次只执行一张任务卡

Codex 每次任务必须：

1. 读取任务卡和直接关联文档；
2. 同步 `origin/develop/v2.0`，确认无冲突；
3. 检查工作树并保护用户已有修改；
4. 在修改前列出计划和预计文件；
5. 只实现当前任务卡，不预建后续空壳；
6. 先更新机器契约/测试，再实现代码；
7. 执行与风险相称的 unit/integration/contract/Web/E2E 检查；
8. 更新本任务状态和必要文档；
9. 创建聚焦提交并推送 `develop/v2.0`；
10. 按仓库 `AGENTS.md` 格式报告。

任务执行状态：`TODO -> IN_PROGRESS -> DONE`；只有验收条件全部满足才能标记 DONE。

### 1.2 全局门禁

- Java 21、Spring Boot 4.1、Spring AI 2.0；
- 不修改已发布 Flyway V1–V18；
- Domain 不依赖 Spring/JDBC/Jackson/AI SDK；
- Application 不依赖 API/Agent/Infrastructure；
- AI 输出 Parse → JSON Schema → business validation；
- 测试不调用真实付费模型；
- learner endpoint 使用 CurrentActor，不接受替换 userId；
- 每个写接口覆盖幂等、授权、重复提交和事务回滚；
- Completion 与 Mastery 分离；
- Access Filter 在排序前；
- Episode Mapping 在 Skill Unit 决策后；
- 媒体存对象存储，不进入 MySQL BLOB 或 Spring classpath；
- Lin Muen task hero 是 Scenario Lesson P0 正常视觉，不是 fallback 头像。

### 1.3 任务状态表

| ID | 任务 | 依赖 | 状态 |
|---|---|---|---|
| V2-T01 | 课程与资源机器 Schema | 无 | DONE |
| V2-T02 | Curriculum / Skill Graph 纵向切片 | T01 | DONE |
| V2-T03 | Resource Catalog / Asset 纵向切片 | T01 | DONE |
| V2-T04 | Experience Graph / Episode Mapping | T02,T03 | DONE |
| V2-T05 | Entitlement / Access Filter | T03 | TODO |
| V2-T06 | Catalog learner/admin API | T03,T05 | TODO |
| V2-T07 | Pedagogical Policy 基线 | T02 | TODO |
| V2-T08 | Daily Prescription 后端 | T04,T05,T07 | TODO |
| V2-T09 | Web 今日处方 | T08 | TODO |
| V2-T10 | Scenario Lesson Session 基线 | T06,T08 | TODO |
| V2-T11 | Web 场景引导与媒体输入 | T10 | TODO |
| V2-T12 | Comprehension 与 Guided Speaking | T10,T11 | TODO |
| V2-T13 | Audio Upload / ASR 确认 | T10 | TODO |
| V2-T14 | Role Play SSE | T12,T13 | TODO |
| V2-T15 | Correction / Retry / Attempt 状态 | T12,T14 | TODO |
| V2-T16 | Evidence / Skill State 原子更新 | T15 | TODO |
| V2-T17 | Web Feedback / Retry / Completion | T15,T16 | TODO |
| V2-T18 | Error / Expression Memory / Review | T16 | TODO |
| V2-T19 | 处方反馈、迁移与闭环 | T18 | TODO |
| V2-T20 | 内容 Import / Validation Pipeline | T01,T03,T04,T15 | TODO |
| V2-T21 | 媒体发布与受保护访问 | T05,T20 | TODO |
| V2-T22 | 内容管理 API / Script | T20,T21 | TODO |
| V2-T23 | B1 24 个资源发布验证 | T19,T22 | TODO |
| V2-T24 | 完整 72 个资源发布 | T23 | TODO |
| V2-T25 | Private Collection / 随心学 | T05,T06,T21 | TODO |
| V2-T26 | Grant/Revoke Web 与 fallback | T08,T25 | TODO |
| V2-T27 | 可观测性、成本与内容指标 | T19,T24,T26 | TODO |
| V2-T28 | V2 E2E、性能与发布门禁 | T27 | TODO |

---

## 2. M1：机器契约与资源底座

### V2-T01 课程与资源机器 Schema

目标：把概念契约变为可自动校验的 JSON Schema。

范围：

- 新增 learning resource manifest、Skill Unit Variant、Episode Mapping、Lesson Package、Asset Metadata Schema；
- `additionalProperties: false`；
- 增加有效/无效 examples；
- 增加 Schema validation test/script；
- 对齐 Lin Muen、task hero、Evidence Criteria、Retry/Review、Audio/Transcript 规则。

预计文件：

- `contracts/schemas/v2/*.schema.json`；
- `contracts/examples/v2/**`；
- `scripts/**` 或现有 validation test；
- 必要设计文档索引。

不包含：数据库、API、资源生成。

测试：

- 所有有效 example 通过；
- 缺 task hero、错误 character、未知字段、无 Evidence Criterion、Audio/Transcript 不匹配示例失败；
- 脚本在 Windows/CI 可重复运行。

验收：Schema 可被 Content Import 和 Codex 生成流程直接复用。

### V2-T02 Curriculum / Skill Graph 纵向切片

目标：实现 V19 Curriculum 表、Domain、Repository 和最小查询。

范围：

- V19 migration；
- SkillNode、SkillUnit、Variant、Prerequisite、EvidenceCriterion；
- graph cycle/import business validation；
- JDBC + in-memory repository；
- ArchUnit package 扩展；
- seed/import 最小测试 fixture，不导入完整 72 资源。

预计文件：`tutor-domain/curriculum`、`tutor-application/curriculum`、`tutor-infrastructure/curriculum`、Flyway、tests。

测试：normal、duplicate key、invalid prerequisite、cycle、disabled Skill、DB rollback、architecture。

验收：可按 level/skill/status 查询有效 Variant，Domain 无外层依赖。

### V2-T03 Resource Catalog / Asset 纵向切片

目标：实现 V20 Resource/Collection/Asset 数据与领域规则。

范围：

- content provider、collection、resource、version、asset 与关系表；
- resource version immutable；
- publish status domain model；
- task hero 唯一性/metadata 校验；
- JDBC + in-memory repository；
- 不实现对象上传与发布 API。

测试：相同版本同 hash 幂等、不同 hash conflict、缺 task hero、asset reference、disabled history read、transaction rollback。

验收：Catalog 可保存精确版本并通过 projection 查询 published candidate metadata。

### V2-T04 Experience Graph / Episode Mapping

目标：实现 V21 Season/Episode/Scene/Mapping 与 resolver。

范围：

- V21 migration；
- Experience domain；
- EpisodeMapping resolver；
- fallback、learner fit、eligible level；
- P0 `storyOrderRequired=false` 校验；
- Season 1 最小 EP002/EP006/EP009 fixture。

测试：同 Skill 多 Episode、同 Episode 多 Skill、无映射 fallback、剧情分不覆盖教学 eligibility、invalid resource reference。

验收：给定已选 Variant 可确定性返回最适配 mapping 或明确 no mapping。

### V2-T05 Entitlement / Access Filter

目标：实现 V22 权限表、Domain、Application Service、缓存失效和审计。

范围：

- entitlement migration 和 RBAC permissions；
- grant/revoke/expire/idempotency；
- AccessDecision reason；
- public/admin-granted/admin-only/disabled；
- Redis short-TTL decision cache 与主动失效；
- admin audit。

测试：未授权、过期、重复 grant/revoke、并发 revoke/start decision、Redis failure、DB rollback、权限/归属。

验收：可证明 Access Filter 在排序前使用，Redis 不是真实来源。

### V2-T06 Catalog learner/admin API

目标：将 Resource Catalog 和 Entitlement 暴露为 canonical API。

范围：

- 更新 OpenAPI：learner list/detail/version/media-access；
- admin resource/collection read endpoints；
- Controller/DTO/exception mapping；
- access-before-pagination；
- private object key 不暴露。

测试：OpenAPI lint、Controller unit、public/private/admin、pagination 不泄漏、resource disabled、ownership、error problem。

验收：Web 可安全读取 task hero/Audio metadata；旧 API 不被破坏。

---

## 3. M2：个性化处方

### V2-T07 Pedagogical Policy 基线

目标：实现可重复、版本化的 P0 教学规则。

范围：Prerequisite、Mastery eligibility、Spacing、Difficulty、Interleaving、Transfer、Retry、Completion、Ranking factor normalization。

约束：纯 Domain、显式 Clock、无数据库/LLM。

测试矩阵：

- due review vs new content；
- continuous failure/ease；
- mastered skill only review/upgrade/transfer；
- prerequisite not met；
- time budget；
- output block required；
- deterministic replay；
- Episode continuity cannot override score。

验收：固定 test vectors 产出稳定 reason code 和决策。

### V2-T08 Daily Prescription 后端

目标：扩展 V23 learning_plan/task 并实现 `/prescriptions/today`。

范围：

- V23 migration；
- LearnerSnapshot loader；
- candidate → access → rank → compose → mapping；
- active/superseded version；
- rationale snapshot、fallback、idempotency；
- regeneration/skip feedback；
- OpenAPI/DTO/Controller。

测试：不同 Skill State 不同计划、同输入可重放、无候选、权限过滤、temporary goal、time feedback、concurrent generation、rollback。

验收：首屏读取持久化处方，不调用 LLM；每个 block 有 resourceVersion 和 expected Evidence。

### V2-T09 Web 今日处方

目标：按交互稿实现今日处方主页面。

范围：

- prescription API client/types；
- task hero 卡片、reason、blocks、time；
- 三种用户反馈与重新组合；
- loading/content/empty/error/stale/fallback；
- responsive focalPoint crop；
- 保留现有认证、quota、i18n。

测试：component/model tests、API error、replayed regeneration、task hero alt/crop、keyboard、mobile width。

验收：不同后端 prescription 能直观显示不同教学目标和 Lin Muen 场景。

---

## 4. M3：Scenario Lesson 与主动输出

### V2-T10 Scenario Lesson Session 基线

目标：V24 第一部分，扩展 training_session/task_attempt 并实现 Lesson Session 状态机。

范围：start/resume/get/pause/current step/step completion；开始时二次 AccessDecision；锁定 resourceVersion；OpenAPI。

测试：idempotent start、owner、revoke before start、stale prescription、invalid step transition、resume、optimistic conflict、rollback。

验收：刷新后能从服务端恢复步骤，客户端不能自报完成。

### V2-T11 Web 场景引导与媒体输入

目标：实现 Scene Context、First Listen、Transcript/Expressions 基础界面。

范围：

- scene-specific Lin Muen task hero；
- Audio controls、Transcript hidden-first；
- structured UI overlay；
- image/audio fallback；
- session resume。

测试：task hero required、avatar only rejected in UI fixture、image failure、audio failure、resume、responsive crop、accessibility。

验收：机场任务显示 Lin Muen 位于登机口关系中的情景图，Speaking 区继续保留场景图。

### V2-T12 Comprehension 与 Guided Speaking

目标：实现理解题和文本 Guided Speaking Attempt。

范围：Attempt OpenAPI、客观题确定性评分、text Attempt receipt/status、步骤门禁、Web question/speaking UI。

测试：answer boundary、duplicate idempotency、wrong session/task、ownership、retryable status、Web loading/error/pending。

验收：完成输入、核心理解检查和至少一次 Speaking 才可继续完成。

### V2-T13 Audio Upload / ASR 确认

目标：支持半双工 PTT 录音、受保护存储、ASR 和低置信度确认。

范围：扩展 `/audio/uploads`、audio ownership/retention、ASR use-case port/adapter、confirmation/re-record API、Web PTT state。

测试：格式/大小/时长、other-user asset、provider timeout、low confidence、confirm/correct/re-record、privacy retention、object failure。

验收：低置信度转写未经用户确认不进入评价或 Evidence。

### V2-T14 Role Play SSE

目标：在处方边界内实现多轮 Role Play。

范围：RolePlayResponder、versioned Prompt、SSE events、turn persistence/reconcile、quota、disconnect handling、Web dialogue UI。

测试：role/goal boundary、prompt injection fixture、stream delta/completed/error、disconnect、duplicate turn、quota/provider failure、invalid provider response。

验收：SSE 断开不丢已接受 Attempt，Episode/角色不能改变 target skill。

### V2-T15 Correction / Retry / Attempt 状态

目标：实现结构化分析、聚焦反馈和 Retry Policy。

范围：Attempt V24 fields、SpeakingAttemptAnalyzer、Correction/Natural Expression、Schema/business validation、V26 analysis retry/idempotency tables、retry worker、retry relation。

测试：正常、缺字段、extra field、invalid criterion、timeout、max repair、critical/non-critical error、retry limit、job replay。

验收：AI 失败保留 Attempt；未经校验结果不改变学习状态。

### V2-T16 Evidence / Skill State 原子更新

目标：完成 V24 Evidence 扩展和原子学习状态更新。

范围：Evidence fields/skill relation、EvidenceApplicationService、Mastery/Review signal、Attempt `EVIDENCE_RECORDED`、optimistic retry。

测试：one Attempt one Evidence header、multi-skill relation、retry improvement、duplicate event、rollback、optimistic conflict、Completion != Mastery。

验收：Evidence、Skill/Error/Expression/Review 更新原子或可安全重放。

### V2-T17 Web Feedback / Retry / Completion

目标：实现交互稿反馈页和闭环完成体验。

范围：good/improvement/natural expression、Retry prompt/PTT/text、pending/final failure、Evidence summary、next-plan signal、completion API。

测试：retry required/optional、analysis pending、provider final failure、session incomplete、summary、mobile/a11y。

验收：用户能看到本次改进及对后续计划的影响，不展示虚假的即时 Mastery 跳升。

---

## 5. M4：Memory、Review 与处方闭环

### V2-T18 Error / Expression Memory / Review

目标：实现 V25 三组学习状态。

范围：migration、domain/repositories、Evidence consumer、due query、retention/minimal raw text、tests。

测试：error aggregation、expression state transition、review due、confidence decay、privacy、duplicate Evidence、rollback。

验收：Planner 可读取结构化 weak point、expression transfer 和 due review。

### V2-T19 处方反馈、迁移与闭环

目标：让 Evidence/Review 真正改变下一次处方。

范围：prescription invalidation/signal、cross-scene transfer candidate、user feedback ranking adjustment、recommendation explanation、integration test vectors。

测试：Retry 改善、失败降支架、轻松完成升复杂度、跨 Episode 迁移、到期复习、topic rejection、不重复疲劳。

验收：同一用户前后 Evidence 能造成可解释的处方变化；剧情不强迫重复已掌握基础任务。

---

## 6. M5：内容生产与发布

### V2-T20 内容 Import / Validation Pipeline

目标：实现 V27 import batch/issue 和资源包自动验证。

范围：manifest parser、JSON Schema、reference/business validator、character/task hero/audio checks、batch status 和 import idempotency；复用 V26 通用幂等基础。

测试：有效包、unknown property、bad reference、hash mismatch、wrong Lin Muen、missing/duplicate task hero、Audio/Transcript mismatch、same/different hash import。

验收：非法课程不能进入 publish candidate。

### V2-T21 媒体发布与受保护访问

目标：完成对象存储上传、hash 校验、public CDN/private signed access 和清理补偿。

范围：ObjectStoragePort adapter、MediaAccessPort、upload staging/finalize、short TTL private access、orphan cleanup。

测试：upload failure、hash mismatch、metadata transaction failure、private URL、revoke/cache invalidation、cleanup reference protection。

验收：DB 不引用不存在资产，私有 URL 不能长期绕过 revoke。

### V2-T22 内容管理 API / Script

目标：实现 admin import/validate/publish/unpublish/disable、Collection 和 Entitlement API/script。

范围：OpenAPI、permissions、Controller、audit、If-Match/idempotency、最小 CLI/script；P0 不开发完整 CMS。

测试：admin permission、version conflict、validation failure、duplicate command、audit rollback、publish cache invalidation。

验收：管理员可从标准 package 安全发布和下架精确资源版本。

### V2-T23 B1 24 个资源发布验证

目标：用 8 主题 × 3 场景 × B1 验证完整内容流水线。

范围：生成 manifest/dialogue/tasks/prompts/audio metadata/task heroes、实际媒体、QA report、导入/publish；不是后端代码功能扩张。

测试：Schema、CEFR、Audio/Transcript、task hero character consistency、Evidence Criteria、all references、Catalog query、one E2E per representative theme。

验收：24 个 published B1 Variant 可由 Planner 选择，抽检通过。

### V2-T24 完整 72 个资源发布

目标：补齐 A2/B2 并发布 72 Variant。

范围：难度升级不是替词；完成自动 QA、AI reviewer、人工抽检和发布报告。

验收：24 scenes × 3 levels 全部 published，每个 Variant 有主动输出、Evidence、Retry/Review 和 Lin Muen task hero。

---

## 7. M6：随心学与 Private Collection

### V2-T25 Private Collection / 随心学

目标：实现 EngFluent 作为第一个 `ADMIN_GRANTED` Collection 的目录和进度能力。

前置门禁：实际媒体托管/分发授权已确认；否则只使用合法 mock metadata，不导入受保护内容。

范围：private collection APIs、catalog outline、progress、last position、Web entry/list/detail/player basic state。

测试：no entitlement empty、direct API denied、expired、resource unavailable、progress owner、revoke preservation。

验收：无权限用户没有入口且不能访问；视频进度不当作 Mastery。

### V2-T26 Grant/Revoke Web 与 fallback

目标：补齐管理授权交互、即时撤权和处方公共 fallback。

范围：admin UI/API client、grant/revoke audit view、learner permission-revoked state、unstarted private block replacement。

测试：admin/non-admin、duplicate command、revoke while open、stale media access、history preserved、public loop unaffected。

验收：撤权后新访问立即失败，用户不会卡在不可完成的今日计划。

---

## 8. M7：观测、E2E 与发布

### V2-T27 可观测性、成本与内容指标

目标：建立 V2 核心链路 trace、metrics 和 audit 查询。

范围：prescription/access/ranking/fallback、lesson/media、attempt/schema/retry、evidence/skill、provider usage/cost、content publish 指标；日志脱敏。

测试：metric tags bounded、trace correlation、secret/raw text/signed URL 不泄漏、provider cost aggregation。

验收：能回答“为什么推荐/排除、为什么降级、哪一版本产生 Evidence、每次交互成本”。

### V2-T28 V2 E2E、性能与发布门禁

目标：证明 V2 P0 从处方到 Evidence、Private Collection 和内容发布完整可用。

范围：

- backend full tests / ArchUnit / Flyway Testcontainers；
- Web unit/build/Playwright；
- OpenAPI/Schema/examples；
- 两用户不同处方；
- task hero/Audio/Transcript/Speaking/Retry/Evidence；
- grant/revoke/fallback；
- AI/media/Redis failure；
- zero-Token first screen；
- performance baseline、release checklist、rollback/degradation runbook。

验收：PRD 23 节和架构 17.3 发布门禁全部有自动化或可审计证据；未满足项不得以“后续优化”绕过 P0。

---

## 9. 推荐首批执行顺序

严格依次开始：

```text
V2-T01
 -> V2-T02
 -> V2-T03
 -> V2-T04
 -> V2-T05
 -> V2-T06
 -> V2-T07
 -> V2-T08
 -> V2-T09
```

完成 V2-T09 后进行一次 M1/M2 产品评审，确认“不同用户得到不同处方 + Lin Muen 场景正确展示”，再进入 Scenario Lesson 大规模实现。不要在资源、权限和处方底座未验证前批量生产 72 个正式媒体资源。
