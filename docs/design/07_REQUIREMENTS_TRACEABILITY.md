# English Tutor Agent 需求追踪矩阵

> 目的：确保 PRD P0 需求在设计、数据、接口和测试中都有落点。

---

## 1. 模块追踪

| PRD 需求 | 主要实现模块 | 核心数据 | 主要 API/流程 | 核心测试 |
|---|---|---|---|---|
| FR-ONB-001 | Identity/Onboarding | user_learning_profile | PUT primary-goal | 目标唯一、目标修改校准 |
| FR-ONB-002 | Identity/Preference | user_learning_profile | PUT preferences | 默认 20 分钟、偏好保存 |
| FR-ONB-003 | Android Onboarding | onboarding progress | GET onboarding/progress | 首次流程 E2E |
| FR-ASM-001 | Assessment | self_assessment | POST assessments/self | 行为描述、自评起点 |
| FR-ASM-002 | Assessment | assessment_session/attempt | assessment APIs | 8–10 分钟、暂停恢复 |
| FR-ASM-003 | Blueprint Engine | blueprint version | next question | 高低自评不同题型 |
| FR-ASM-004 | Learner/Reporting | skill state/result | GET assessment result | 优势、短板、置信说明 |
| FR-PRO-001 | Learner Model | learner_skill_state | GET profile/skills | 多维画像更新 |
| FR-PRO-002 | Mastery | knowledge_state | GET knowledge-states | 掌握状态机 |
| FR-PRO-003 | Learner Updater | learning_evidence | evidence pipeline | 单次变化上限、可追溯 |
| FR-PRO-004 | Android Progress | profile summary | GET learner-summary | 简化/展开显示 |
| FR-PLN-001 | Planning | learning_plan | GET plans/today | 自动计划、无需选课 |
| FR-PLN-002 | Plan Composer | learning_task | plan generation | 不同用户计划差异 |
| FR-PLN-003 | Planning | rationale | plan response | 解释有真实证据 |
| FR-PLN-004 | Planning/Android | duration | plan adjustment | 5 分钟保留高价值任务 |
| FR-PLN-005 | Planning | adjustment version | POST adjustments | 临时需求不覆盖画像 |
| FR-TRN-001 | Training/Audio/Android | session/audio asset | training/audio APIs | 文字语音听力 E2E |
| FR-TRN-002 | Task Registry | learning_task | current-task | 所有 P0 任务类型可渲染 |
| FR-TRN-003 | Content/Training | task payload/evidence | listening chain | 输入到输出链路 |
| FR-TRN-004 | Training | hint level/evidence | submit attempt | 辅助依赖入证据 |
| FR-TRN-005 | Planning/Content | scenario | plan/task | 场景与目标能力相关 |
| FR-TRN-006 | Content | content source/version | content pipeline | 版权和内容校验 |
| FR-CON-001 | Conversation Coach | message/session | SSE message | 交流优先、不逐句打断 |
| FR-CON-002 | Android/Training | mode | session mode | 文字语音切换不丢进度 |
| FR-CON-003 | Planner/Task | active output flag | task blueprint | 每计划主动输出 |
| FR-CON-004 | Scaffolding | hint level | task feedback | 提示从轻到重 |
| FR-COR-001 | Correction | correction_record | SSE correction | 分层纠错时机 |
| FR-COR-002 | Correction policy | correction_record | feedback | 1–3 个重点 |
| FR-COR-003 | Expression Coach | correction_record | feedback/retry | 自然表达与重答 |
| FR-COR-004 | Learner/Review | knowledge_state | evidence/review | 历史问题关联 |
| FR-REV-001 | Review Scheduler | review_schedule | today plan | 自动安排复习 |
| FR-REV-002 | Review/Content | task type | review tasks | 多形式迁移 |
| FR-REV-003 | Mastery | knowledge_state/evidence | evidence pipeline | 非一次正确掌握 |
| FR-REV-004 | Review | failure history | plan generation | 失败后换形式 |
| FR-IEL-001 | IELTS | simulation/task | IELTS APIs | Part 1 流程 |
| FR-IEL-002 | IELTS/Audio | simulation/audio | IELTS APIs | Part 2 计时录音 |
| FR-IEL-003 | IELTS | simulation/task | IELTS APIs | Part 3 难度与反馈 |
| FR-IEL-004 | IELTS | ielts_simulation | complete/result | 完整模拟不打断 |
| FR-IEL-005 | IELTS Evaluator | result/rubric | GET result | AI 参考标签和免责声明 |
| FR-RPT-001 | Reporting | session summary | complete session | 每日总结 |
| FR-RPT-002 | Reporting Job | weekly_report | GET weekly | 周报内容 |
| FR-RPT-003 | Assessment/Job | stage assessment | stage APIs | 四周动态测验 |
| FR-RPT-004 | Progress | evidence/skill state | profile APIs | 具体进步证据 |
| FR-REM-001 | Android Settings | DataStore/profile | PUT preference | 提醒开关 |
| FR-REM-002 | Android Sync | reminder config | local schedule | 建议时间、可忽略 |
| FR-DAT-001 | Privacy | retention settings | privacy APIs | 用途说明 |
| FR-DAT-002 | Privacy/Audio | retention mode | PUT privacy | 原始保存开关 |
| FR-DAT-003 | Privacy Deletion | deletion request | deletion APIs | 关系库/对象/缓存删除 |

---

## 2. 里程碑追踪

### M1：首次使用闭环

覆盖：FR-ONB、FR-ASM、FR-PRO 初始画像、FR-PLN 首个计划。

完成证据：

- Web 可完成最短目标、偏好和初评入口；Android 真机验收后移；
- 数据库形成初始画像；
- 初评后自动产生计划；
- 高自评与普通自评流程不同。

### M2：每日文字闭环

覆盖：FR-PLN、FR-TRN 文字任务、FR-CON、FR-COR、FR-RPT-001。

完成证据：

- 文字任务可流式回复；
- 纠错按层级出现；
- Attempt 生成 Evidence；
- 第二次计划发生合理变化。

### M3：Web 表达教练闭环

覆盖：FR-TRN 文字训练、FR-CON、FR-COR、FR-RPT-001、FR-DAT 原文保存控制。

完成证据：

- Web 可输入表达并接收 SSE 回复；
- `correction_ready` 展示 grammar feedback、natural expression 和 Try Again；
- Try Again 形成二次表达和 Evidence；
- 完成训练后展示总结和下一计划变化。

### M4：语音与听力闭环

覆盖：FR-TRN-001/003/004、FR-CON-002、音频 NFR。

完成证据：

- 真机录音、上传、ASR、回复和 TTS；
- 弱网重试不重复提交；
- ASR 低置信度不误判；
- 不方便说话可切文字。

### M5：长期学习闭环

覆盖：FR-REV、FR-RPT-002/003/004、掌握状态。

完成证据：

- 历史错误按时复习；
- 独立、迁移和延迟证据可改变状态；
- 周报和阶段测验改变计划。

### M6：IELTS 闭环

覆盖：FR-IEL。

完成证据：

- Part 1/2/3 和完整模拟；
- 分数标识 AI 参考；
- 评测稳定性回归；
- 结果生成针对性计划。

### M7：发布候选

覆盖：FR-REM、FR-DAT、全部 NFR 与质量护栏。

---

## 3. 变更规则

后续若设计无法实现某个 P0：

```text
创建 Design Issue
→ 引用 FR 编号
→ 说明技术原因和用户影响
→ 提供至少两个备选方案
→ 更新 PRD 或设计决策
→ 用户确认后才能改变实现范围
```
