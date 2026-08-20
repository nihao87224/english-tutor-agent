# English Tutor Agent V2.0 API 与数据库设计

> 文档版本：`2.0.0`
> 状态：`V2.0 契约设计基线`
> 日期：`2026-08-19`
> 上游：`12_HIGH_LEVEL_DESIGN.md`、`13_BACKEND_DETAILED_DESIGN.md`

---

## 1. 设计范围

本文冻结 V2.0 P0 的：

- REST/SSE 资源、路径、方法、状态码、幂等和错误语义；
- 核心请求/响应对象；
- MySQL 表、主外键、唯一约束、索引和迁移顺序；
- API 对数据所有权和事务的映射。

正式实现仍须同步更新：

- `contracts/openapi/english-tutor-api.yaml`；
- `contracts/schemas/*.schema.json`；
- Flyway `V19+` migration；
- API examples、repository tests 和 integration tests。

本文不是机器契约，若后续 OpenAPI/Schema 与本文不一致，必须先更新并评审本文，不能静默偏离。

## 2. API 约定

### 2.1 基础规则

- Base path：`/api/v1`。产品 V2 不等于 API `/v2`；避免无必要破坏现有认证和平台端点；
- Content type：JSON 使用 `application/json`，Problem 使用 `application/problem+json`，SSE 使用 `text/event-stream`；
- 时间：ISO-8601 UTC instant，例如 `2026-08-19T08:30:00Z`；
- 日期：学习者本地日期 `YYYY-MM-DD`，响应同时返回 `timezone`；
- 外部 ID：稳定 opaque string，客户端不得推断数据库 BIGINT；
- 枚举：大写 `UPPER_SNAKE_CASE`；
- 分页：cursor-based，`limit` 默认 20、最大 100；
- 写命令：使用 `Idempotency-Key`，最大 128 字符；
- 乐观锁：管理更新使用请求字段或 `If-Match` 中的 version；
- CurrentActor：learner endpoint 不接受目标 userId；
- API 不返回 Provider secret、内部 SQL ID 或私有 object key。

### 2.2 兼容策略

V2 新增 canonical endpoint：

- `/prescriptions/today`；
- `/lesson-sessions`；
- `/learning-resources`；
- `/private-collections`。

现有 `/plans/today` 与 `/training-sessions` 在 Web 迁移期保留，仅服务 V1.x DTO。新 V2 Web 不混用两组状态；V2 E2E 通过后标记旧端点 deprecated，后续单独任务删除，不在本阶段破坏兼容。

## 3. 通用响应

### 3.1 Problem Details

```json
{
  "type": "https://english-tutor/errors/entitlement-revoked",
  "title": "Private collection access was revoked",
  "status": 403,
  "code": "ENTITLEMENT_REVOKED",
  "detail": "This resource is no longer available to the current learner.",
  "instance": "/api/v1/learning-resources/res_123",
  "traceId": "trc_123",
  "retryable": false,
  "fields": []
}
```

稳定错误码：

| HTTP | Code |
|---|---|
| 400 | `VALIDATION_FAILED`, `ASR_CONFIRMATION_REQUIRED` |
| 401 | `AUTHENTICATION_REQUIRED`, `AUTHENTICATION_EXPIRED` |
| 403 | `ACCESS_DENIED`, `ENTITLEMENT_REQUIRED`, `ENTITLEMENT_REVOKED` |
| 404 | `RESOURCE_NOT_FOUND`, `PRESCRIPTION_NOT_FOUND`, `SESSION_NOT_FOUND`, `ATTEMPT_NOT_FOUND` |
| 409 | `IDEMPOTENCY_CONFLICT`, `PRESCRIPTION_STALE`, `SESSION_STATE_CONFLICT`, `TASK_STATE_CONFLICT`, `VERSION_CONFLICT` |
| 410 | `RESOURCE_DISABLED`, `MEDIA_ACCESS_EXPIRED` |
| 422 | `RESOURCE_VALIDATION_FAILED`, `AI_OUTPUT_INVALID` |
| 429 | `QUOTA_EXCEEDED`, `RATE_LIMITED` |
| 503 | `AI_CONFIGURATION_REQUIRED`, `AI_TEMPORARILY_UNAVAILABLE`, `ASSET_TEMPORARILY_UNAVAILABLE` |

### 3.2 Page

```json
{
  "items": [],
  "nextCursor": null,
  "hasMore": false
}
```

### 3.3 Idempotency

适用：create/regenerate prescription、start session、submit attempt、grant/revoke、import/publish。

- 第一次请求：正常处理，响应可包含 `Idempotency-Replayed: false`；
- 重复相同 key + request hash：返回原业务结果与 `Idempotency-Replayed: true`；
- 重复 key + 不同 request hash：409 `IDEMPOTENCY_CONFLICT`；
- key 只在 actor + operation 范围内唯一。

## 4. Prescription API

### 4.1 GET `/api/v1/prescriptions/today`

读取或确定性生成当前用户今日处方。

Query：

- `date`：可选，本地学习日期；P0 只允许今天；
- `timezone`：可选，仅首次或与 Profile 一致时接受；默认 Profile timezone。

Response `200 DailyLearningPrescriptionResponse`：

```json
{
  "prescriptionId": "prx_01",
  "version": 3,
  "learningDate": "2026-08-19",
  "timezone": "Asia/Shanghai",
  "status": "ACTIVE",
  "priorityGoal": {
    "code": "TRAVEL_COMMUNICATION",
    "label": "新加坡旅行沟通"
  },
  "rationale": "你的旅行目标优先，确认信息技能今天到期复习。",
  "reasonCodes": ["GOAL_MATCH", "REVIEW_DUE", "ERROR_MATCH"],
  "estimatedMinutes": 20,
  "experience": {
    "seasonId": "S01",
    "episodeId": "EP006",
    "sceneId": "airport_gate_change",
    "title": "Airport Adventure"
  },
  "blocks": [
    {
      "blockId": "blk_01",
      "sequence": 1,
      "type": "OUTPUT",
      "title": "确认并复述机场信息",
      "skillUnitVariantId": "travel.confirm_information.b1",
      "resource": {
        "resourceId": "season1.ep006.gate_change.b1",
        "resourceVersion": "1.0.0"
      },
      "episodeMappingId": "map_ep006_confirm_b1",
      "difficulty": "B1",
      "scaffolding": "MEDIUM",
      "trainingType": "ROLE_PLAY",
      "estimatedMinutes": 8,
      "expectedEvidence": ["confirm_location", "confirm_time", "restate_information"],
      "taskHero": {
        "assetId": "asset_task_hero_01",
        "url": "https://cdn.example.invalid/...",
        "aspectRatio": "16:9",
        "focalPoint": {"x": 0.68, "y": 0.42},
        "altText": "Lin Muen stands near an airport gate and checks the changed boarding information."
      },
      "status": "READY"
    }
  ],
  "generatedAt": "2026-08-19T00:10:00Z",
  "expiresAt": "2026-08-19T16:00:00Z"
}
```

No candidate：不返回空 200；返回 409 `PRESCRIPTION_NO_CANDIDATE`，并附公共通用对话 fallback 是否可用。

### 4.2 POST `/api/v1/prescriptions/today/regenerations`

用途：用户反馈太难、太简单、时间不足、不想练主题、临时目标后重新组合。

Header：`Idempotency-Key` required。

Request：

```json
{
  "currentPrescriptionId": "prx_01",
  "currentVersion": 3,
  "reason": "TIME_INSUFFICIENT",
  "availableMinutes": 10,
  "temporaryGoal": null,
  "note": null
}
```

`reason`：`TOO_HARD`、`TOO_EASY`、`TIME_INSUFFICIENT`、`TOPIC_REJECTED`、`TEMPORARY_GOAL`。

Response：`201` 新 version；旧 version `SUPERSEDED`。若 currentVersion 过期，409 `PRESCRIPTION_STALE` 并返回 current reference。

### 4.3 POST `/api/v1/prescriptions/{prescriptionId}/blocks/{blockId}/skips`

记录跳过原因并返回替代 block 或剩余处方。Header `Idempotency-Key` required。

## 5. Learning Resource API

### 5.1 GET `/api/v1/learning-resources`

只返回当前用户可访问且 published/available 的资源。

Query：`type`、`collectionId`、`topic`、`scene`、`level`、`skillId`、`cursor`、`limit`。

Access filter 必须在分页前执行，避免 page size 或 total 暴露私有资源存在。

### 5.2 GET `/api/v1/learning-resources/{resourceId}`

返回 active version 的用户可见详情，包括 learner fit、skill mappings、Episode context、媒体 metadata，不返回生成 Prompt 全文、private object key 或 admin license note。

### 5.3 GET `/api/v1/learning-resources/{resourceId}/versions/{version}`

仅用于恢复历史 session；普通用户只有其合法 session/evidence 引用该版本时可读取学习所需内容。管理员可按权限读取任意版本。

### 5.4 POST `/api/v1/learning-resources/{resourceId}/media-access`

Header `Idempotency-Key` required for private media，public asset 可直接返回 CDN URL。

Request：

```json
{
  "assetId": "asset_01",
  "purpose": "PLAYBACK"
}
```

Response：

```json
{
  "assetId": "asset_01",
  "url": "https://object.example.invalid/signed/...",
  "expiresAt": "2026-08-19T08:40:00Z",
  "mimeType": "audio/mpeg",
  "contentHash": "sha256:..."
}
```

每次签发重新检查 Entitlement。客户端不得缓存超过 expiresAt。

## 6. Lesson Session API

### 6.1 POST `/api/v1/lesson-sessions`

Header `Idempotency-Key` required。

Request：

```json
{
  "prescriptionId": "prx_01",
  "prescriptionVersion": 3,
  "blockId": "blk_01",
  "inputMode": "VOICE_OR_TEXT"
}
```

Response `201` 或 replay `200`：

```json
{
  "sessionId": "lsn_01",
  "status": "IN_PROGRESS",
  "resource": {"resourceId": "season1.ep006.gate_change.b1", "resourceVersion": "1.0.0"},
  "skillUnitVariantId": "travel.confirm_information.b1",
  "episodeMappingId": "map_ep006_confirm_b1",
  "currentStep": "SCENE_CONTEXT",
  "step": {},
  "progress": {"completedSteps": 0, "totalRequiredSteps": 7},
  "version": 1
}
```

开始前复检 resource/entitlement；若处方失效但有 fallback，409 `PRESCRIPTION_STALE` 返回 replacement block，不静默替换正在开始的请求。

### 6.2 GET `/api/v1/lesson-sessions/{sessionId}`

返回 session summary、currentStep、step payload、Attempt pending 状态和媒体 fallback。只允许 owner 或管理员审计权限。

### 6.3 POST pause/resume

- `/api/v1/lesson-sessions/{sessionId}/pause`
- `/api/v1/lesson-sessions/{sessionId}/resume`

Header `Idempotency-Key` required。返回更新后 session；非法状态 409。

### 6.4 POST `/api/v1/lesson-sessions/{sessionId}/steps/{stepId}/completions`

仅用于无需 Attempt 的确定性步骤，例如确认完成 First Listen/查看必要内容。后端根据 resource policy 判断能否完成，不接受客户端自报完成整个课程。

### 6.5 POST `/api/v1/lesson-sessions/{sessionId}/completions`

Header `Idempotency-Key` required。

只有 CompletionPolicy 满足且 required Evidence 已记录才完成；否则 409 `SESSION_STATE_CONFLICT`，返回 missing requirements。

## 7. Attempt、ASR 与 Feedback API

### 7.1 复用 Audio Upload

继续使用现有 `POST /api/v1/audio/uploads`，扩展 response：

```json
{
  "audioAssetId": "usr_audio_01",
  "uploadStatus": "READY",
  "mimeType": "audio/webm",
  "durationMs": 18200,
  "contentHash": "sha256:..."
}
```

服务端校验 owner、格式、时长、大小和 hash。上传成功不计作 Attempt。

### 7.2 POST `/api/v1/lesson-sessions/{sessionId}/attempts`

Header `Idempotency-Key` required。

Request：

```json
{
  "taskId": "task_roleplay_01",
  "inputType": "AUDIO",
  "text": null,
  "audioAssetId": "usr_audio_01",
  "retryOfAttemptId": null,
  "clientStartedAt": "2026-08-19T08:20:00Z",
  "clientDurationMs": 18200
}
```

约束：TEXT 时 text required；AUDIO 时 audioAssetId required 且必须属于当前 actor；retryOf 必须属于同 session/task。

Response `202`：

```json
{
  "attemptId": "att_01",
  "status": "ANALYSIS_PENDING",
  "submittedAt": "2026-08-19T08:20:19Z",
  "pollAfterMs": 800,
  "version": 1
}
```

客观题可同步返回 `200 ANALYZED`。

### 7.3 GET `/api/v1/lesson-sessions/{sessionId}/attempts/{attemptId}`

Response：

```json
{
  "attemptId": "att_01",
  "taskId": "task_roleplay_01",
  "status": "RETRY_REQUIRED",
  "transcript": {
    "text": "Just to confirm...",
    "confidence": 0.91,
    "confirmationRequired": false
  },
  "feedback": {
    "taskCompletion": "PARTIAL",
    "good": ["You confirmed the new gate."],
    "improvements": ["Restate the boarding time as well."],
    "naturalExpression": "Just to confirm, the new gate is A17 and boarding still starts at 9:20, right?",
    "criterionResults": [
      {"criterionId": "confirm_location", "result": "MET", "confidence": 0.96},
      {"criterionId": "confirm_time", "result": "NOT_MET", "confidence": 0.94}
    ]
  },
  "retry": {
    "required": true,
    "prompt": "Confirm both the gate and boarding time.",
    "remainingAttempts": 2
  },
  "evidenceIds": [],
  "version": 3
}
```

### 7.4 POST `/api/v1/lesson-sessions/{sessionId}/attempts/{attemptId}/transcript-confirmations`

用于低置信度 ASR。

```json
{
  "decision": "CONFIRM",
  "correctedText": "..."
}
```

`decision`：`CONFIRM`、`CORRECT`、`RE_RECORD`。RE_RECORD 关闭当前 Attempt 且不产生 Evidence。

### 7.5 SSE `/api/v1/lesson-sessions/{sessionId}/role-play/messages/stream`

POST + `Accept: text/event-stream`，Header `Idempotency-Key` required。

Request：taskId、text/audioAssetId、conversationTurnId。

Event：

```text
event: turn.accepted
data: {"attemptId":"att_02","turnId":"turn_03"}

event: reply.delta
data: {"sequence":1,"text":"Let me check"}

event: reply.completed
data: {"turnId":"turn_03","messageId":"msg_04"}

event: analysis.pending
data: {"attemptId":"att_02"}

event: stream.error
data: {"code":"AI_TEMPORARILY_UNAVAILABLE","retryable":true,"traceId":"trc_01"}
```

SSE 断开不撤销已接受 Attempt；客户端通过 GET Attempt/Session 对账。heartbeat 使用 comment，不作为业务 event。

## 8. Private Collection API

### 8.1 GET `/api/v1/private-collections`

只返回有效 Entitlement 的 Collection。无权限返回 `200 items=[]`，客户端隐藏一级入口。

### 8.2 GET `/api/v1/private-collections/{collectionId}`

后端复检；返回 catalog outline、license display、progress summary。

### 8.3 GET/PUT Progress

- `GET /api/v1/private-collections/{collectionId}/resources/{resourceId}/progress`
- `PUT /api/v1/private-collections/{collectionId}/resources/{resourceId}/progress`

PUT Header `Idempotency-Key` required，保存 last position/completion，不把视频播放进度当作 Skill Mastery。

## 9. Admin Resource API

权限新增：

- `RESOURCE_READ`、`RESOURCE_MANAGE`、`RESOURCE_PUBLISH`；
- `COLLECTION_READ`、`COLLECTION_MANAGE`；
- `ENTITLEMENT_READ`、`ENTITLEMENT_MANAGE`。

### 9.1 Import

- `POST /api/v1/admin/resource-imports`：创建/import package；
- `GET /api/v1/admin/resource-imports/{importId}`：状态和 validation issues；
- `POST /api/v1/admin/resource-imports/{importId}/validations`：重新验证。

P0 接受 manifest JSON + 已上传 asset references，不在 JSON API 中传大媒体 base64。

### 9.2 Resource lifecycle

- `GET /api/v1/admin/learning-resources`；
- `GET /api/v1/admin/learning-resources/{resourceId}`；
- `POST /api/v1/admin/learning-resources/{resourceId}/versions/{version}/publications`；
- `DELETE /api/v1/admin/learning-resources/{resourceId}/versions/{version}/publication`；
- `POST /api/v1/admin/learning-resources/{resourceId}/disabling`。

Publish Header `Idempotency-Key` 和 `If-Match` required；校验失败返回 422 + issue summary。

### 9.3 Collection

- `POST /api/v1/admin/collections`；
- `PATCH /api/v1/admin/collections/{collectionId}`；
- `PUT /api/v1/admin/collections/{collectionId}/resources/{resourceId}`；
- `DELETE /api/v1/admin/collections/{collectionId}/resources/{resourceId}`。

### 9.4 Entitlement

- `POST /api/v1/admin/collections/{collectionId}/entitlements`；
- `GET /api/v1/admin/collections/{collectionId}/entitlements`；
- `DELETE /api/v1/admin/collections/{collectionId}/entitlements/{userKey}`。

Grant request：

```json
{
  "userKey": "usr_01",
  "expiresAt": null,
  "reason": "Purchased access verified"
}
```

Grant/Revoke Header `Idempotency-Key` required，并写入 admin audit。

## 10. Schema 与字段校验

### 10.1 AI structured output

所有影响 Evidence 的 Schema：

- `additionalProperties: false`；
- required 明确；
- enum 封闭；
- score/confidence 范围 `[0,1]`；
- criterionId 必须来自 task snapshot；
- 最多 3 个 material improvements；
- 不接受模型输出 userId、mastery delta、review dueAt 或 access decision。

### 10.2 Course manifest

机器 Schema 至少拆分：

- `learning-resource-manifest.schema.json`；
- `skill-unit-variant.schema.json`；
- `episode-mapping.schema.json`；
- `lesson-package.schema.json`；
- `asset-metadata.schema.json`。

Course manifest 的 `character` 必须为 `Lin Muen`；每个发布变体恰好一个 task hero；Audio script 与 Transcript 引用相同 sentence IDs。

## 11. 数据库通用约定

- MySQL 8.4、InnoDB、utf8mb4；
- 内部主键 `BIGINT AUTO_INCREMENT`，外部 key `VARCHAR(64)` unique；
- 时间 `DATETIME(3)` UTC，命名 `*_at_utc`；
- 乐观锁字段 `version BIGINT NOT NULL DEFAULT 0`；
- boolean 使用 `BOOLEAN`；
- score/confidence 使用 `DECIMAL(6,5)` + CHECK；
- JSON 仅保存不可变快照、低查询频率 metadata 或 extensible payload；权限、状态、版本和查询键必须使用普通列；
- 用户数据表包含 `user_id` 外键并建立 user-scoped index；
- 不保存媒体 BLOB；只保存 object key/hash/metadata；
- 不修改 V1–V18 migration；V2 从 V19 前向增加。

## 12. 迁移规划

| Migration | 内容 |
|---|---|
| V19 | Curriculum / Skill Graph / Skill Unit |
| V20 | Content Provider / Collection / Resource / Asset |
| V21 | Season / Episode / Scene / Episode Mapping |
| V22 | Entitlement + RBAC permissions |
| V23 | V2 Prescription 扩展现有 learning_plan/task |
| V24 | Scenario Session / Attempt / Evidence 扩展 |
| V25 | Error / Expression Memory / Review State |
| V26 | Analysis Retry Job / General API Idempotency |
| V27 | Content Import / Validation |

实现前必须再次检查远端最高 migration；若已被占用，顺延版本号，不能改写远端 migration。

## 13. V19 Curriculum 表

### `curriculum_skill`

| Column | Type | Constraint |
|---|---|---|
| id | BIGINT | PK auto |
| skill_key | VARCHAR(128) | unique not null |
| name | VARCHAR(160) | not null |
| category | VARCHAR(64) | not null |
| cefr_min/max | VARCHAR(8) | not null |
| importance | INT | 0–100 |
| status | VARCHAR(32) | ACTIVE/DISABLED |
| created/updated_at_utc | DATETIME(3) | not null |
| version | BIGINT | optimistic |

Indexes：`(category,status)`。

### `curriculum_skill_edge`

`parent_skill_id`、`child_skill_id`、`edge_type`；PK `(parent_skill_id, child_skill_id, edge_type)`；禁止 self edge，环由 import business validation 检查。

### `curriculum_skill_unit`

`skill_unit_key VARCHAR(160) unique`、communication_goal、review_template_json、semantic_version、status、timestamps、version。

### `curriculum_skill_unit_variant`

`variant_key VARCHAR(192) unique`、skill_unit_id FK、cefr_level、communication_complexity、estimated_min/max、training_types_json、scaffolding_levels_json、common_error_tags_json、completion_policy_json、retry_policy_json、mastery_policy_json、status、version。

Indexes：`(cefr_level,status)`、`(skill_unit_id,status)`。

### 关系表

- `curriculum_variant_target_skill(variant_id, skill_id, role)`；
- `curriculum_variant_prerequisite(variant_id, skill_id, minimum_mastery, minimum_confidence)`；
- `curriculum_evidence_criterion(id, criterion_key, variant_id, description, weight, required, sequence_no)`。

## 14. V20 Resource 表

### `content_provider`

`provider_code VARCHAR(64) PK`、display_name、provider_type (`INTERNAL/THIRD_PARTY`)；不要与 `ai_provider_config` 混用。

### `resource_collection`

`id BIGINT PK`、`collection_key VARCHAR(64) unique`、provider_code FK、title、access_scope、status、source_url、ownership_type、license_note、allowed_audience、admin_note、timestamps、version。

Indexes：`(access_scope,status)`、`(provider_code,status)`。

### `learning_resource`

`id`、resource_key unique、provider_code、collection_id、resource_type、title、description、language、level、topic、scene、communication_goal、access_scope、publish_status、active_version_id nullable、estimated_minutes、created/updated/version。

Indexes：

- `(publish_status, access_scope, level)`；
- `(topic, scene, level, publish_status)`；
- `(collection_id, publish_status)`。

### `learning_resource_version`

`id`、resource_id FK、semantic_version、manifest_hash、manifest_json、learner_fit_json、generation_metadata_json、created_at、published_at、status、version；unique `(resource_id, semantic_version)`。

### `learning_asset`

`id`、asset_key external unique、asset_version、purpose、object_key、content_hash、mime_type、byte_length、access_scope、metadata_json、status、created_at；unique `(object_key, content_hash)`，index `(purpose,status)`。

### 关系表

- `resource_version_asset(resource_version_id, asset_id, display_order)`；
- `resource_version_skill_variant(resource_version_id, variant_id)`；
- `collection_resource(collection_id, resource_id, display_order, status)`。

发布时校验 active_version FK 后再更新 `learning_resource.active_version_id`。

## 15. V21 Experience 表

### `experience_season`

season_key unique、title、status、metadata_json、timestamps/version。

### `experience_episode`

episode_key unique、season_id FK、title、story_anchor、story_order_required BOOLEAN default false、status、metadata_json、sequence_no（仅展示，不是教学顺序）。

### `experience_scene`

scene_key unique、episode_id FK、title、location、story_context、character_state_json、status。

### `episode_mapping`

mapping_key unique、variant_id FK、episode_id FK、scene_id FK、eligible_levels_json、learner_fit_json（目标标签与禁忌条件）、story_transition_json、fit_inputs_json（确定性体验匹配输入）、fallback_mapping_id nullable、status、timestamps/version。fallback 必须保持同一 Skill Unit Variant，且 P0 resolver 必须先执行 Variant、eligible level 与禁忌条件过滤，剧情连续性只能对合格候选作次级排序。

Indexes：`(variant_id,status)`、`(episode_id,scene_id,status)`。

### `episode_mapping_resource`

mapping_id、resource_version_id、priority；PK `(mapping_id,resource_version_id)`，引用精确且不可变的 Resource Version。

## 16. V22 Entitlement 表

### `user_collection_entitlement`

| Column | Type | Constraint |
|---|---|---|
| id | BIGINT | PK auto |
| entitlement_key | VARCHAR(64) | unique |
| user_id | BIGINT | FK app_user |
| collection_id | BIGINT | FK resource_collection |
| status | VARCHAR(32) | ACTIVE/REVOKED/EXPIRED |
| granted_by_user_id | BIGINT | FK app_user |
| granted_at_utc | DATETIME(3) | not null |
| expires_at_utc | DATETIME(3) | nullable |
| revoked_at_utc | DATETIME(3) | nullable |
| reason | VARCHAR(500) | nullable |
| version | BIGINT | optimistic |

Unique `(user_id, collection_id)`；indexes `(user_id,status,expires_at_utc)`、`(collection_id,status)`。

RBAC migration 同时 seed 新权限，并通过现有 ADMIN role 绑定。

## 17. V23 Prescription 表

优先扩展现有 `learning_plan`/`learning_task`，不创建语义重复主表。

### `learning_plan` 新增

- `prescription_version BIGINT NOT NULL DEFAULT 1`；
- `learner_timezone VARCHAR(64)`；
- `priority_goal VARCHAR(64)`；
- `policy_version VARCHAR(64)`；
- `input_snapshot_json JSON`；
- `reason_codes_json JSON`；
- `expires_at_utc DATETIME(3)`；
- `supersedes_plan_id BIGINT NULL FK learning_plan`。

新增 unique `(user_id, plan_date, prescription_version)`；保留旧 unique 直到数据迁移确认，再单独 migration 调整，不能在同一 migration 无保护删除兼容约束。

### `learning_task` 新增

- `block_type VARCHAR(32)`；
- `skill_unit_variant_id BIGINT FK`；
- `resource_version_id BIGINT FK`；
- `episode_mapping_id BIGINT FK`；
- `scaffolding_level VARCHAR(32)`；
- `training_type VARCHAR(64)`；
- `expected_evidence_json JSON`；
- `fallback_resource_version_id BIGINT NULL FK`；
- `recommendation_factors_json JSON`。

Index `(plan_id,status,sequence_no)`、`(resource_version_id,status)`。

## 18. V24 Session、Attempt、Evidence

### `training_session` 新增

- `learning_task_id BIGINT NULL FK learning_task`；
- `resource_version_id BIGINT NULL FK`；
- `skill_unit_variant_id BIGINT NULL FK`；
- `episode_mapping_id BIGINT NULL FK`；
- `current_step VARCHAR(64)`；
- `step_state_json JSON`。

已有 `plan_id` 保留。V2 Session type 使用 `SCENARIO_LESSON`。

### `task_attempt` 新增

- `idempotency_key VARCHAR(128)`；
- `attempt_status VARCHAR(48)`；
- `retry_of_attempt_id BIGINT NULL FK task_attempt`；
- `asr_transcript TEXT NULL`；
- `asr_confidence DECIMAL(6,5) NULL`；
- `transcript_confirmed BOOLEAN NOT NULL DEFAULT FALSE`；
- `evaluation_json JSON NULL`；
- `analysis_error_code VARCHAR(64) NULL`；
- `evaluator_prompt_version VARCHAR(64) NULL`；
- `provider_trace_json JSON NULL`（脱敏）。

Unique `(session_id,idempotency_key)`；通用 actor + operation 级 replay/conflict 由 V26 `api_idempotency_record` 负责。Index `(attempt_status,updated_at_utc)`、`(retry_of_attempt_id)`。

现有 `result` 在兼容期保留，由 adapter 映射新状态；后续单独 migration 清理。

### `learning_evidence` 新增

- `skill_id BIGINT NULL FK curriculum_skill`；
- `skill_unit_variant_id BIGINT NULL FK`；
- `resource_version_id BIGINT NULL FK`；
- `episode_id BIGINT NULL FK`；
- `task_type VARCHAR(64)`；
- `criteria_results_json JSON`；
- `retry_improves_evidence_id BIGINT NULL FK learning_evidence`；
- `policy_version VARCHAR(64)`。

Unique non-null `attempt_id` 保证每个 Attempt 最多一组主 Evidence；若一个 Attempt 影响多个 Skill，使用一条 evidence header + 新 `learning_evidence_skill` 关系表，避免破坏唯一性。

### `learning_evidence_skill`

`evidence_id`、`skill_id`、role、impact_score、previous_estimate、next_estimate；PK `(evidence_id,skill_id)`。

## 19. V25 Memory 与 Review

### `learner_error_memory`

`id`、error_key、user_id、error_tag、related_skill_id、frequency、severity、last_attempt_id、last_occurred_at_utc、status、metadata_json、version；unique `(user_id,error_tag,related_skill_id)`。

### `learner_expression_memory`

`id`、expression_key、user_id、normalized_expression、state (`UNDERSTOOD/PROMPTED/INDEPENDENT/TRANSFERRED`)、confidence、last_attempt_id、last_used_at_utc、metadata_json、version；unique `(user_id,normalized_expression)`。

### `learner_review_state`

`id`、review_key、user_id、target_type、skill_id nullable、expression_memory_id nullable、due_at_utc、forgetting_risk、last_recall_quality、review_count、policy_version、status、version。

CHECK：skill/expression target 恰好一个。Index `(user_id,status,due_at_utc)`。

## 20. V26/V27 Operations 表

V26 先提供运行时 AI 分析恢复和通用幂等；V27 再提供离线内容导入与校验，避免 Scenario Lesson 运行时依赖内容管理里程碑。

### V26 `analysis_retry_job`

job_key、attempt_id unique、job_type、status、attempt_count、next_run_at_utc、lease_owner、lease_until_utc、last_error_code、timestamps/version；index `(status,next_run_at_utc)`。

### V26 `api_idempotency_record`

actor_user_id、operation、idempotency_key、request_hash、status、response_reference、response_status、expires_at_utc、timestamps；unique `(actor_user_id,operation,idempotency_key)`，index `(expires_at_utc)`。

### V27 `content_import_batch`

import_key、actor_user_id、manifest_hash、status、resource_key/version、summary_json、created/updated/completed、version；unique `(resource_key,resource_version,manifest_hash)`。

### V27 `content_validation_issue`

batch_id、issue_code、severity、json_path、message、metadata_json；index `(batch_id,severity)`。

## 21. 删除、保留与审计

- Resource 不物理删除已被 session/evidence 引用的 version；使用 DISABLED；
- revoke 不删除 Private Progress；
- user deletion 按现有 privacy workflow 处理用户数据，不级联删除公共 Curriculum/Resource；
- 用户录音按 privacy retention 清理 object，Attempt 保留最小状态和不可逆 metadata；
- Admin audit 保存 grant/revoke/publish/provider changes，before/after JSON 脱敏；
- idempotency record 和 retry job 可按运维策略过期清理；
- content hash/object key 清理必须检查无任何 active reference。

## 22. 数据库验证

每个 migration 至少验证：

1. 空库 V1→最新完整迁移；
2. 含 V18 样例数据的前向迁移；
3. 外键和 CHECK 生效；
4. duplicate resource version/idempotency/entitlement 被拒绝；
5. optimistic update 检测冲突；
6. Access/Candidate/Review 查询使用目标索引；
7. rollback 后无部分 Evidence/Skill 更新；
8. Flyway checksum 稳定，历史 migration 未修改。

## 23. API 验收

- OpenAPI lint 和 example validation 通过；
- 每个写 API 覆盖正常、validation、ownership、permission、idempotency replay/conflict；
- Attempt 覆盖 AI timeout、invalid JSON、analysis pending/final failure；
- SSE 覆盖 delta、completed、error、disconnect/reconcile；
- private media 覆盖 no entitlement、expired、revoke 和 URL expiry；
- publish 覆盖 Schema/business/asset failure；
- V1 Web 在兼容端点上仍通过，V2 Web 只使用 canonical V2 endpoints。
