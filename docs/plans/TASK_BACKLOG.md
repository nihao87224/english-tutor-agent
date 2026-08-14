# Task Backlog

状态取值：`TODO`、`PLANNING`、`IN_PROGRESS`、`REVIEW`、`BLOCKED`、`DONE`。

## Milestone 0

| ID | 任务 | 状态 | 依赖 | 主要验收 |
|---|---|---:|---|---|
| M0-T01 | 建立仓库与目录结构 | DONE | - | 目录、规则、文档可用 |
| M0-T02 | 初始化后端工程 | DONE | M0-T01 | Java 21 构建和测试通过 |
| M0-T03 | 初始化 Android 工程 | DONE | M0-T01 | Debug 构建通过 |
| M0-T04 | 本地基础设施 | DONE | M0-T01 | MySQL/Redis/S3 兼容对象存储健康 |
| M0-T05 | Flyway、Testcontainers、健康检查 | DONE | M0-T02,M0-T04 | 测试环境 Flyway/health 成功 |
| M0-T06 | CI 与契约校验 | DONE | M0-T02,M0-T03 | PR 质量门可运行 |
| M0-T07 | Fake AI Provider | DONE | M0-T02 | 无外部密钥可演示 |
| M0-T08 | 冷启动验证 | DONE | M0-T02..07 | 新环境按文档启动 |

## Milestone 1

| ID | 任务 | 状态 | 依赖 | 关联需求 |
|---|---|---:|---|---|
| M1-T01 | 主要学习目标 | DONE | M0 | FR-ONB |
| M1-T02 | 学习、纠错、提醒和隐私偏好 | DONE | M1-T01 | FR-ONB,FR-DAT |
| M1-T03 | Onboarding 进度恢复 | DONE | M1-T01 | FR-ONB |
| M1-T04 | 听说读写自评 | DONE | M1-T03 | FR-ASM |
| M1-T05 | 自适应初评会话 | DONE | M1-T04 | FR-ASM |
| M1-T06 | 客观题评分 | DONE | M1-T05 | FR-ASM |
| M1-T07 | 开放答案评估 | DONE | M1-T05 | FR-ASM |
| M1-T08 | 初始画像 | DONE | M1-T06,M1-T07 | FR-PRO |
| M1-T09 | 首个今日计划 | DONE | M1-T08 | FR-PLN |
| M1-T10 | Android 首次使用 E2E | DONE | M1-T01..09 | P0 |

## Milestone 2

| ID | 任务 | 状态 |
|---|---|---:|
| M2-T01 | Training Session 与任务状态机 | DONE |
| M2-T02 | 文字训练任务提交 | DONE |
| M2-T03 | SSE 流式会话 | DONE |
| M2-T04 | 分层纠错 | DONE |
| M2-T05 | Learning Evidence | DONE |
| M2-T06 | 每日总结 | DONE |
| M2-T07 | 下一计划自动变化 E2E | DONE |

## Milestone 3

| ID | 任务 | 状态 |
|---|---|---:|
| M3-R00 | V1.0 Web-first 路线重基线 | DONE |
| M3-T01 | Web 工程初始化与 API Client | DONE |
| M3-T02 | Web 最短 Onboarding 与本地用户键 | DONE |
| M3-T03 | Web 今日表达教练首页 | DONE |
| M3-T04 | Web SSE 对话与流式回复 | DONE |
| M3-T05 | Web 分层纠错与自然表达面板 | DONE |
| M3-T06 | Try Again 改写/复述练习闭环 | DONE |
| M3-T07 | Web 每日总结与下一计划变化展示 | DONE |
| M3-T08 | Web Expression Coach E2E | DONE |

## Milestone 4

| ID | 任务 | 状态 |
|---|---|---:|
| M4-T01 | Android 录音状态机 | TODO |
| M4-T02 | 音频上传 | TODO |
| M4-T03 | ASR Provider | TODO |
| M4-T04 | 听力播放器 | TODO |
| M4-T05 | TTS Provider | TODO |
| M4-T06 | ASR 低置信度确认 | TODO |
| M4-T07 | 弱网重试和恢复 E2E | TODO |

## Milestone 5–7

M5 长期复习与动态掌握、M6 IELTS Speaking、M7 发布准备在对应里程碑启动前详细拆分。
不得提前创建大量未使用代码。

## SaaS Foundation v1.1.0

> 权威设计：`docs/design/ENGLISH_TUTOR_AGENT_SAAS_FOUNDATION_DESIGN_v1.1.0.md`
> 实施计划：`docs/plans/SAAS_FOUNDATION_IMPLEMENTATION_PLAN_v1.1.0.md`
> 规则：一次只实现一个 SaaS milestone，不得提前写后续 milestone 的空壳代码。

| ID | 任务 | 状态 | 依赖 | 主要验收 |
|---|---|---:|---|---|
| SaaS-M1 | Identity Schema + Auth Backend | DONE | v1.0 baseline | Email 唯一、USER/ADMIN 登录、refresh rotation、bootstrap admin |
| SaaS-M2 | CurrentActor + Multi-user Isolation | DONE | SaaS-M1 | User A 无法访问 User B 学习资源，admin 不走 legacy identity |
| SaaS-M3 | Daily Quota Engine | DONE | SaaS-M2 | remaining=1 并发下仅 1 次成功，幂等不重复扣减 |
| SaaS-M4 | Runtime AI Provider + Secret | DONE | SaaS-M3, ADR-0013 | 默认 provider 可运行时切换，secret 加密且不回显 |
| SaaS-M5 | Admin Backend | TODO | SaaS-M4 | USER 访问 admin API = 403，敏感操作写 audit |
| SaaS-M6 | Web Learner SaaS UX + i18n | TODO | SaaS-M2, SaaS-M3 | register/login/practice/quota/history/logout 闭环 |
| SaaS-M7 | Web Admin Console | TODO | SaaS-M5 | Admin UI 与真实 API 联通，不使用生产 mock 数据 |
| SaaS-M8 | Android Learner Auth + Quota + i18n | TODO | SaaS-M6 | Web/Android 同邮箱看到同一用户、quota 和学习数据 |
| SaaS-M9 | Hardening + Legacy Cleanup | TODO | SaaS-M1..M8 | 无 legacy identity、无明文 secret、无跨用户访问 |

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
