# V2.0 Content Pipeline

## 1. 目标

建立可批量生产 Scenario Lesson 的离线流程。

## 2. Pipeline

```text
Lesson Blueprint
 ↓
Skill Unit / Learner Fit / Prerequisite
 ↓
Pedagogical Variants / Evidence Criteria
 ↓
Lin Muen Episode Mapping
 ↓
LLM Generate Script
 ↓
Generate Questions
 ↓
Generate Expressions
 ↓
Quality Check
 ↓
TTS
 ↓
Image Generation
 ↓
Asset Validation
 ↓
Lesson Package
 ↓
Publish
```

## 3. 生成顺序

不要先生成媒体。

正确顺序：

1. 确定 Skill Unit、教学目标和 learner fit；
2. 确定 prerequisites、难度、支架和 Training Type；
3. 定义 common error mappings、Evidence Criteria、Retry 和 Review；
4. 选择可承载该训练的 Lin Muen Episode Mapping；
5. 生成 Story、Mission、Script 和任务；
6. 校验 CEFR、communication complexity 和教学价值；
7. 生成音频与图片；
8. 校验 Schema、业务规则和人物一致性；
9. 打包 manifest。

## 4. Quality Check

检查：

- transcript 与 audio一致；
- 表达自然；
- 难度符合 CEFR；
- 问题可回答；
- 图片符合场景；
- 音频清晰。
- 推荐引擎可以依据 learner fit 和 Skill Unit 检索；
- Evidence Criteria 可观察、可评价；
- Story 顺序未覆盖教学适配；
- Lin Muen 身份、语气和视觉一致。

## 5. Codex 生产要求

Codex 不负责运行时生成课程。

Codex 负责：

- 批量生成文件；
- 校验目录；
- 生成 manifest；
- 执行质量检查脚本。

## 6. 发布

生成结果：

```text
lesson.json
scene.webp
main.mp3
transcript.json
```

导入 Resource Catalog 后发布。
