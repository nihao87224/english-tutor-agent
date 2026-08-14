# ADR-0010：M0/M1 优先使用 Fake Provider

状态：Superseded by ADR-0013

## Decision

在真实 LLM、ASR、TTS 接入前，先使用固定、可重复的 Fake Provider 跑通：

- 初始评估；
- 能力画像；
- 今日计划；
- 文字/语音状态流程；
- JSON Schema、重试和降级；
- Android 端到端路径。

真实 Provider 选择不会阻塞 M0，也不阻塞 M1 的确定性业务闭环。
