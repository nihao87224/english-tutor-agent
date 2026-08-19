# English Tutor Agent V2.0 Resource Model

## 1. 核心对象

## LearningResource

统一学习资源。

字段：

- id
- provider
- collectionId
- type
- title
- level
- topic
- scene
- communicationGoal
- skillFocus
- accessScope
- assetRefs
- version

## Collection

资源集合。

示例：

- INTERNAL_SCENARIO_LIBRARY
- ENGLISH_FLUENCY_PRIVATE

## Asset

媒体资源。

类型：

- IMAGE
- AUDIO
- VIDEO
- JSON
- TRANSCRIPT

## Entitlement

用户权限。

字段：

- userId
- collectionId
- status
- grantedBy
- expiresAt

## LessonProgress

记录学习状态：

- NOT_STARTED
- IN_PROGRESS
- COMPLETED
- NEEDS_REVIEW

## 2. ScenarioLesson

```text
ScenarioLesson
 |
 +-- learnerFit
 +-- skillUnitVariants
 +-- episodeMappings
 +-- image
 +-- audio
 +-- transcript
 +-- expressions
 +-- questions
 +-- speakingTasks
 +-- rolePlay
```

## SkillUnitVariant

教学最小单位，至少包含：

- skillUnitId；
- communicationGoal；
- target / supporting skills；
- prerequisites；
- CEFR 与 communication complexity；
- training types；
- scaffolding levels；
- common error mappings；
- evidence criteria；
- completion / retry / review policy。

## EpisodeMapping

连接 Capability Graph 与 Lin Muen Experience Graph：

- skillUnitId；
- seasonId / episodeId / sceneId；
- eligible levels；
- story transition；
- experience fit score inputs；
- asset variant references。

Planner 先选择 Skill Unit Variant，再解析 Episode Mapping。Episode 顺序不得作为 mastery 或 review 的替代条件。

## 3. Resource Version

资源必须版本化。

示例：

```
workplace.daily_update.b1@1.0.0
```

历史学习记录绑定 resourceVersion。

Learning Evidence 还必须绑定当时的 `skillUnitId`、`episodeId`、`taskType` 和 `attemptId`，确保内容更新后仍可解释掌握状态变化。

## 4. 权限模型

AccessScope:

- PUBLIC
- ADMIN_GRANTED
- ADMIN_ONLY
- DISABLED

推荐系统只能使用通过权限过滤的资源。

## 5. 资源目录规范

```text
resources/learning-content/v2.0/
├── catalog
├── lessons
│   └── workplace
│       └── daily_update
│           └── b1
├── private-collections
├── schemas
└── reports
```

媒体不进入代码 classpath。
