# Scenario Lesson Flow

## 1. 学习目标

Scenario Lesson 不是播放课程，而是完成一次输入到输出闭环。

## 2. 标准流程

```text
进入课程
 ↓
任务主图 + 场景引导
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

## 3. 真实实现页面视觉契约

`task_hero` 是 P0 Scenario Lesson 的必备视觉资源，不是原型专用装饰。

- 今日处方中的任务卡展示 `task_hero` 的响应式裁切缩略图；
- 进入课程后，在“理解场景”区域展示完整任务主图、Episode / Scene、任务目标和 Lin Muen 当前诉求；
- 任务主图必须清晰出现 Lin Muen，并体现当前地点、动作或沟通问题，不能只使用无人物环境图；
- Guided Speaking 和 Role Play 的训练主体区域必须继续展示当前 `task_hero`，或展示与当前子步骤匹配的 `scene_state` 场景图；画面需同时看见 Lin Muen 与可识别的环境关系，头像只能作为对话身份补充，不能满足情景视觉要求；
- 场景图应优先采用全身、四分之三身或环境中景。例如登机口确认任务应展示 Lin Muen 位于登机口候机区域，而不是只在对话框旁挂一枚头像；
- 页面文字、按钮、航班、菜单或工作信息使用结构化 UI overlay，不依赖图片内生成文字；
- 使用 `focalPoint` 或预生成裁切变体适配桌面、移动端和任务卡，不得在窄屏裁掉 Lin Muen；
- 首屏使用离线预生成并已发布的图片资源，不在用户进入课程时等待实时生成。

主图或 `scene_state` 加载失败时才显示 Lin Muen 的已发布人物 fallback、场景文字和 Audio / Transcript；头像属于降级方案，不是正常训练页面的主视觉实现。

## 4. 完成条件

不能只根据播放完成判断。

至少需要：

- 完成输入；
- 完成理解检查；
- 完成一次 Speaking Task。

## 5. AI 节点

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

## 6. 降级

图片失败：显示人物 fallback 并继续 Audio + Text。

Audio失败：继续 Transcript + Speaking。

AI失败：保存回答，稍后重新分析。

## 7. 与 Planner 集成

完成后输出：

- skill evidence；
- weak points；
- review task；
- next plan signal。
