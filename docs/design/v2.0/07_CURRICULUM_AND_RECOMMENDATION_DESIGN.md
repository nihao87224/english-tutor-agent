# English Tutor Agent V2.0 课程体系与智能推荐设计

> 文档版本：`2.0.0`
> 状态：`V2.0 教学体系设计基线`
> 日期：`2026-08-19`
> 目标：定义 AI 如何判断用户应该学什么，以及课程如何围绕能力提升设计。

---

# 1. 核心设计理念

V2.0 最大的产品价值不是增加更多学习素材，而是：

> **让 AI 在正确的时间，为正确的用户，安排正确的训练内容。**

图片、音频、视频只是教学载体，不是核心价值。

核心闭环：

```
用户能力状态
    ↓
发现能力缺口
    ↓
选择训练目标
    ↓
匹配课程 Skill Unit
    ↓
完成输入与输出训练
    ↓
评估掌握程度
    ↓
更新能力模型
    ↓
下一次推荐调整
```

---

# 2. 课程设计原则

## 2.1 不按照知识点组织课程

传统：

```
Grammar
 ├── Present tense
 ├── Past tense
 └── Passive voice
```

English Tutor Agent：

```
Communication Goal
 ├── 描述经历
 ├── 解释问题
 ├── 表达观点
 └── 参与会议
```

用户不是为了学习过去式而学习，而是为了能够描述昨天发生的问题。

---

# 3. Skill Graph 能力模型

用户能力不使用简单课程完成数表示，而使用技能图谱。

示例：

```
Workplace English
        |
        +-- Report Progress
        |       |
        |       +-- Describe completed work
        |       +-- Explain blocker
        |       +-- Describe next step
        |
        +-- Explain Problems
        |       |
        |       +-- Describe issue
        |       +-- Explain cause
        |       +-- Suggest solution
        |
        +-- Meeting Communication
                |
                +-- Ask clarification
                +-- Agree / disagree
                +-- Summarize discussion
```

每个 Skill 记录：

- 当前等级；
- 最近表现；
- 常见错误；
- 已掌握表达；
- 待加强能力。

---

# 4. Skill Unit 课程单元

V2.0 最小教学单位不是 Video Lesson，而是 Skill Unit。

一个 Skill Unit：

```
Skill Unit
 |
 +-- Context
 +-- Input
 +-- Understanding
 +-- Guided Practice
 +-- Speaking Task
 +-- Role Play
 +-- Evaluation
 +-- Review
```

例如：

## Explain a Technical Problem

目标：

用户可以向同事解释一个技术问题。

训练：

A2:

```
The database is not working.
Users cannot login.
```

B1:

```
The issue happened after deployment.
The service failed because of a database connection problem.
```

B2:

```
The root cause appears to be related to connection pool configuration.
We need to evaluate whether increasing the limit is the right solution.
```

---

# 5. 难度体系

CEFR 作为外部标准，但内部增加 Communication Complexity。

## Level 1

回答事实：

```
What do you do?
```

## Level 2

描述经历：

```
What happened yesterday?
```

## Level 3

解释原因：

```
Why did it happen?
```

## Level 4

表达观点：

```
What do you think about this solution?
```

## Level 5

讨论权衡：

```
Compare these two approaches.
```

---

# 6. 首期课程地图

一级主题按照真实沟通任务组织。

## Daily Life

- Introduce Yourself
- Talk About Routine
- Make Plans

## Food & Shopping

- Order Food
- Ask About Products
- Solve a Service Problem

## Travel

- Airport
- Hotel
- Unexpected Problems

## Workplace

- Daily Update
- Ask for Help
- Explain Problems

## Tech & Engineering

- Explain Bug
- Report Incident
- Discuss Technical Trade-off

## Social

- Small Talk
- Invitation
- Apology and Appreciation

## Ideas & Stories

- Retell Experience
- Explain Opinion
- Explain Process

## IELTS

- Part 1 Answer
- Part 2 Story
- Part 3 Discussion

---

# 7. AI 推荐系统设计

推荐不是根据兴趣，而是根据能力缺口。

输入：

```
User Profile
+
Learning Goal
+
Skill State
+
Error Memory
+
Recent Evidence
+
Completion History
+
Available Time
+
Resource Entitlement
```

输出：

```
Today's Training Plan
```

---

# 8. 推荐优先级

默认排序：

1. 当前目标相关性；
2. 最近薄弱技能；
3. 高频错误关联；
4. 难度匹配；
5. 最近未训练；
6. 可完成时间匹配；
7. 用户偏好。

---

# 9. 推荐示例

用户：

```
目标：工作英语
等级：B1

最近问题：
- 经常使用 because 解释原因
- 无法描述 bug 原因
- 会议回应困难
```

系统不要推荐：

```
General Vocabulary
```

应该推荐：

```
Explain a Technical Problem

原因：
1. 与工作目标高度相关
2. 命中最近弱点
3. 当前等级适合
```

---

# 10. 今日学习生成

每日计划由 Planner Agent 生成：

```
Morning

1. Review
5 min

2. Skill Unit
10 min

3. Speaking Practice
10 min

Evening

1. Role Play
15 min

2. Reflection
5 min
```

---

# 11. 课程完成判断

完成不是播放完成。

必须包含：

- 理解输入；
- 完成至少一个输出任务；
- 获得 AI Feedback；
- 必要时 Retry。

完成状态与掌握状态分离。

```
Completion
 !=
Mastery
```

---

# 12. 内容生产原则

课程生产优先生成：

1. Communication Goal；
2. Skill Objective；
3. Scenario；
4. Dialogue；
5. Speaking Task；
6. Evaluation Criteria。

图片和音频只是实现这些目标的资源。

---

# 13. 与参考产品的融合

## ESL Podcast

吸收：

- 高质量输入；
- Transcript；
- Vocabulary；
- Explanation。

## Hibay

吸收：

- 真实场景；
- 分级训练；
- Role Play；
- Feedback。

## ELSA / Loora

吸收：

- 输出驱动；
- 即时反馈；
- 持续训练。

最终形成：

```
Input
 ↓
Understand
 ↓
Practice
 ↓
Speak
 ↓
Feedback
 ↓
Retry
 ↓
Memory
```

---

# 14. Codex 内容生成要求

Codex 生成课程资源时，不允许从“生成一段英语文章”开始。

必须从：

```
Skill Unit Definition
 ↓
Learning Objective
 ↓
Scenario Design
 ↓
Content Generation
 ↓
Asset Generation
```

开始。

评价课程质量的第一标准：

> 用户完成后，是否比开始前更能完成一个真实英语沟通任务。
