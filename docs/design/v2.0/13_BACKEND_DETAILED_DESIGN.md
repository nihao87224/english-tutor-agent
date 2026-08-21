# English Tutor Agent V2.0 后端详细设计

> 文档版本：`2.0.0`
> 状态：`V2.0 后端实现基线`
> 日期：`2026-08-19`
> 上游：`01_ARCHITECTURE.md`、`12_HIGH_LEVEL_DESIGN.md`

---

## 1. 设计目标

本文定义 V2.0 后端在现有 Java 21 / Spring Boot 4.1 / Maven 模块化单体中的实现边界，包括：

- 领域聚合、值对象与确定性策略；
- Application Service、输入/输出端口与事务；
- REST/SSE、JDBC/Redis/Object Storage、LLM/ASR/TTS 适配关系；
- 处方、课程、Attempt、Retry、Evidence、权限与发布状态机；
- 幂等、并发、补偿、降级、审计与测试要求。

本文给出的类名是目标命名基线。实现任务可在不改变职责和依赖方向的前提下微调，但不得创建跨层快捷依赖。

## 2. 模块依赖

```text
tutor-bootstrap
  -> tutor-api
  -> tutor-agent
  -> tutor-infrastructure
  -> tutor-observability

tutor-api            -> tutor-application
tutor-agent          -> tutor-application
tutor-infrastructure -> tutor-application
tutor-application    -> tutor-domain
tutor-domain         -> JDK only
```

### 2.1 模块实现规则

| Module | 允许 | 禁止 |
|---|---|---|
| domain | entity、value object、policy、domain event | Spring、JDBC、Jackson、Provider SDK |
| application | use case、transaction orchestration、port、command/result | API DTO、数据库实现、agent adapter |
| api | controller、request/response、SSE mapping、CurrentActor | 业务规则、SQL、Provider 调用 |
| agent | Prompt、Provider contract/adapter、AI result parser | 修改业务状态、访问 Controller |
| infrastructure | JDBC、Redis、S3、encryption、job repository | Controller、业务排序规则 |
| bootstrap | bean wiring、security、Flyway、configuration | 领域判断 |

V2 新增 domain package 后必须加入 `ModuleArchitectureTest` 的 domain package 集合。

## 3. 领域模型

### 3.1 Curriculum 聚合

Package：`cn.forever24.tutor.curriculum`

#### SkillNode

```text
SkillNode
- SkillId id
- String name
- SkillCategory category
- Optional<SkillId> parentId
- CefrRange cefrRange
- int importance
- SkillStatus status
```

规则：

- id 稳定且不可复用；
- parent 不允许形成环；
- disabled Skill 不产生新候选，但历史 Evidence 仍可读取。

#### SkillUnit

```text
SkillUnit
- SkillUnitId id
- CommunicationGoal goal
- Set<SkillId> targetSkills
- Set<SkillId> supportingSkills
- Set<Prerequisite> prerequisites
- List<SkillUnitVariant> variants
- ReviewTemplate reviewTemplate
- SemanticVersion version
```

#### SkillUnitVariant

```text
SkillUnitVariant
- VariantId id
- CefrLevel level
- CommunicationComplexity complexity
- Set<TrainingType> trainingTypes
- Set<ScaffoldingLevel> scaffoldingLevels
- Set<ErrorTag> commonErrors
- List<EvidenceCriterion> evidenceCriteria
- CompletionPolicy completionPolicy
- RetryPolicy retryPolicy
- MasteryImpactPolicy masteryImpactPolicy
- DurationRange duration
```

不变量：至少一个 target skill、Training Type 和可观察 Evidence Criterion；Completion Policy 不得直接等同 Mastery。

### 3.2 Resource Catalog 聚合

Package：`cn.forever24.tutor.resource`

#### LearningResource

```text
LearningResource
- ResourceId id
- ProviderCode provider
- CollectionId collectionId
- ResourceType type
- ResourceVersion activeVersion
- AccessScope accessScope
- PublishStatus status
- ResourceMetadata metadata
```

#### ResourceVersion

```text
ResourceVersion
- ResourceId resourceId
- SemanticVersion version
- ContentHash manifestHash
- LearnerFit learnerFit
- Set<VariantId> skillUnitVariants
- Set<EpisodeMappingId> episodeMappings
- List<AssetReference> assets
- Instant createdAt
- Optional<Instant> publishedAt
```

规则：

- `(resourceId, version)` 唯一且发布后不可变；
- 新内容使用新 version；
- activeVersion 只能指向已通过校验的版本；
- disabled/unpublished 不进入新处方；
- 历史 session/evidence 继续引用旧版本。

#### Asset

```text
Asset
- AssetId id
- AssetVersion version
- AssetPurpose purpose
- ObjectKey objectKey
- ContentHash contentHash
- MimeType mimeType
- long byteLength
- AssetMetadata metadata
- AssetStatus status
```

图片 metadata 包含 aspectRatio、shotType、displaySurfaces、focalPoint、altText、characterReferenceIds；音频 metadata 包含 voiceId、speakerRole、accent、speechRate、sourceVersion。

每个可发布 Scenario Lesson Variant 恰好一个 `TASK_HERO`，并至少覆盖 `SCENARIO_INTRO` 与 `SCENARIO_TRAINING`。

#### Collection

```text
Collection
- CollectionId id
- ProviderCode provider
- String title
- AccessScope accessScope
- CollectionStatus status
- LicenseMetadata license
```

第三方私有内容必须拥有独立 Provider、Collection 与 license metadata。

### 3.3 Entitlement 聚合

Package：`cn.forever24.tutor.entitlement`

```text
Entitlement
- EntitlementId id
- UserKey userKey
- CollectionId collectionId
- EntitlementStatus status
- GrantedBy grantedBy
- Instant grantedAt
- Optional<Instant> expiresAt
- Optional<Instant> revokedAt
- long version
```

行为：`grant`、`revoke`、`expire`、`isActiveAt(clock)`。

唯一性：每个 `(userKey, collectionId)` 只有一个当前记录，重复 grant/revoke 必须幂等。

#### AccessDecision

纯领域结果：

```text
AccessDecision
- boolean allowed
- AccessDecisionReason reason
- Optional<CollectionId> collectionId
- Instant evaluatedAt
```

检查顺序固定：resource status → collection status → access scope → entitlement/expiry → ownership/context。

### 3.4 Experience 聚合

Package：`cn.forever24.tutor.experience`

```text
Season
- SeasonId id
- String title
- ExperienceStatus status

Episode
- EpisodeId id
- SeasonId seasonId
- String storyAnchor
- Set<SceneId> scenes
- boolean storyOrderRequired   // P0 必须 false

EpisodeMapping
- EpisodeMappingId id
- VariantId skillUnitVariantId
- EpisodeId episodeId
- SceneId sceneId
- LearnerFit learnerFit
- Set<CefrLevel> eligibleLevels
- StoryTransition transition
- List<AssetReference> assets
- ExperienceFitInputs fitInputs
- Optional<EpisodeMappingId> fallback
```

规则：EpisodeMapping 不能保存或修改 Mastery；找不到映射时必须回退，不得为剧情推进更换 Skill Unit。

### 3.5 Learner Model

扩展 package：`profile`、`learner`、`planning`

#### LearnerSnapshot

只读组合值，由 Application Service 从多个 repository 加载：

```text
LearnerSnapshot
- UserKey userKey
- GoalProfile goals
- CefrEstimate level
- Map<SkillId, UserSkillState> skills
- List<ErrorMemory> errors
- List<ExpressionMemory> expressions
- List<ReviewState> dueReviews
- LearningPreference preference
- DailyConstraint constraint
- Instant asOf
```

它不是单独持久化的大 JSON 事实；处方保存生成时的最小输入摘要和版本用于解释/重放。

#### UserSkillState

沿用现有模型并扩展：masteryScore、confidence、trend、lastPracticedAt、lastEvidenceAt、stateVersion。

规则：

- 只接受 Validated Evidence；
- 长期未使用降低 confidence，不删除历史 mastery；
- 更新使用 optimistic version；
- 评分算法由版本化 `MasteryPolicy` 执行。

### 3.6 DailyLearningPrescription 聚合

Package：`cn.forever24.tutor.planning`

```text
DailyLearningPrescription
- PrescriptionId id
- UserKey userKey
- LocalDate learningDate
- ZoneId learnerZone
- PrescriptionVersion version
- PrescriptionStatus status
- PriorityGoal priorityGoal
- List<PrescriptionBlock> blocks
- List<RecommendationReason> reasons
- PolicyVersion policyVersion
- LearnerInputSnapshot inputSnapshot
- Instant createdAt
- Instant expiresAt
```

```text
PrescriptionBlock
- BlockId id
- BlockType type        // REVIEW/ACQUISITION/OUTPUT/TRANSFER
- VariantId skillUnitVariantId
- ResourceVersionRef resource
- EpisodeMappingId episodeMappingId
- Difficulty difficulty
- ScaffoldingLevel scaffolding
- TrainingType trainingType
- DurationMinutes estimatedMinutes
- List<EvidenceCriterionRef> expectedEvidence
- CompletionPolicy completionPolicy
- Optional<ResourceVersionRef> fallback
- BlockStatus status
```

不变量：

- 至少一个 OUTPUT block；
- 总时长不超出有效 time budget；
- 所有 resource 已通过 AccessDecision；
- 每个 block 有 expected Evidence；
- 同一天可产生新 version，但只有一个 ACTIVE；旧 version 变为 SUPERSEDED；
- Episode continuity 只能作为 experience score，不覆盖教学排序。

### 3.7 LessonSession 聚合

扩展 package：`cn.forever24.tutor.training`

```text
LessonSession
- LessonSessionId id
- UserKey userKey
- PrescriptionId prescriptionId
- BlockId blockId
- ResourceVersionRef resource
- VariantId skillUnitVariantId
- EpisodeMappingId episodeMappingId
- LessonSessionStatus status
- LessonStep currentStep
- Map<TaskId, TaskProgress> tasks
- Instant startedAt
- Optional<Instant> completedAt
- long version
```

状态：

```text
CREATED -> IN_PROGRESS <-> PAUSED -> COMPLETED
                         \-> ABANDONED
```

步骤：

```text
SCENE_CONTEXT -> FIRST_LISTEN -> COMPREHENSION
 -> TRANSCRIPT_EXPRESSIONS -> GUIDED_SPEAKING
 -> ROLE_PLAY -> FEEDBACK -> RETRY -> EVIDENCE -> COMPLETE
```

不是每个课程都强制 Role Play，但至少完成一个 Speaking Task。步骤跳转由 resource completion policy 驱动，不由前端任意指定。

### 3.8 TaskAttempt 聚合

```text
TaskAttempt
- AttemptId id
- LessonSessionId sessionId
- TaskId taskId
- UserKey userKey
- AttemptSequence sequence
- InputType inputType
- Optional<String> textInput
- Optional<AssetId> audioAssetId
- AttemptStatus status
- Optional<AsrTranscript> transcript
- Optional<EvaluationResult> evaluation
- Optional<AttemptId> retryOf
- IdempotencyKey idempotencyKey
- Instant submittedAt
- long version
```

状态：

```text
RECEIVED
 -> TRANSCRIPTION_PENDING -> TRANSCRIPTION_CONFIRMATION_REQUIRED
 -> ANALYSIS_PENDING -> ANALYZED
 -> RETRY_REQUIRED / ACCEPTED
 -> EVIDENCE_RECORDED

任何 pending 状态 -> ANALYSIS_FAILED_RETRYABLE / FAILED_FINAL
```

同一 `(userKey, idempotencyKey, operation)` 只能创建一个 Attempt receipt。

### 3.9 LearningEvidence 与 Review

扩展 `cn.forever24.tutor.learner`

```text
LearningEvidence
- EvidenceId id
- UserKey userKey
- SkillId skillId
- VariantId skillUnitVariantId
- ResourceVersionRef resource
- EpisodeId episodeId
- TaskType taskType
- AttemptId attemptId
- List<CriterionResult> criteria
- EvidenceConfidence confidence
- EvidenceResult result
- Optional<EvidenceId> retryImproves
- Instant occurredAt
- EvidencePolicyVersion policyVersion
```

每个 EvidenceId 和 AttemptId 唯一关联，防止重复影响 Skill State。

ReviewState 记录 skill/expression、dueAt、forgettingRisk、lastRecallQuality、reviewCount 和 policyVersion。

## 4. 确定性策略

Package：对应领域下的 `policy` 或明确业务 package，不能放 Controller。

| Policy | 输入 | 输出 |
|---|---|---|
| PrerequisitePolicy | learner skills、variant prerequisites | eligible/reason |
| MasteryPolicy | previous state、validated evidence | next skill state |
| SpacingPolicy | performance、confidence、history、clock | dueAt/risk |
| DifficultyPolicy | level、recent attempts、failure/ease trend | difficulty/scaffold |
| InterleavingPolicy | due review、target skills、history | ordered blocks |
| TransferPolicy | used expressions、episodes、evidence | transfer candidate |
| RetryPolicy | criterion failures、severity、attempt count | retry/accept/final |
| CompletionPolicy | task progress | session/block completion |
| AccessPolicy | resource/collection/entitlement | AccessDecision |
| PrescriptionRankingPolicy | normalized factors | scored candidate |

所有策略：

- 接收显式 Clock/Instant，不读取系统时间静态方法；
- 有稳定 `policyVersion`；
- 对相同输入返回相同输出；
- 输出 reason code；
- 不调用 LLM、数据库或网络。

## 5. Application Service

Package：`cn.forever24.tutor.application.<capability>`

### 5.1 PrescriptionApplicationService

用例：

- `getOrGenerateTodayPrescription(CurrentActor, RequestContext)`；
- `regeneratePrescription(CurrentActor, PrescriptionFeedbackCommand)`；
- `getPrescription(CurrentActor, PrescriptionId)`。

编排：

1. 计算 learner local date；
2. 获取用户级短锁或数据库唯一键；
3. 返回仍有效 ACTIVE prescription；
4. 加载 LearnerSnapshot；
5. 运行 candidate generation/policy；
6. 调用 AccessQueryPort 过滤；
7. 排序组合；
8. 调用 EpisodeMappingQueryPort；
9. 校验 aggregate invariants；
10. 事务保存 prescription；
11. 可选调用受约束 rationale renderer，不影响处方成功。

### 5.2 ResourceCatalogApplicationService

- 查询当前用户可访问 Catalog；
- 读取精确 ResourceVersion；
- 为 Lesson 解析媒体访问；
- Import draft、validate、publish、unpublish/disable；
- 发布后清理 catalog cache。

公开查询必须只返回 published/available；管理查询可按权限查看 draft/validation issue。

### 5.3 EntitlementApplicationService

- `grant(AdminActor, GrantEntitlementCommand)`；
- `revoke(AdminActor, RevokeEntitlementCommand)`；
- `listForCurrentUser(CurrentActor)`；
- `decide(CurrentActor, ResourceRef)`。

Grant/Revoke 与 Audit 在一个事务中提交；提交后失效 Redis cache。重复命令返回当前状态，不重复审计同一 idempotent operation。

### 5.4 LessonSessionApplicationService

- `startOrResume(CurrentActor, StartLessonCommand)`；
- `getCurrent(CurrentActor, SessionId)`；
- `pause/resume`；
- `submitAttempt`；
- `complete`。

开始时再次执行 AccessDecision，并锁定 resourceVersion。Session 永不自动切换到资源新版本。

### 5.5 AttemptApplicationService

处理文本/客观题 Attempt：

1. 校验 CurrentActor 和 session/task；
2. 根据 idempotency key 查重；
3. 领域创建 RECEIVED Attempt；
4. 客观题本地评分，开放题进入 ANALYSIS_PENDING；
5. 保存 receipt；
6. 触发 AI 分析或持久化重试任务；
7. 返回可轮询/订阅的状态。

处理语音 Attempt：上传流程完成并得到 audioAssetId 后才创建业务 Attempt。低置信度 ASR 不进入最终评价。

### 5.6 ConversationApplicationService

复用现有 SSE 机制并扩展用例级端口：

- `RolePlayResponder`；
- `SpeakingAttemptAnalyzer`；
- `CorrectionAnalyzer`；
- `NaturalExpressionCoach`；
- `RecommendationRationaleRenderer`。

Application 传入明确 Skill、Role、Dialogue Boundary、Evidence Criteria、PromptVersion；Agent adapter 不可自行加载或修改学习状态。

V2 Role Play 每一轮采用 durable-before-provider 顺序：

1. 在 Session 锁定的 `resourceVersion` 中解析 `RolePlayTask`；
2. 校验 CurrentActor、`ROLE_PLAY` 当前步骤、taskId、Skill Unit Variant 和 Episode Mapping；
3. 先以独立幂等键保存 `TaskAttempt`，再保存 owner-scoped `RolePlayTurn(ACCEPTED)`；
4. Audio Attempt 复用低置信度 ASR 确认门禁，未确认前 Turn 为 `AWAITING_TRANSCRIPT`；
5. 配额预留后调用版本化 `RolePlayResponder`，用户原文必须作为 untrusted data 隔离；
6. Provider 输出先验证非空、长度和 trace 元数据，再将完整回复与 Prompt/Provider 版本持久化；
7. 最终 SSE 从已持久化事实映射，断开不得回滚 Attempt/Turn；客户端 GET turns 对账；
8. Provider/配额失败保存 `FAILED_RETRYABLE`，非法输出保存 `FAILED_FINAL`，均不改变 target skill；
9. T14 完成回复后 Attempt 保持 `ANALYSIS_PENDING` 且 Session 仍在 `ROLE_PLAY`，由 T15 的分析/Correction 状态机决定后续步骤。

`RolePlayResponder` 的边界输入必须包含 resource/version、skillUnitVariantId、episodeMappingId、
goal、learner/AI role、success criteria、opening line 和已完成历史。Episode 或角色体验不可覆盖这些字段。

### 5.7 EvidenceApplicationService

输入：已完成业务校验的 `ValidatedAttemptAnalysis`。

事务：

1. 检查 Attempt 未生成 Evidence；
2. 使用 EvidencePolicy 创建 Evidence；
3. 按 target/supporting skill 分配 Evidence；
4. 使用 MasteryPolicy 更新 Skill State；
5. 更新 Error/Expression Memory；
6. 使用 SpacingPolicy 更新 Review；
7. 标记 Attempt `EVIDENCE_RECORDED`；
8. 发出 `EvidenceAccepted` 进程内/持久化事件。

AI 原始结果不能直接进入此服务；必须转换成已验证的 Application DTO。

### 5.8 ContentImportApplicationService

```text
createImportBatch
 -> parse manifest
 -> validate schema
 -> validate business references
 -> verify assets/hash
 -> persist draft + issues
 -> review decision
 -> publish exact version
```

相同 `(resourceId, version, manifestHash)` 重复导入返回原 batch/result。相同版本不同 hash 返回 conflict。

## 6. Application Ports

### 6.1 Repository ports

- `SkillGraphRepository`；
- `SkillUnitRepository`；
- `ResourceCatalogRepository`；
- `AssetMetadataRepository`；
- `CollectionRepository`；
- `EntitlementRepository`；
- `PrescriptionRepository`；
- `EpisodeRepository` / `EpisodeMappingRepository`；
- `LessonSessionRepository`；
- `TaskAttemptRepository`；
- `RolePlayTurnRepository`；
- `LearningEvidenceRepository`；
- `ErrorMemoryRepository`；
- `ExpressionMemoryRepository`；
- `ReviewStateRepository`；
- `ImportBatchRepository`；
- `RetryJobRepository`；
- `AuditRepository`（复用现有）。

Repository 返回领域对象或专用 projection，不返回 `ResultSet`、JPA entity 或 API DTO。

### 6.2 External ports

- `ObjectStoragePort`：put/stat/delete；
- `MediaAccessPort`：public URL 或 short-lived private access；
- `AudioTranscriber`：用例级 ASR；
- `LessonSpeechSynthesizer`：用例级 TTS；
- `RolePlayResponder`；
- `SpeakingAttemptAnalyzer`；
- `ContentDraftGenerator` / `ContentReviewer`（离线工具使用）；
- `Clock`；
- `IdGenerator`；
- `TransactionRunner` 或 Spring transaction boundary（实现层）。

低层 Chat/ASR/TTS Provider interface 继续位于 `tutor-agent`，Application 只依赖上述用例级接口。

## 7. Adapter 设计

### 7.1 tutor-api

每个 Controller：

- 解析 CurrentActor；
- 执行 Bean Validation；
- 读取 `Idempotency-Key`；
- 调用一个 Application use case；
- 将 domain/application result 映射为 response；
- 不捕获后静默吞掉业务错误。

SSE endpoint 只负责 event mapping、heartbeat 和断开清理；流式过程中产生的最终业务结果仍由 Application 持久化。

### 7.2 tutor-infrastructure

JDBC adapter：

- 显式 SQL 和 row mapper；
- 用户查询总是包含 user key；
- optimistic version 使用条件更新并检查 affected rows；
- 批量 Candidate 查询避免 N+1；
- Flyway 仅前向迁移。

Redis adapter：

- cache miss 回源；
- key 带 schema/version 前缀；
- Entitlement revoke 主动删除 user/collection access key；
- Redis 异常不改变长期事实。

Object Storage adapter：

- object key 不使用用户原始文件名；
- 上传后验证 length/hash；
- private asset 默认禁止匿名读取；
- URL 签发记录 resource/actor/expiry trace，但不记录完整 URL token。

### 7.3 tutor-agent

```text
Application use-case port
 -> Prompt assembler (versioned)
 -> project-owned provider contract
 -> protocol adapter
 -> raw response
 -> parser
 -> JSON Schema validator
 -> business-shape validator
 -> application result
```

Provider 路由从数据库配置读取；切换 default 在下一请求生效。Provider timeout、rate limit、auth、invalid response、content refusal 使用稳定错误分类。

## 8. 关键时序

### 8.1 生成处方

```text
API -> PrescriptionApp: getOrGenerate(actor, date)
PrescriptionApp -> Repository: findActive
alt missing/stale
  PrescriptionApp -> Learner repositories: load snapshot
  PrescriptionApp -> Policies: decide skill/difficulty/review
  PrescriptionApp -> Catalog: candidate projection
  PrescriptionApp -> Access: filter
  PrescriptionApp -> RankingPolicy: rank/compose
  PrescriptionApp -> Experience: resolve mapping
  PrescriptionApp -> Repository: save active version
end
API <- PrescriptionApp: prescription response
```

### 8.2 Speaking Attempt

```text
API -> AttemptApp: submit(command, idempotencyKey)
AttemptApp -> SessionRepo: authorize/load
AttemptApp -> AttemptRepo: findByIdempotencyKey
AttemptApp -> AttemptRepo: save RECEIVED/ANALYSIS_PENDING
API <- AttemptApp: 202 attempt receipt

Worker/App -> SpeakingAnalyzer: analyze bounded input
SpeakingAnalyzer -> Provider: structured request
Provider --> SpeakingAnalyzer: response
SpeakingAnalyzer: parse/schema/business validate
Worker/App -> AttemptRepo: save ANALYZED
Worker/App -> EvidenceApp: accept validated analysis
EvidenceApp -> repositories: atomic evidence/state/review update
```

同步实现允许在响应等待预算内直接分析；无论同步或异步，Attempt 必须先保存，且 API 状态语义一致。

### 8.3 Revoke

```text
Admin API -> EntitlementApp: revoke
EntitlementApp -> EntitlementRepo: lock/find
EntitlementApp -> Entitlement: revoke
EntitlementApp -> AuditRepo: append
EntitlementApp -> transaction: commit
EntitlementApp -> Redis: invalidate
EntitlementApp -> PrescriptionFallback: mark affected unstarted blocks
```

## 9. 事务、并发与幂等

### 9.1 事务

| 用例 | 原子范围 |
|---|---|
| prescription generation | prescription + blocks + reasons + active version switch |
| attempt receipt | attempt + idempotency record + session task progress |
| evidence acceptance | evidence + skill/error/expression/review updates + attempt status |
| entitlement mutation | entitlement + audit |
| resource publish | version status + active version + audit/outbox |

外部 Provider/Object Storage 调用不包在长数据库事务中。

### 9.2 Idempotency

写 API 使用：

```text
(actorId, operation, idempotencyKey)
 -> requestHash
 -> responseStatus/responseReference
 -> expiresAt
```

相同 key + 相同 requestHash 返回原结果；相同 key + 不同 requestHash 返回 `IDEMPOTENCY_CONFLICT`。

### 9.3 Locking

- 当日处方依赖数据库唯一键和短用户锁，锁失败后重新查询；
- Skill State 用 optimistic version；
- Entitlement grant/revoke 可按唯一行 `SELECT ... FOR UPDATE`；
- 发布 active version 使用资源级 optimistic version；
- 不使用 Redis 锁作为唯一数据正确性保障。

## 10. AI 分析重试任务

```text
AnalysisRetryJob
- JobId
- AttemptId
- JobType
- JobStatus
- attemptCount
- nextRunAt
- lastErrorCategory
- leaseOwner/leaseUntil
- createdAt/updatedAt
```

Worker 规则：

- 原子 claim 到期 job；
- 指数退避并有最大次数；
- 只对 retryable error 重试；
- 执行前再次检查 Attempt 状态；
- 成功后幂等写 Analysis/Evidence；
- final failure 保留 Attempt，用户可主动重新分析或重新回答；
- 不在日志记录完整用户原文。

## 11. 错误模型

稳定业务错误类别：

- `AUTHENTICATION_REQUIRED`；
- `ACCESS_DENIED` / `ENTITLEMENT_REQUIRED` / `ENTITLEMENT_REVOKED`；
- `RESOURCE_NOT_PUBLISHED` / `RESOURCE_UNAVAILABLE` / `ASSET_UNAVAILABLE`；
- `PRESCRIPTION_STALE` / `PRESCRIPTION_NO_CANDIDATE`；
- `SESSION_STATE_CONFLICT` / `TASK_STATE_CONFLICT`；
- `IDEMPOTENCY_CONFLICT`；
- `ASR_CONFIRMATION_REQUIRED`；
- `AI_CONFIGURATION_REQUIRED`；
- `AI_TEMPORARILY_UNAVAILABLE` / `AI_OUTPUT_INVALID`；
- `VALIDATION_FAILED`；
- `OPTIMISTIC_LOCK_CONFLICT`。

API 层映射 HTTP status 和 problem details；领域层不认识 HTTP。

## 12. 配置

应用配置：

- 数据库、Redis、对象存储连接；
- media access TTL；
- job poll/lease/retry 上限；
- upload size/audio duration 上限；
- SSE heartbeat 和最大会话时长；
- feature flags（仅用于渐进发布，不替代权限）。

AI provider endpoint/model/default/timeout/key 全部来自数据库，不提供环境变量 fallback；环境变量只保存基础设施和系统解密密钥。

## 13. 可观测性

Application Service 在用例入口创建业务 span，附加：

- actor、resource/prescription/session/attempt 的内部 ID；
- rule/prompt/resource version；
- access/ranking/fallback reason code；
- provider/model/latency/usage/error category；
- validation stage；
- retry job attempt。

禁止：API key、authorization header、private signed URL、完整录音或不必要用户原文。

## 14. 测试详细要求

### 14.1 Domain

- 每个 Policy 正常、边界、无候选、连续失败、连续轻松、到期复习、迁移；
- aggregate 非法状态跳转；
- Completion 与 Mastery 分离；
- Episode continuity 不覆盖教学结果；
- Entitlement expiry/revoke；
- Retry/Evidence 去重。

### 14.2 Application

- ownership/access；
- 相同 idempotency key；
- Provider timeout/invalid JSON/config missing；
- DB failure rollback；
- optimistic conflict；
- revoke 与 start 竞争；
- public fallback；
- Attempt 先保存、AI 后失败；
- Evidence 原子更新。

### 14.3 Adapter

- JDBC mapping、索引命中和 user isolation；
- Redis unavailable；
- Object Storage hash mismatch/private URL；
- Provider protocol、timeout、usage/trace；
- Flyway 从当前 V18 前向升级空库和已有数据样本。

### 14.4 Architecture

- Domain 无 Spring/Jackson/JDBC/AI；
- Application 无 API/Agent/Infrastructure；
- API 不依赖 adapters；
- Agent/Infrastructure 彼此不依赖；
- 新 package 纳入 ArchUnit。

## 15. 实施约束

- 不一次创建所有 package 空壳；按纵向任务逐步增加；
- 先冻结 OpenAPI/Schema/DDL，再实现接口和 persistence；
- 每次 Flyway 只前向增加，不修改已发布 migration；
- 测试默认 stub/mock Provider；
- 每个任务至少覆盖正常、边界、权限、外部失败、幂等、非法 AI 输出和事务回滚；
- 详细表结构和端点以 `14_API_AND_DATABASE_DESIGN.md` 为准。
