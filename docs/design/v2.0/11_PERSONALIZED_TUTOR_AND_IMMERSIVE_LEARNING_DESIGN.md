# English Tutor Agent V2.0 个性化私教与沉浸式学习融合设计

> 文档版本：`2.0.1`
> 状态：`V2.0 双核心设计基线`
> 日期：`2026-08-19`
> 上游需求：`docs/prd/v2.0/ENGLISH_TUTOR_AGENT_PRD_v2.0.0.md`

---

## 1. 设计目标

V2.0 必须同时兑现两个不可互相替代的价值：

1. **AI 私教专业性**：系统依据用户目标、能力、短板、遗忘风险和近期证据，决定今天最值得训练什么、如何训练以及何时复习；
2. **Lin Muen 沉浸陪伴**：把训练目标放入连续、真实、有情感连接的故事和任务中，让用户愿意进入场景、持续开口并形成学习动力。

冻结关系：

```text
AI 私教决定教学
        ↓
Lin Muen 承载体验
        ↓
用户产生真实输出
        ↓
Evidence 更新私教判断
```

Lin Muen 不负责判断用户掌握程度，也不决定推荐顺序；她是学习伙伴、故事主角和反馈表达载体。

---

## 2. 双核心产品模型

### 2.1 Personalized Tutor Core

负责：

- 维护 Learner Model；
- 识别目标相关的 Skill Gap；
- 执行复习、难度、交错和迁移策略；
- 生成 Daily Learning Prescription；
- 解释为什么推荐；
- 根据 Evidence 更新后续计划。

### 2.2 Lin Muen Immersive Experience

负责：

- 提供 Season / Episode 连续世界；
- 将 Skill Unit 映射为真实情景任务；
- 保持角色身份、语气、关系和视觉一致性；
- 通过 Story、Mission、Dialogue、Role Play 和 Review 增强参与感；
- 让纠错与鼓励保持温暖、自然、非评判式表达。

### 2.3 不允许的替代关系

- 不允许用“按 Episode 顺序播放”替代个性化教学；
- 不允许用 LLM 主观判断替代确定性的掌握、复习和权限规则；
- 不允许为了个性化而实时生成全部图片、音频或视频；
- 不允许为了剧情连续性向用户推荐已经掌握或明显不匹配的训练；
- 不允许为了效率把 Lin Muen 降级为静态封面人物。

---

## 3. 核心领域结构

```text
Learner Model
├── Goal Profile
├── Skill State
├── Error Memory
├── Expression Memory
├── Recent Evidence
├── Review State
├── Preference
└── Available Time

Pedagogical Policy
├── Prerequisite Policy
├── Mastery Policy
├── Spacing Policy
├── Difficulty Policy
├── Interleaving Policy
├── Transfer Policy
└── Retry Policy

Content Capability Graph
├── Skill Unit
├── Difficulty Variant
├── Training Type
├── Evidence Criteria
└── Episode Mapping

Experience Graph
├── Season
├── Episode
├── Story State
├── Relationship State
└── Scene Assets
```

课程推荐连接的是 `Learner Model -> Skill Unit`；Episode Mapping 发生在教学目标确定之后。

---

## 4. Learner Model

每个用户至少维护：

| 维度 | 作用 |
|---|---|
| Goal Profile | 长期目标、临时诉求、目标场景和优先级 |
| Skill State | mastery、confidence、trend、lastPracticedAt |
| Error Memory | 错误类型、频率、严重度、最近出现和相关 Skill |
| Expression Memory | 已理解、能提示使用、能独立迁移的表达 |
| Evidence | 听力、Guided Speaking、Role Play、Retry、Assessment 证据 |
| Review State | dueAt、遗忘风险、上次回忆质量和复习次数 |
| Preference | 主题、交互方式、节奏和 Lin Muen 体验偏好 |
| Constraint | 当天可用时间、设备、音频条件和压力水平 |

`completion` 不能直接提升 `mastery`。只有满足质量门槛的 Evidence 才能改变掌握状态。

---

## 5. 教学策略基线

V2.0 P0 使用可测试的确定性规则，LLM 只提供受约束的分析与内容变化。

### 5.1 Prerequisite

未达到前置技能最低掌握度时，不推荐依赖该技能的高复杂度任务。

### 5.2 Retrieval Practice

复习优先要求用户回忆和输出，不以重新播放材料作为主要完成条件。

### 5.3 Spaced Practice

复习到期由表现、置信度和时间共同计算；不得只按固定天数或课程完成数安排。

### 5.4 Difficulty Fit

训练难度应位于用户当前稳定能力略上方。连续失败时降低任务复杂度或增加支架，连续轻松完成时提高复杂度或进入迁移任务。

### 5.5 Interleaving

每日计划避免单一题型堆叠，在主技能、相关技能和到期复习之间交错安排。

### 5.6 Transfer

用户在一个场景使用过的表达，需在后续不同场景中再次独立使用，才能形成更高置信度的掌握证据。

### 5.7 Feedback and Retry

反馈聚焦最影响沟通目标的 1–3 个问题。关键问题必须有 Retry；Retry 结果单独形成 Evidence。

---

## 6. Daily Learning Prescription

每日自动课程不是一条 Resource ID，而是一份有教学意图的处方：

```text
DailyLearningPrescription
├── priorityGoal
├── targetSkills
├── rationale
├── reviewBlock
├── acquisitionBlock
├── outputBlock
├── transferBlock (optional)
├── experienceContext
├── estimatedMinutes
└── completionAndEvidencePolicy
```

生成流程：

```text
加载用户目标与能力状态
        ↓
处理到期复习与紧急诉求
        ↓
识别高价值 Skill Gap
        ↓
应用前置、难度、间隔与去重规则
        ↓
选择 Skill Unit 与 Training Type
        ↓
匹配可用 Episode / Scene
        ↓
组合 Lin Muen Story + Mission
        ↓
保存推荐原因与预期 Evidence
```

P0 排序必须至少考虑：Goal Match、Skill Gap、Review Urgency、Error Match、Difficulty Fit、Transfer Value、Freshness、Time Fit 和 Access。

---

## 7. Skill Unit 与 Episode 的组合

### 7.1 Skill Unit 是教学最小单位

Skill Unit 至少定义：

- `skillId`；
- `communicationGoal`；
- `level` 与 communication complexity；
- prerequisites；
- target / supporting skills；
- common error mappings；
- training types；
- scaffolding levels；
- evidence criteria；
- mastery impact policy；
- review template；
- estimated minutes。

### 7.2 Episode 是体验容器

Episode 至少定义：

- 故事背景与 Lin Muen 的当前目标；
- 场景和角色关系；
- 可承载的 Skill Unit；
- 难度变体和可替换任务；
- 图片、音频、对话和任务资源；
- 每个可发布任务变体的 scene-specific `task_hero`，用于今日处方任务卡和 Scenario Lesson 场景引导；
- Story continuity state；
- 不依赖技能顺序的进入条件。

### 7.3 Season 不等于课程顺序

Season 1 的 `EP001`–`EP010` 是故事空间，不是所有用户必须按同一教学顺序完成的十节固定课。

允许：

- 首次体验通过短引导建立人物关系；
- 根据 Skill Gap 跳转至最适合的 Episode；
- 在同一 Episode 中选择 A2/B1/B2 或不同 Training Type；
- 为剧情连续性提供一句自然过渡，不改变教学目标；
- 已掌握用户跳过基础任务，进入迁移或更复杂任务。

---

## 8. 静态资源与运行时个性化

离线预生成：

- Episode story anchors；
- 场景图片和正式音频；
- 标准 Dialogue / Transcript；
- Skill Unit 与难度变体；
- Expressions、Questions、Scaffolds 和 Review Templates；
- Role Play 边界和 Evidence Criteria。

运行时确定性组合：

- 目标技能选择；
- 复习到期；
- 难度和支架级别；
- Training Type；
- Episode 匹配；
- 完成条件与 Evidence 写入。

运行时 LLM：

- 在边界内改变对话细节；
- 扮演角色并响应用户；
- 分析用户输出；
- 生成纠错、自然表达和鼓励；
- 对临时真实诉求生成短期训练变化。

---

## 9. Lin Muen 角色边界

Lin Muen 可以：

- 解释今天为什么一起完成某个任务；
- 作为故事主角、对话伙伴或需要用户帮助的人；
- 用温暖语气提供反馈；
- 回忆共同完成过的故事节点；
- 鼓励用户完成 Retry 和复习。

Lin Muen 不可以：

- 宣称基于直觉判断用户已掌握某项技能；
- 绕过 Planner 自行改变课程目标；
- 为了剧情推进忽略失败 Evidence；
- 伪装成官方 IELTS 考官或给出官方成绩；
- 让情感互动掩盖错误、权限或隐私边界。

---

## 10. Evidence 闭环

每个训练块必须声明预期 Evidence：

```text
Task Attempt
    ↓
Task Completion Analysis
    ↓
Accuracy / Fluency / Naturalness / Strategy
    ↓
Evidence Validation
    ↓
Skill State + Error Memory + Expression Memory
    ↓
Review Schedule + Next Prescription
```

Evidence 至少绑定：

- userId；
- skillId；
- resourceId / episodeId / skillUnitId；
- resourceVersion；
- taskType；
- attemptId；
- criteria results；
- confidence；
- occurredAt UTC；
- retry relation。

AI 输出必须先解析、Schema 校验、业务校验，再进入学习状态更新。

---

## 11. 推荐解释与用户控制

今日课程需提供简短、可验证的解释，例如：

> 今天和 Lin Muen 练习酒店入住，因为你的旅行目标优先级较高，最近两次表达中“确认信息”较弱，同时该技能已到复习时间。

用户可以：

- 表示太难、太简单、时间不足或不想练该主题；
- 提交临时目标；
- 选择减少情景叙事或增加 Lin Muen 陪伴强度；
- 跳过单个推荐，但系统需记录原因并重新组合计划。

用户偏好影响呈现和排序，但不能覆盖必要的权限、前置技能和安全规则。

---

## 12. 验收标准

### 12.1 私教专业性

- 两个不同 Skill State 的同等级用户可获得不同的今日训练；
- 推荐能够给出基于目标、Evidence、复习或短板的原因；
- 已掌握 Skill 不会因剧情顺序被重复作为基础课推荐；
- Retry 和迁移结果能够改变 Skill State 或 Review；
- 相同输入状态得到可重复、可测试的核心决策。

### 12.2 沉浸体验

- 每个推荐 Skill Unit 都能映射到至少一个 Lin Muen 场景；
- 故事、对话、图片、音频和任务保持角色一致；
- 真实任务页面必须展示与当前 Scene / Mission 匹配、清晰出现 Lin Muen 的 `task_hero`；Speaking / Role Play 训练主体区域继续展示该场景图或连续的 `scene_state`，不能降级成“纯对话界面 + 人物头像”；
- 情景图需交代 Lin Muen 的具体位置、动作和环境关系，例如登机口任务展示她位于候机或登机区域的全身、四分之三身或环境中景；
- 跳集或切换难度时仍有自然的体验过渡；
- 用户完成的是情景沟通任务，而不是孤立语法题；
- Lin Muen 的鼓励不替代准确反馈和 Retry。

### 12.3 成本与稳定性

- 首屏和正式媒体不依赖运行时生成；
- 运行时失败不丢失用户回答和学习进度；
- Planner、Evidence 与资源版本可追踪；
- 图片或音频失败时仍可降级完成有效训练。

---

## 13. 冻结决策

1. AI 私教是产品决策核心，Lin Muen 是沉浸体验核心；
2. Skill Graph 与 Experience Graph 分离，通过 Episode Mapping 连接；
3. Daily Learning Prescription 先确定教学目标，再选择故事情景；
4. Season 不是强制教学顺序；
5. Completion 与 Mastery 分离；
6. 确定性规则管理掌握、复习、难度、权限和证据；
7. LLM 管理受约束的语言互动、分析和反馈；
8. 预生成媒体保证质量与成本，运行时个性化集中在组合与交互；
9. 每个 Episode Resource 必须声明可训练 Skill、Evidence Criteria 和适配条件；
10. V2.0 成功以能力改善、计划适配度和持续学习为主，不以 Episode 播放量为主。
