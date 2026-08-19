# Image Generation Specification v2.0

## Purpose

Define image generation rules for Lin Muen English learning resources.

## Reference Priority

1. Lin Muen main reference image
2. Supplementary character references
3. Outfit references
4. Scene references

## Image Requirements

Default style:

- realistic smartphone photography
- natural lighting
- lifestyle blogger style
- authentic daily life feeling

## Task Hero Image（P0）

每个可发布的 Scenario Lesson / Episode Mapping 变体必须提供一张 `purpose = task_hero` 的任务主图。它会真实显示在今日处方任务卡和 Scenario Lesson 的场景引导区域，不是只用于资源目录或生成测试。

任务主图要求：

- Lin Muen 必须清晰可辨，是画面主角或明确的任务关系中心；
- 画面体现当前 Scene、Lin Muen 正在做的事以及沟通问题，避免仅生成通用头像或无人物环境图；
- 默认采用能同时交代人物和环境的全身、四分之三身或环境中景；除非教学线索依赖面部表情，否则近距离头像不能作为 `task_hero`；
- 场景中的位置与动作必须具体。例如机场确认任务应展示 Lin Muen 站在登机口或候机区域并准备确认信息，不能只生成机场背景加人物头像；
- 使用 Lin Muen main reference 和必要的 portrait / outfit reference 保持人物一致性；
- 生成一个横向 master，并提供 `focalPoint`，支持 16:9 场景区、4:3 移动端和 1:1 任务卡裁切；
- 构图为标题、任务目标和状态 overlay 保留安全区域，但图片本身不得包含可读文字；
- 同一任务的裁切变体必须来自同一 master asset，不得因断点重新生成不同面孔；
- 输出必须包含可访问的 `altText`，描述 Lin Muen、场景和动作，不重复页面标题。

Canonical reference image 只作为生成与一致性校验输入。正式页面优先展示与当前任务匹配的 scene-specific `task_hero`；仅在主图加载失败时使用已发布的 Lin Muen 人物 fallback。

当一个任务包含明显变化的子步骤时，可额外提供 `purpose = scene_state` 的图片。`scene_state` 必须与 `task_hero` 保持同一人物身份、服装和场景连续性；它不是必需的动画帧，也不得由运行时临时生成。

## Scene Categories

### Daily Life

Examples:

- coffee shop
- shopping
- weekend activities

### Travel

Examples:

- airport
- hotel
- restaurant
- sightseeing

### Work

Examples:

- meeting
- presentation
- interview

### Social

Examples:

- making friends
- conversations

## Prompt Template

Character:
Lin Muen

Scene:
location and environment

Action:
what she is doing

Style:
realistic lifestyle photography

Learning Goal:
English skill practiced in this scene
