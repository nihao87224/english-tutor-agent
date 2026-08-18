# Architecture Decision Records

> 编号规则：本目录中的 `ADR-000x` 与 `docs/design/08_ARCHITECTURE_DECISIONS_AND_RISKS.md`
> 的 `ADR-00x` 语义一一对应。新增实施决策从 ADR-0012 起编号。

| ADR | 决策 | 状态 |
|---|---|---|
| ADR-0001 | 第一版采用模块化单体 | Accepted |
| ADR-0002 | 首版语音采用半双工 Push-to-Talk | Accepted |
| ADR-0003 | 规则决定学习优先级，LLM 生成受约束内容 | Accepted |
| ADR-0004 | 服务端维护长期事实，Android 使用离线优先缓存 | Accepted |
| ADR-0005 | 结构化 AI 输出必须强校验 | Accepted |
| ADR-0006 | MySQL + Redis + S3 兼容对象存储 | Accepted |
| ADR-0007 | Java 21 + Spring Boot 4.1.0 + Spring AI 2.0.0 | Accepted |
| ADR-0008 | Android Jetpack Compose + UDF | Accepted |
| ADR-0009 | LLM / ASR / TTS 使用项目自有 Provider 抽象 | Accepted |
| ADR-0010 | M0/M1 优先使用 Fake Provider 跑通确定性闭环 | Superseded by ADR-0013 |
| ADR-0011 | Java 根包名使用 `cn.forever24.tutor` | Accepted |
| ADR-0012 | Android 使用 Hilt 依赖注入 | Accepted |
| ADR-0013 | Production AI uses OpenAI-backed real providers | Superseded for default LLM by ADR-0015 |
| ADR-0014 | SaaS Foundation is the next product platform increment | Accepted for implementation planning |
| ADR-0015 | Multi-provider LLM protocols with DeepSeek as the default | Accepted |
