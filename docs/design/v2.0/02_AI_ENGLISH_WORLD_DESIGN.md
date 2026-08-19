# Lin Muen AI English World Design v2.0

> 文档版本：`2.0.1`
> 定位：个性化 AI 私教的沉浸体验层

## Vision

English Tutor Agent V2.0 是由专业 AI 私教驱动、由 Lin Muen 世界承载的沉浸式英语学习产品。

用户不是按固定目录消费故事。AI 私教先判断用户今天最需要训练的能力，再把训练安排进 Lin Muen 的生活、旅行、工作和社交任务中。

```text
Personalized Tutor Core
        ↓ chooses
Skill Unit + Teaching Strategy
        ↓ maps to
Lin Muen Episode / Scene
        ↓ produces
Immersive Mission + Evidence
```

## World Structure

```text
Experience Graph: Season -> Episode -> Scene -> Story State
Capability Graph: Goal -> Skill -> Skill Unit -> Difficulty Variant

Episode Mapping connects the two graphs.
```

Season 提供关系和故事连续性，Skill Unit 提供可评价的学习目标。二者必须分离建模。

## Season 1: Getting Closer to English

1. Meet Lin Muen
2. Coffee Shop Morning
3. Weekend Shopping
4. Fashion Blogger Day
5. Preparing Singapore Trip
6. Airport Adventure
7. Hotel Check-in
8. Meeting Foreign Friends
9. Work Meeting
10. English Challenge

这些 Episode 是可复用的体验空间，不是所有用户统一的十节固定课程。

允许：

- 根据 Skill Gap 跳转到最适合的 Episode；
- 在同一 Episode 中选择不同 CEFR、communication complexity 和 Training Type；
- 用一句自然过渡保持剧情连续；
- 已掌握用户进入迁移、澄清、问题解决或长段表达任务；
- 在不同 Episode 重练同一 Skill，以收集迁移 Evidence。

## Lin Muen's Role

Lin Muen 是故事主角、英语学习伙伴、情景任务参与者，以及温暖反馈与鼓励的表达载体。

Lin Muen 不是 Mastery、Review 或推荐顺序的决策者，不替代 Planner，不强迫用户跟随固定剧情顺序，也不充当官方考试评分者。

## Design Principles

每次体验必须同时包含：

- 与用户目标相关的 Skill Unit；
- 明确 communication goal；
- Lin Muen 人物连接；
- 真实生活情景；
- 主动输出与 Retry；
- 可验证的 Evidence Criteria；
- 对后续 Daily Learning Prescription 有效的学习结果。

体验质量服从教学适配，教学专业性通过体验增强。两者缺一不可。
