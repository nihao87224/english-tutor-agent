# English Tutor Agent V2.0 总体架构设计

## 1. 架构目标

V2.0 在 V1.x Agent 学习闭环之上增加 Resource Layer，使系统具备：

- 基于用户状态的个性化教学决策；
- 可解释的每日课程处方；
- 稳定教学资源管理；
- 个性化资源推荐；
- Lin Muen 沉浸式场景学习；
- 私有课程权限控制；
- 离线内容生产。

架构必须保证：AI 私教是决策核心，Lin Muen 是体验核心；故事层不能反向覆盖 Mastery、Review、Difficulty 或 Access 决策。

## 2. 总体架构

```text
Client
 |
API
 |
Learning Application
 |
+-- Personalized Tutor Core
|   +-- Learner Model
|   +-- Pedagogical Policy
|   +-- Daily Prescription
|   +-- Recommendation Explanation
|
+-- Content Capability Layer
|   +-- Skill Graph
|   +-- Skill Unit Catalog
|   +-- Resource Service
|   +-- Access Filter
|
+-- Lin Muen Experience Layer
|   +-- Season / Episode Service
|   +-- Episode Mapping
|   +-- Character Agent
|   +-- Scenario Lesson Player
|
+-- Training and Evidence Layer
|   +-- Speaking Training
|   +-- Correction / Retry
|   +-- Evaluation
|   +-- Evidence / Memory / Review
 |
+-- Private Collection Service
+-- Model / ASR / TTS Provider Ports
 |
Database + Object Storage
```

## 3. 核心模块

### Personalized Tutor Core

职责：

- 加载 Goal、Skill State、Error Memory、Evidence、Review State 和可用时间；
- 通过确定性教学策略识别训练优先级；
- 生成包含复习、新学、输出和迁移块的 Daily Learning Prescription；
- 输出可解释的推荐原因；
- 不直接依赖具体 LLM、ASR 或 TTS SDK。

### Pedagogical Policy

负责 prerequisite、mastery、spacing、difficulty、interleaving、transfer 和 retry。核心结果必须可重复、可测试；LLM 只能提供受约束分析，不能直接修改掌握度或复习到期时间。

### Skill Unit Catalog

保存可训练能力、前置技能、难度变体、Training Type、常见错误映射、Evidence Criteria 和 Episode Mapping。Skill Unit 是教学最小单位。

### Lin Muen Experience Layer

在 Skill Unit 已确定后选择合适的 Season / Episode / Scene，组合 Story、Mission、Dialogue、视觉与声音资源。它维护体验连续性，但不能改变教学处方。

### Resource Service

职责：

- 查询学习资源；
- 管理 metadata；
- 根据条件筛选候选资源；
- 返回媒体 asset。

### Access Filter

所有推荐和访问必须经过：

```text
Resource Candidate
 -> Publish Check
 -> Permission Check
 -> User Matching
 -> Ranking
```

### Scenario Lesson Service

负责：

- 加载课程包；
- 根据处方选择 Skill Unit Variant 与 Episode Mapping；
- 记录学习进度；
- 驱动 First Listen、Speaking Task、Role Play。

### Speaking Training Service

复用已有：

- Correction Agent；
- Expression Coach；
- Evaluation Agent；
- Memory Service。

## 4. 资源存储原则

Git 保存：

- lesson manifest；
- schema；
- prompts；
- generation metadata。

Object Storage 保存：

- 图片；
- 音频；
- 视频；
- 大型附件。

禁止将学习媒体放入 Spring Boot resources。

## 5. 推荐链路

```text
Learner Model
 -> Review Urgency / Temporary Goal
 -> Skill Gap Identification
 -> Pedagogical Policy
 -> Skill Unit Candidates
 -> Publish + Access Filter
 -> Difficulty / Time / Freshness Ranking
 -> Episode Mapping
 -> Daily Learning Prescription
 -> Lin Muen Experience
 -> Evidence
```

Episode Mapping 必须发生在 Skill Unit 和教学策略确定之后。剧情顺序只能影响体验连贯性评分，不能覆盖能力缺口和复习优先级。

## 6. EngFluent 集成原则

EngFluent 属于 Private Collection：

- 不污染公共资源；
- 不默认进入 Planner；
- 需要 entitlement；
- 支持 grant/revoke。

## 7. 演进原则

V2.0 不拆微服务。

优先保持模块化单体，通过领域边界演进。

领域依赖方向：

```text
Experience -> Application Ports <- Tutor Domain
Resource Infrastructure -> Application Ports
Provider SDK -> Provider Adapters -> Domain Ports
```

Tutor Domain 不依赖 Character Agent、Web、数据库实现或供应商 SDK。
