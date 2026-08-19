# English Tutor Agent V2.0 总体架构设计

## 1. 架构目标

V2.0 在 V1.x Agent 学习闭环之上增加 Resource Layer，使系统具备：

- 稳定教学资源管理；
- 个性化资源推荐；
- 场景课程学习；
- 私有课程权限控制；
- 离线内容生产。

## 2. 总体架构

```text
Client
 |
API
 |
Learning Application
 |
+-- Learning Planner
+-- Resource Service
+-- Scenario Lesson Service
+-- Private Collection Service
+-- Speaking Training Service
 |
+-- Memory
+-- Review
+-- Evaluation
+-- Model Provider
 |
Database + Object Storage
```

## 3. 核心模块

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
Planner
 ↓
Resource Candidate
 ↓
Access Filter
 ↓
Skill Match
 ↓
Recommendation
 ↓
Today Plan
```

## 6. EngFluent 集成原则

EngFluent 属于 Private Collection：

- 不污染公共资源；
- 不默认进入 Planner；
- 需要 entitlement；
- 支持 grant/revoke。

## 7. 演进原则

V2.0 不拆微服务。

优先保持模块化单体，通过领域边界演进。
