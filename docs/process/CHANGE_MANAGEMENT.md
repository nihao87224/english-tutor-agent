# Change Management

## 1. Change types

### Product change

改变用户目标、核心流程、范围、业务规则或验收口径。

要求：

- 更新 PRD；
- 说明用户影响；
- 重新检查设计与验收场景；
- 明确版本号。

### Architecture change

改变模块边界、存储、通信方式、关键技术基线。

要求：

- 新建 ADR；
- 分析迁移、风险、测试和回滚；
- ADR 接受后才能编码。

### Contract change

改变 API、Schema、数据库语义。

要求：

- 先更新机器契约；
- 评估向后兼容；
- Android/后端共同 Review。

### Implementation detail

不改变外部行为和架构约束的内部实现调整。

可在任务内处理，但仍需测试和 Review。

## 2. Decision states

- Proposed
- Accepted
- Rejected
- Superseded
- Deprecated

## 3. Versioning

- PRD：产品行为变化时升级；
- Design：架构/详细设计变化时升级；
- API：破坏兼容时升级主版本；
- Prompt/Schema：每次行为变化独立版本化；
- App：遵循发布版本和变更日志。
