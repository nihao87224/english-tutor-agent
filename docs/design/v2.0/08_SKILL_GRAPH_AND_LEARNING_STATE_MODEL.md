# English Tutor Agent V2.0 Skill Graph 与 Learning State 模型设计

> 文档版本：`2.0.0`
> 分支：`develop/v2.0`
> 目标：定义用户能力如何表示、学习证据如何沉淀，以及 Planner 如何基于能力状态推荐下一步训练。

---

# 1. 设计目标

V2.0 的核心问题：

> AI 如何知道用户现在最应该练什么？

答案不能来自：

- 用户浏览记录；
- 简单课程完成数量；
- 单次测试结果。

应该来自：

```
User Goal
+
Skill Graph
+
Learning Evidence
+
Error Memory
+
Historical Performance
+
Content Metadata
```

最终形成动态学习状态。

---

# 2. 用户能力模型

## 2.1 不使用课程完成度作为能力指标

错误模型：

```
完成课程 20/100
=
英语水平提高
```

正确模型：

```
用户能力
 |
 +-- Communication Skills
 |
 +-- Language Accuracy
 |
 +-- Vocabulary Usage
 |
 +-- Fluency
 |
 +-- Listening Comprehension
```

课程只是提升能力的手段。

---

# 3. Skill Graph

## 3.1 定义

Skill Graph 是用户英语能力的结构化表示。

示例：

```
Workplace English
 |
 +-- Report Progress
 |      |
 |      +-- Describe completed work
 |      +-- Explain blocker
 |      +-- Explain next step
 |
 +-- Explain Problems
 |      |
 |      +-- Describe issue
 |      +-- Explain cause
 |      +-- Suggest solution
 |
 +-- Meeting Communication
        |
        +-- Ask clarification
        +-- Agree / disagree
        +-- Summarize discussion
```

---

# 4. Skill Node 模型

每个 Skill Node 包含：

```
SkillNode

id
name
category
parentSkill
cefrRange
importance
```

用户状态：

```
UserSkillState

userId
skillId
masteryScore
confidence
lastPracticedAt
lastEvidenceAt
trend
```

---

# 5. Mastery Score

每个 Skill 使用 0-100 分表示。

示例：

```
Explain Technical Problem

score: 42
```

含义：

```
0-20
完全不会

20-50
能够表达，但问题较多

50-70
基本满足沟通

70-90
自然表达

90+
熟练应用
```

---

# 6. Learning Evidence

能力提升必须来自 Evidence。

Evidence 来源：

```
Conversation
Scenario Lesson
Speaking Task
Role Play
Assessment
Review Task
```

示例：

用户完成：

```
Explain a Deployment Incident
```

AI 分析：

```
Grammar: 75
Fluency: 60
Naturalness: 55
Task Completion: 80
```

生成：

```
Evidence

skill:
Explain Problem

impact:
+3 mastery

weakness:
Cause Expression
```

---

# 7. 错误如何映射 Skill

错误不能孤立保存。

例如：

用户：

```
The service broken because database problem.
```

错误：

```
article
collocation
sentence structure
```

需要映射：

```
Technical Explanation
        |
        +-- Cause Explanation
        +-- Problem Description
```

这样 Planner 才知道应该推荐什么训练。

---

# 8. Learning State 更新规则

一次训练结束：

```
Training Result
       |
       v
Evaluation
       |
       v
Evidence
       |
       v
Skill State Update
       |
       v
Planner Adjustment
```

---

# 9. 遗忘与衰减模型

Skill 不应该永久增长。

长期未使用需要降低置信度。

示例：

```
masteryScore
+
confidenceScore
```

如果：

```
90天没有使用
```

则：

```
confidence下降
```

但历史能力不会完全删除。

---

# 10. Weak Point 模型

Weak Point 是 Planner 推荐的重要输入。

来源：

```
高频错误
+
低评分 Skill
+
用户主动反馈
+
失败任务
```

示例：

```
Weak Points

1. Explain Cause
priority: high

2. Meeting Response
priority: medium

3. Articles
priority: low
```

---

# 11. Planner 推荐决策

Planner 不直接选择课程。

流程：

```
User State
     |
     v
Find Weak Skill
     |
     v
Find Matching Skill Unit
     |
     v
Filter Permission
     |
     v
Rank Resources
     |
     v
Generate Today Plan
```

---

# 12. 推荐评分模型

资源推荐可以使用：

```
Score =
 GoalMatch
+
 WeakPointMatch
+
 SkillGap
+
 DifficultyMatch
+
 Freshness
+
 UserPreference
```

示例：

用户：

```
Goal:
Technical Meeting

Weak Skill:
Explain Cause
```

候选：

```
Coffee Ordering
score 20

Explain Root Cause
score 95
```

选择后者。

---

# 13. 今日计划生成示例

用户状态：

```
B1

Strong:
Daily Update

Weak:
Explain Problems

Goal:
Workplace English
```

生成：

```
Today

1.
Review:
Cause Expression
5 min

2.
Skill Unit:
Explain Technical Problem
15 min

3.
Role Play:
Production Incident
10 min
```

---

# 14. 与现有 Memory 系统关系

V1.x Memory 主要记录：

- 错误；
- 表达偏好；
- 学习历史。

V2.0 Skill State 增加：

- 能力结构；
- 掌握程度；
- 技能变化趋势。

关系：

```
Memory
  |
  +-- Error Memory
  +-- Expression Memory
  +-- Skill State
```

---

# 15. 产品价值

Skill Graph 是 English Tutor Agent 与普通 AI Chat 的核心区别。

普通 AI：

```
用户问
 ↓
回答
```

English Tutor Agent：

```
了解用户
 ↓
发现缺口
 ↓
安排训练
 ↓
验证掌握
 ↓
调整未来教学
```

这才形成长期私人英语教练能力。
