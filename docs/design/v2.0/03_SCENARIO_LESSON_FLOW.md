# Scenario Lesson Flow

## 1. 学习目标

Scenario Lesson 不是播放课程，而是完成一次输入到输出闭环。

## 2. 标准流程

```text
进入课程
 ↓
理解场景
 ↓
First Listen
 ↓
Comprehension Check
 ↓
Transcript
 ↓
Key Expressions
 ↓
Guided Speaking
 ↓
Role Play
 ↓
Correction
 ↓
Try Again
 ↓
Evidence
 ↓
Memory / Review
```

## 3. 完成条件

不能只根据播放完成判断。

至少需要：

- 完成输入；
- 完成理解检查；
- 完成一次 Speaking Task。

## 4. AI 节点

实时调用模型：

- 用户回答分析；
- Role Play；
- Correction；
- Natural Expression；
- Evaluation。

不调用模型：

- 图片加载；
- 音频播放；
- Transcript展示；
- 基础题目展示。

## 5. 降级

图片失败：继续 Audio + Text。

Audio失败：继续 Transcript + Speaking。

AI失败：保存回答，稍后重新分析。

## 6. 与 Planner 集成

完成后输出：

- skill evidence；
- weak points；
- review task；
- next plan signal。
