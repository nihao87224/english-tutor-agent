# Course Resource Schema v2.0

> 文档版本：`2.0.1`
> 状态：`Skill Unit + Episode 双图谱概念契约`
> 后续机器契约必须据此生成 JSON Schema，并遵循 `additionalProperties: false`。

## Episode Resource

```json
{
  "schemaVersion": "2.0.1",
  "resourceId": "season1.ep001.introduction.b1",
  "resourceVersion": "1.0.0",
  "seasonId": "S01",
  "episodeId": "EP001",
  "character": "Lin Muen",
  "story": {
    "title": "Meet Lin Muen",
    "context": "The learner meets Lin Muen and helps begin a friendly conversation.",
    "continuityRequired": false
  },
  "learnerFit": {
    "goalTags": ["daily_communication"],
    "cefrRange": ["B1"],
    "estimatedMinutes": 12,
    "prerequisites": [],
    "contraindications": []
  },
  "skillUnits": [
    {
      "skillUnitId": "social.self_introduction.b1",
      "communicationGoal": "Introduce yourself and ask a follow-up question",
      "targetSkills": ["self_introduction", "follow_up_question"],
      "supportingSkills": ["active_listening"],
      "level": "B1",
      "communicationComplexity": 2,
      "trainingTypes": ["guided_speaking", "role_play"],
      "scaffoldingLevels": ["high", "medium", "none"],
      "commonErrorTags": ["incomplete_answer", "missing_follow_up"],
      "evidenceCriteria": [
        "introduces self clearly",
        "states one relevant detail",
        "asks one natural follow-up question"
      ]
    }
  ],
  "images": [],
  "audio": [],
  "dialogue": [],
  "practice": [],
  "reviewTasks": [],
  "recommendation": {
    "eligibleReasons": ["goal_match", "skill_gap", "review_due", "transfer"],
    "storyOrderRequired": false
  },
  "evidencePolicy": {
    "minimumOutputTasks": 1,
    "retryOnCriticalError": true,
    "completionDoesNotImplyMastery": true
  }
}
```

## Required Content

Each episode must provide:

- Story
- Visual assets
- Learner fit and prerequisites
- Skill Unit mappings
- Learning objectives and communication goals
- Dialogue
- Speaking practice
- Evidence criteria
- Retry and completion policy
- Review tasks

## Asset Metadata

Images and audio should contain:

- id
- version
- purpose
- generationPrompt
- assetKey
- contentHash
- generation model/version metadata
- character reference ids for Lin Muen images
- speakerRole, voiceId, accent and speechRate for audio

## Selection Rules

推荐系统以 `skillUnits` 和 `learnerFit` 生成候选，不以 `episodeId` 顺序生成候选。

`storyOrderRequired` 在 P0 必须为 `false`。Story continuity 可以影响体验匹配分，但不得覆盖 Goal Match、Skill Gap、Review Urgency、Difficulty Fit 或 Access Filter。

## Validation Rules

- `character` 必须为 `Lin Muen`；
- 至少一个 Skill Unit；
- 每个 Skill Unit 至少一个可观察的 Evidence Criterion；
- Practice 必须覆盖主动输出；
- Role Play 必须声明 user role、AI role、goal 和 success criteria；
- Audio Script 与 Transcript 必须逐句一致；
- 图片不得依赖生成文字传递关键信息；
- Completion 与 Mastery 必须分离；
- resourceVersion 必须被 Evidence 引用；
- AI 生成结果须依次通过解析、JSON Schema 和业务校验。
