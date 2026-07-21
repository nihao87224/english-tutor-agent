# ADR-0009：AI Provider 抽象

状态：Accepted

## Decision

LLM、ASR、TTS 通过项目自有接口接入。业务层不得依赖供应商专用请求/响应类。

统一接口至少表达：

- Provider 与模型标识；
- 能力；
- 超时和错误分类；
- 使用量与成本；
- ASR/评估置信度；
- trace 信息；
- 结构化输出结果。
