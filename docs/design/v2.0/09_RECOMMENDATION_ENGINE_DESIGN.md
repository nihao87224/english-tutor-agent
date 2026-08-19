# English Tutor Agent V2.0 Recommendation Engine Design

> 文档版本：`2.0.1`
> 分支：`develop/v2.0`
> 目标：定义 AI 如何基于用户状态选择下一步最值得训练的内容。

---

# 1. 设计目标

推荐系统不是课程展示系统。

核心问题：

> 为什么今天给这个用户推荐这个训练？

答案必须来自：

```
User Goal
+
Skill State
+
Weak Points
+
Learning Evidence
+
Content Capability
+
Available Time
```

输出不是简单的课程列表，而是可解释的 `Daily Learning Prescription`。Lin Muen Episode 只在教学目标确定后参与体验匹配。

---

# 2. 推荐原则

## 2.1 能力缺口优先

错误：

```
用户喜欢旅游
↓
推荐旅游英语
```

正确：

```
用户目标 + 当前不足
↓
选择训练目标
```

---

## 2.2 不推荐用户已经掌握内容

如果：

```
Skill:
Daily Update

Mastery:
85
```

则减少重复推荐。

除非：

- 间隔复习；
- 更高难度升级；
- 新场景迁移。

---

# 3. 推荐输入模型

## 3.1 用户维度

```
UserProfile

Goal
CurrentLevel
PreferredTopics
DailyAvailableTime
LearningPreference
```

---

## 3.2 能力维度

```
SkillState

SkillId
MasteryScore
Trend
Confidence
LastPracticeTime
```

---

## 3.3 错误维度

```
ErrorMemory

ErrorType
Frequency
Severity
LastOccurrence
RelatedSkills
```

例如：

```
article error
+
technical explanation
```

映射：

```
Explain Technical Problem
```

---

# 4. 推荐流程

```
User State
    |
    v
Resolve Urgent Goal and Review Due
    |
    v
Identify High-value Skill Gaps
    |
    v
Apply Prerequisite and Mastery Policy
    |
    v
Find Matching Skill Unit Variants
    |
    v
Publish and Access Filter
    |
    v
Difficulty / Time / Freshness Ranking
    |
    v
Apply Interleaving and Transfer Policy
    |
    v
Map to Lin Muen Episode / Scene
    |
    v
Generate Daily Learning Prescription
```

---

# 5. Candidate Generation

候选来源：

```
PUBLIC Scenario Lesson

+

Private Collection
(仅有权限时)

+

Review Task

+

Conversation Practice
```

---

# 6. Resource Filtering

推荐前必须过滤：

## Permission

```
HasAccess(resource)
```

## Status

```
Published
AND
Available
```

## Duplicate

避免：

```
连续三天同一课程
```

---

# 7. Ranking Model

推荐分数：

```
FinalScore =

GoalMatch
+
SkillGap
+
ErrorMatch
+
ReviewUrgency
+
DifficultyFit
+
TransferValue
+
Freshness
+
TimeFit
+
UserPreference
```

P0 评分后仍需执行组合约束：

- 今日至少一个主动输出块；
- 到期高优先级复习不得被故事新鲜度长期挤出；
- 连续失败时增加支架或降低 communication complexity；
- 连续轻松完成时提高复杂度或安排跨场景迁移；
- Episode continuity 仅作为体验匹配因子，不得覆盖能力和复习因子。

---

# 8. 各因素说明

## GoalMatch

目标相关程度。

例如：

```
Goal:
Work Meeting

Resource:
Explain Technical Problem

High Score
```

---

## SkillGap

能力越弱，优先级越高。

示例：

```
Skill score 30
>
Skill score 80
```

---

## ErrorMatch

课程是否解决近期错误。

例如：

用户：

```
I very think...
```

推荐：

```
Natural Expression Training
```

---

## DifficultyFit

不能只推荐最弱项。

原则：

```
当前水平附近
+
略有挑战
```

例如：

B1 用户：

优先：B1/B1+

不直接：C1

---

# 9. 推荐解释

每次推荐最好生成原因。

示例：

```
今天推荐：
Explain a Technical Problem

原因：

1. 你的工作目标是技术沟通
2. 最近3次表达中原因解释较弱
3. 该技能当前评分42
4. 难度适合B1水平
```

这会增强用户信任。

---

# 10. Planner Agent 工作流

```
Daily Trigger
      |
      v
Load User State
      |
      v
Select Priority Skills
      |
      v
Select Skill Units and Teaching Strategies
      |
      v
Map Experience Context
      |
      v
Compose Daily Learning Prescription
      |
      v
Save Plan
```

---

# 11. 每日计划结构

示例：

```
Morning

1.
Review yesterday error
5 min

2.
Skill Training
Explain Root Cause
15 min

Evening

3.
Role Play
Production Incident
10 min
```

处方还必须保存：

- target skill 与选择原因；
- difficulty / scaffolding；
- selected episode mapping；
- expected evidence；
- completion policy；
- fallback resource；
- prescription version。

---

# 12. 推荐反馈闭环

推荐不是一次性的。

```
Recommend
   |
User Starts
   |
User Completes
   |
Evaluation
   |
Update Skill State
   |
Improve Next Recommendation
```

---

# 13. 特殊场景

## 临时需求

用户：

```
Tomorrow I have an English meeting.
```

覆盖长期计划：

```
Urgent Goal
+
Short-term Training
```

推荐：

```
Meeting Survival Practice
```

---

## 用户拒绝推荐

记录：

```
RecommendationFeedback
```

分析：

- 难度过高；
- 主题不感兴趣；
- 时间不足。

调整未来排序。

---

# 14. V2.0 初期实现建议

不要一开始使用复杂机器学习模型。

推荐阶段：

```
Rule Based Ranking
        |
        v
Evidence Accumulation
        |
        v
LLM Assisted Planning
        |
        v
Future ML Ranking
```

第一版重点是建立高质量学习数据。

---

# 15. 核心原则总结

English Tutor Agent 推荐系统：

不是：

```
猜你喜欢什么课程
```

而是：

```
你现在最需要补齐什么能力，为什么，以及下一步如何验证你已经掌握。
```

Lin Muen 回答的是“如何让这次训练真实、有关系、有动力”；Personalized Tutor Core 回答的是“为什么今天练这个、难度如何、怎样判断有效”。
