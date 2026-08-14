# Test Strategy

## 1. Test pyramid

### Unit

- 领域规则；
- 计划优先级；
- 能力和掌握状态转换；
- 复习到期；
- 错误严重度与反馈时机；
- Android ViewModel 状态机。

### Integration

- MySQL Repository；
- Flyway clean database；
- Redis 幂等和锁；
- S3 上传元数据；
- API 鉴权、校验和事务；
- JSON Schema 验证。

### Contract

- OpenAPI 文件可解析；
- 请求/响应示例符合 Schema；
- Android Client 与服务端契约兼容；
- AI 示例符合 JSON Schema。

### End-to-end

每个里程碑至少一个真实纵向路径：

- M1：目标→初评→画像→今日计划；
- M2：文字训练→纠错→证据→下次计划；
- M3：录音→ASR→回复→TTS；
- M4：高频错误→复习→掌握更新；
- M5：完整 IELTS 模拟。

## 2. AI testing

### Deterministic tests

使用 local stub 或 mocked transport 返回固定结果，验证：

- 解析；
- Schema；
- 业务校验；
- 重试；
- 降级；
- 证据写入。

### Regression evaluation

真实模型变更前运行 `evaluation/*.jsonl`，检查：

- 纠错准确性；
- 不应打断率；
- 计划个性化；
- 不同能力用户区分度；
- IELTS 评分稳定性；
- 成本和延迟。

## 3. Release gates

- 单元与集成测试通过；
- OpenAPI/Schema 校验通过；
- clean DB 迁移通过；
- P0 E2E 通过；
- 无 BLOCKER/HIGH Review 问题；
- 隐私删除路径通过；
- Android 核心流程真机验证。
