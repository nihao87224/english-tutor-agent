# V2.0 Content Pipeline

## 1. 目标

建立可批量生产 Scenario Lesson 的离线流程。

## 2. Pipeline

```text
Lesson Blueprint
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

1. 确定教学目标；
2. 生成 Script；
3. 校验 CEFR 难度；
4. 生成音频；
5. 生成图片；
6. 打包 manifest。

## 4. Quality Check

检查：

- transcript 与 audio一致；
- 表达自然；
- 难度符合 CEFR；
- 问题可回答；
- 图片符合场景；
- 音频清晰。

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
