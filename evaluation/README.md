# AI Evaluation Datasets

这些 JSONL 是最初的回归种子，不是完整题库。

规则：

- 每行一个独立 JSON；
- caseId 永不复用；
- 修改预期需要 Review；
- 真实模型评测必须记录 Provider、model、Prompt version 和参数；
- 不得加入真实用户原文或录音；
- 失败样本修复后应保留为回归案例。
