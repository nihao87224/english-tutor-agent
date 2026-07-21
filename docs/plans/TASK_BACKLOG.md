# Task Backlog

状态取值：`TODO`、`PLANNING`、`IN_PROGRESS`、`REVIEW`、`BLOCKED`、`DONE`。

## Milestone 0

| ID | 任务 | 状态 | 依赖 | 主要验收 |
|---|---|---:|---|---|
| M0-T01 | 建立仓库与目录结构 | DONE | - | 目录、规则、文档可用 |
| M0-T02 | 初始化后端工程 | TODO | M0-T01 | Java 21 构建和测试通过 |
| M0-T03 | 初始化 Android 工程 | TODO | M0-T01 | Debug 构建通过 |
| M0-T04 | 本地基础设施 | TODO | M0-T01 | MySQL/Redis/MinIO 健康 |
| M0-T05 | Flyway、Testcontainers、健康检查 | TODO | M0-T02,M0-T04 | clean DB 启动成功 |
| M0-T06 | CI 与契约校验 | TODO | M0-T02,M0-T03 | PR 质量门可运行 |
| M0-T07 | Fake AI Provider | TODO | M0-T02 | 无外部密钥可演示 |
| M0-T08 | 冷启动验证 | TODO | M0-T02..07 | 新环境按文档启动 |

## Milestone 1

| ID | 任务 | 状态 | 依赖 | 关联需求 |
|---|---|---:|---|---|
| M1-T01 | 主要学习目标 | TODO | M0 | FR-ONB |
| M1-T02 | 学习、纠错、提醒和隐私偏好 | TODO | M1-T01 | FR-ONB,FR-DAT |
| M1-T03 | Onboarding 进度恢复 | TODO | M1-T01 | FR-ONB |
| M1-T04 | 听说读写自评 | TODO | M1-T03 | FR-ASM |
| M1-T05 | 自适应初评会话 | TODO | M1-T04 | FR-ASM |
| M1-T06 | 客观题评分 | TODO | M1-T05 | FR-ASM |
| M1-T07 | 开放答案评估 | TODO | M1-T05 | FR-ASM |
| M1-T08 | 初始画像 | TODO | M1-T06,M1-T07 | FR-PRO |
| M1-T09 | 首个今日计划 | TODO | M1-T08 | FR-PLN |
| M1-T10 | Android 首次使用 E2E | TODO | M1-T01..09 | P0 |

## Milestone 2

| ID | 任务 | 状态 |
|---|---|---:|
| M2-T01 | Training Session 与任务状态机 | TODO |
| M2-T02 | 文字训练任务提交 | TODO |
| M2-T03 | SSE 流式会话 | TODO |
| M2-T04 | 分层纠错 | TODO |
| M2-T05 | Learning Evidence | TODO |
| M2-T06 | 每日总结 | TODO |
| M2-T07 | 下一计划自动变化 E2E | TODO |

## Milestone 3

| ID | 任务 | 状态 |
|---|---|---:|
| M3-T01 | Android 录音状态机 | TODO |
| M3-T02 | 音频上传 | TODO |
| M3-T03 | ASR Provider | TODO |
| M3-T04 | 听力播放器 | TODO |
| M3-T05 | TTS Provider | TODO |
| M3-T06 | ASR 低置信度确认 | TODO |
| M3-T07 | 弱网重试和恢复 E2E | TODO |

## Milestone 4–6

详细拆分在对应里程碑启动前完成。不得提前创建大量未使用代码。

## Task card template

```markdown
# <TASK-ID> <任务名称>

状态：TODO
优先级：P0
负责人：
关联需求：
关联设计：
依赖：

## 用户价值

## 范围

## 非范围

## 业务规则

## 接口与数据影响

## 验收标准

## 测试计划

## Review 重点

## 完成记录
```
