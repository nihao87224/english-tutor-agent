# Codex Resource Pre-generation Guide

## 1. Purpose

本文件用于指导 Codex 批量生成 V2.0 学习资源。

目标：生成可直接导入 Resource Catalog 的标准课程资产。

## 2. 输出目录

```text
resources/learning-content/v2.0/
└── lessons/
    └── {topic}/
        └── {scene}/
            └── {level}/
                ├── lesson.json
                ├── transcript.json
                ├── questions.json
                ├── expressions.json
                ├── image.webp
                └── audio.mp3
```

## 3. 首批生成范围

72 lessons:

- 8 topics
- 24 scenes
- A2/B1/B2

优先生成 B1 24 lessons 验证流程。

## 4. 单课生成 Prompt 约束

Codex/LLM 必须生成：

1. Communication Goal
2. Scene Context
3. Natural Dialogue
4. Transcript
5. Key Expressions
6. Vocabulary
7. Comprehension Questions
8. Speaking Tasks
9. Role Play Goal

禁止：

- 教科书式对话；
- 不自然高级词堆砌；
- 为了语法展示制造奇怪句子。

## 5. 图片生成规范

Prompt 要求：

- realistic lifestyle photo
- clear scene relationship
- no important text inside image
- suitable for English learning
- show who speaks with whom

示例：

"A realistic office meeting room, a software engineer explaining a deployment issue to two teammates, natural workplace atmosphere, educational scenario image, no text."

## 6. Audio 规范

每课：

- 1段主音频；
- 多角色区分；
- 保存 voice metadata；
- transcript 必须与音频一致。

## 7. lesson.json 示例结构

```json
{
  "id":"workplace.daily_update.b1",
  "level":"B1",
  "topic":"Workplace",
  "scene":"Daily Update",
  "communicationGoal":"Give a work progress update",
  "assets":{
    "image":"image.webp",
    "audio":"audio.mp3"
  }
}
```

## 8. 生成原则

教材一次生成，多次复用。

不要生成视频。

视频不是 V2.0 依赖。

重点投入：

- 场景真实性；
- 音频质量；
- 输出任务设计；
- AI Role Play 衔接。
