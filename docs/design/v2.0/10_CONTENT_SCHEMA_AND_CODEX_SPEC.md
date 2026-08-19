# English Tutor Agent V2.0 Content Schema 与 Codex 资源生成规范

> 文档版本：`2.0.1`
> 分支：`develop/v2.0`
> 用途：指导 Codex 批量生成 V2.0 学习资源。

---

# 1. 文档目标

本文档定义：

- 学习资源目录结构；
- Lesson JSON Schema；
- 图片生成规范；
- Audio/TTS 生成规范；
- 内容质量检查规则；
- Codex 批量生成流程。

核心原则：

> 不生成素材，而是生成可以驱动 AI 教学闭环的课程资源。

Season / Episode 资源还必须遵循：

- `07_COURSE_RESOURCE_SCHEMA.md`：Skill Unit + Episode 概念契约；
- `11_PERSONALIZED_TUTOR_AND_IMMERSIVE_LEARNING_DESIGN.md`：个性化教学与沉浸体验边界。

本文中的旧式单课示例仅用于文件拆分说明，不得被解释为“所有用户按固定课程顺序学习”。

---

# 2. 资源目录规范

推荐：

```
resources/
└── learning-content/
    └── v2.0/
        ├── catalog/
        ├── lessons/
        ├── assets/
        │   ├── images/
        │   └── audio/
        ├── schemas/
        └── reports/
```

---

# 3. Lesson 目录规范

示例：

```
lessons/
└── workplace/
    └── explain_problem/
        └── b1/
            ├── lesson.json
            ├── transcript.json
            ├── questions.json
            ├── expressions.json
            ├── speaking_tasks.json
            ├── image.webp
            └── audio.mp3
```

---

# 4. lesson.json

示例：

```json
{
  "id": "workplace.explain_problem.b1",
  "title": "Explain a Technical Problem",
  "level": "B1",
  "topic": "Workplace",
  "skill": "Explain Problem",
  "communicationGoal": "Explain why a technical issue happened",
  "durationMinutes": 12,
  "trainingTypes": [
    "guided_speaking",
    "role_play"
  ]
}
```

---

# 5. Lesson 必备内容

每个 Skill Unit 必须包含：

- learner fit；
- prerequisites；
- difficulty / scaffolding variants；
- common error mappings；
- Evidence Criteria；
- Episode Mappings；
- completion / retry / review policy。

## Context

说明：

- 场景在哪里；
- 用户是谁；
- 用户需要完成什么任务。

---

## Dialogue

要求：

- 接近真实交流；
- 避免教材化语言；
- 包含目标表达。

---

## Key Expressions

数量：

- A2: 3-4
- B1: 4-6
- B2: 5-8

格式：

```json
{
 "expression": "The issue seems to be caused by...",
 "meaning": "说明问题原因",
 "usage": "workplace"
}
```

---

# 6. Speaking Task Schema

示例：

```json
{
 "type":"role_play",
 "goal":"Explain a production issue",
 "userRole":"Engineer",
 "aiRole":"Manager",
 "successCriteria":[
   "Describe the issue",
   "Explain the cause",
   "Suggest next step"
 ]
}
```

---

# 7. 图片生成规范

## 目标

图片不是展示，而是帮助用户理解场景。

## 风格

推荐：

- realistic photo style;
- clean environment;
- natural lighting;
- clear character relationship;
- no text inside image.

禁止：

- 图片中生成菜单文字；
- 图片中生成 Jira 内容；
- 图片中生成大量英文。

---

# 8. 图片 Prompt 模板

```
Create a realistic English learning scene image.

Scene:
{scene}

Characters:
{characters}

Learning purpose:
{communication_goal}

Style:
realistic photography,
clean composition,
professional educational material,
natural lighting.

Do not include readable text.
```

---

# 9. TTS 音频规范

## 每课至少：

- 一个完整场景音频；
- 多角色区分；
- 自然停顿。

保存 metadata：

```
voiceId
accent
speaker
speed
version
```

---

# 10. 音频 Prompt

```
Generate a natural English conversation.

Level:
{CEFR}

Accent:
American neutral

Style:
real workplace conversation.

Avoid:
textbook speaking style.
```

---

# 11. Codex 生成流程

禁止：

```
Generate 100 English lessons.
```

正确：

```
1. Read Skill Unit definition

2. Define learner fit, prerequisites and target skills

3. Define difficulty, scaffolding and Training Type variants

4. Define Evidence Criteria, Retry and Review policy

5. Select an eligible Lin Muen Episode Mapping

6. Generate scenario and story context

7. Generate dialogue and expressions

8. Generate speaking and transfer tasks

9. Generate image prompt and audio script

10. Validate JSON Schema and business rules
```

---

# 12. 自动质量检查

每个 Lesson 必须检查：

## 教学目标

- 是否明确；
- 是否对应 Skill。

## 难度

- CEFR 是否匹配；
- 词汇是否过难。

## 语言

- 是否自然；
- 是否真实场景可用。

## 训练

- 是否要求用户输出；
- 是否可以 Role Play。

## 资源

- 图片是否符合场景；
- Audio 与 Transcript 是否一致。

---

# 13. 首批生成顺序

不要一次生成 72 个 Skill Unit Variant。

推荐：

## Batch 1

```
Workplace
+
Tech Engineering
+
B1
```

约：

10-15 Lessons

用于验证。

---

## Batch 2

扩展：

- Daily Life
- Travel
- Food

---

## Batch 3

补齐：

- A2
- B2
- IELTS

---

# 14. Codex Master Instruction

```
You are generating learning resources for English Tutor Agent V2.0.

Do not create simple English lessons.

Create Skill Units designed to improve real communication ability.

Every resource must include:
- communication goal
- scenario
- input
- speaking task
- evaluation criteria

The final goal is not that users understand English.
The final goal is that users can use English in real situations.
```

---

# 15. 验收标准

一个资源通过标准：

用户完成后：

1. 知道一个真实沟通目标；
2. 学会几个可迁移表达；
3. 完成一次主动输出；
4. 获得 AI 反馈；
5. 能进入下一次复习。

否则不认为是有效学习资源。
