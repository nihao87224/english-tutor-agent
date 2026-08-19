# Codex Generation Guide v2.0

> 文档版本：`2.0.1`
> 核心顺序：先定义可训练能力与 Evidence，再生成 Lin Muen 故事资源。

## Goal

Use Codex to generate English Tutor Agent v2.0 learning resources.

## Required Inputs

Read:

- docs/design/v2.0/
- assets/lin-muen/reference/

重点读取：

- `07_COURSE_RESOURCE_SCHEMA.md`
- `07_CURRICULUM_AND_RECOMMENDATION_DESIGN.md`
- `09_RECOMMENDATION_ENGINE_DESIGN.md`
- `11_PERSONALIZED_TUTOR_AND_IMMERSIVE_LEARNING_DESIGN.md`

## Generation Rules

1. Keep Lin Muen character consistent.
2. Define Skill Unit, learner fit and prerequisites before story content.
3. Define difficulty/scaffolding variants and common error mappings.
4. Define observable Evidence Criteria and Retry policy.
5. Map the Skill Unit to an Episode; do not use Episode order as curriculum order.
6. Generate story-driven lessons.
7. Generate image prompts and audio scripts with versioned metadata.
8. Generate dialogue, guided practice, role play and transfer tasks.
9. Generate retrieval-based review tasks.
10. Validate JSON Schema and business rules.

## Output

Each episode should contain:

- story
- learner fit
- skill unit mappings
- prerequisites
- difficulty and scaffolding variants
- images
- vocabulary
- dialogue
- exercises
- evidence criteria
- retry policy
- review

## Do Not

- Create random characters.
- Generate ordinary textbook lessons.
- Ignore Lin Muen visual identity.
- Generate ten identical fixed lessons for every learner.
- Let story order override learner goals, weak skills or review due state.
- Treat completion as mastery.
- Generate content without observable success criteria.

## Generation Order

```text
Skill Unit Definition
 -> Learner Fit and Prerequisites
 -> Pedagogical Variants
 -> Evidence and Review Policy
 -> Episode Mapping
 -> Story / Dialogue / Mission
 -> Image / Audio Metadata
 -> Schema and Business Validation
```
