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
 +-- image
 +-- audio
 +-- transcript
 +-- expressions
 +-- questions
 +-- speakingTasks
 +-- rolePlay
```

## 3. Resource Version

资源必须版本化。

示例：

```
workplace.daily_update.b1@1.0.0
```

历史学习记录绑定 resourceVersion。

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
