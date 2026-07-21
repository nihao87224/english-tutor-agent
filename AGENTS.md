# Agent Development Instructions

本文件适用于 Cursor Agent、Codex、Claude Code 及其他参与本仓库开发的 AI 编码工具。

## 1. Source of truth

资料优先级：

1. PRD
2. 已接受的设计文档
3. ADR
4. OpenAPI、JSON Schema 等机器契约
5. 当前任务卡
6. 当前用户明确指令

发现冲突时：

- 不得自行选择“看起来更合理”的方案；
- 记录冲突位置与影响；
- 停止扩大改动范围；
- 在任务结果中明确标记为 `BLOCKED_BY_DECISION`。

## 2. Required workflow

每个任务必须按以下步骤执行：

1. 阅读当前任务及关联文档；
2. 检查现有代码和测试；
3. 在修改前输出实施计划；
4. 列出预计新增、修改和删除的文件；
5. 仅实现当前任务范围；
6. 编写或更新自动化测试；
7. 执行构建、测试和静态检查；
8. 人工可读地总结变更、测试结果、风险与遗留项；
9. 更新任务卡和必要文档。

## 3. Scope rules

- 一次只处理一个任务卡。
- 不得在未要求时重构无关代码。
- 不得一次创建未来多个里程碑的空壳实现。
- 不得为了“架构完整”引入当前任务不需要的抽象。
- 必须保持模块依赖方向。
- 需要扩大范围时，先说明原因和最小扩大范围。
- 不得删除文件、接口或数据库字段，除非任务明确允许。

## 4. Architecture invariants

- 第一版后端采用模块化单体。
- 业务领域不得直接依赖具体 LLM、ASR、TTS SDK。
- 学习优先级、复习到期和掌握状态由确定性规则负责。
- LLM 负责受约束的内容生成、分析和语言反馈。
- AI 输出必须通过 JSON Schema 和业务校验。
- 核心业务数据存入 MySQL；Redis 只保存短期状态、缓存、幂等和锁。
- 音频文件存对象存储，不存数据库 BLOB。
- 数据库结构只能通过 Flyway 前向迁移。
- Android 使用 Compose、ViewModel、StateFlow 和单向数据流。
- 首版语音交互采用可控的半双工 Push-to-Talk。

## 5. Code rules

- 使用清晰的业务命名，不使用无意义缩写。
- Controller/Route 不包含业务规则。
- Application Service 负责用例编排。
- Domain 层不依赖 Web、数据库实现和供应商 SDK。
- 外部调用必须配置超时、重试边界和可观测信息。
- 所有写接口必须考虑幂等、授权和重复提交。
- 时间统一使用 UTC 存储，对用户展示时转换时区。
- 不提交密钥、Token、真实用户录音或隐私数据。
- 日志不得输出完整授权头、密钥和不必要的用户原文。

## 6. Testing rules

新增功能至少覆盖：

- 正常路径；
- 输入边界；
- 权限或归属校验；
- 外部服务失败；
- 重复请求或幂等；
- AI 输出非法 JSON；
- 数据库异常或事务回滚；
- Android loading / content / empty / error 状态。

除非任务明确为纯文档，不得以“未运行测试”标记完成。

## 7. AI feature rules

- Prompt 必须版本化。
- 测试默认使用 Fake Provider 或固定响应。
- 不在单元测试中依赖真实付费模型。
- Provider 输出先解析，再 Schema 校验，再业务校验。
- AI 失败不得导致已完成的用户学习进度丢失。
- 低置信度 ASR 必须允许用户确认或重录。
- IELTS 评分必须明确为练习参考，不得声称为官方成绩。

## 8. Completion report

完成任务时必须输出：

```text
Task:
Status:

Changed files:
- ...

Implemented:
- ...

Tests executed:
- command: result

Contract/document updates:
- ...

Known risks:
- ...

Follow-up tasks:
- ...
```

## 9. Definition of Done

最终以 `docs/process/DEFINITION_OF_DONE.md` 为准。
