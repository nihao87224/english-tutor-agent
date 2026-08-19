# Episode Content Design v2.0

> 文档版本：`2.0.1`
> 原则：Episode 是体验容器，Skill Unit 是教学最小单位。

## Episode Structure

Each episode contains:

1. Story
2. Visual Assets
3. Learning Objective
4. Skill Unit Mappings
5. Vocabulary and Expressions
6. Dialogue Variants
7. User Mission
8. Evidence Criteria
9. Review Tasks
10. Social Media Content

每个 Episode 必须支持一个或多个 `SkillUnitVariant`，并为不同能力状态提供可选择的难度、支架和 Training Type。不得只提供一条所有用户相同的固定 Dialogue 和 Exercise。

## Story Driven Learning

Do not create isolated grammar lessons.

Example:

Bad:

Learn airport English.

Good:

Help Lin Muen solve communication problems during her first overseas trip.

Better:

The tutor identifies that the learner needs to practice clarification. It selects the Airport Adventure episode and asks the learner to help Lin Muen confirm a changed gate, with criteria for asking, confirming and restating key information.

## User Experience

The user should feel they are participating in Lin Muen's daily life while practicing English.

体验连续性不能覆盖教学适配：

- 用户可以按能力缺口进入非顺序 Episode；
- 系统用简短过渡恢复 Story Context；
- 已掌握基础任务的用户进入更复杂任务或跨场景迁移；
- 连续失败的用户获得更多 scaffolding，而不是被剧情推进；
- 每次 Mission 必须产生可写入 Skill State 的 Evidence。

## Adaptive Episode Slots

Episode 至少提供以下可组合槽位：

```text
Story Anchor       固定故事背景和人物关系
Skill Slot         可映射的 Skill Unit Variant
Input Slot         图片、音频、Transcript
Practice Slot      Guided Speaking / Retrieval / Transfer
Role Play Slot     受约束的实时互动
Evidence Slot      成功标准、失败原因和 Retry
Review Slot        可调度的复习模板
```

## Selection Contract

Planner 先输出：

- target skill；
- communication goal；
- difficulty；
- scaffolding level；
- training type；
- expected evidence；
- time budget。

Episode Service 再选择匹配的 Story、Scene 和资源。若没有合适映射，应回退到其他 Episode 或通用 Scenario Lesson，不得为推进剧情而降低教学匹配。
