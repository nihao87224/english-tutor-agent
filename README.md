# English Tutor Agent — Vibe Coding Starter

> 版本：`1.0.1`  
> 日期：`2026-07-21`  
> 状态：`编码启动基线`

本仓库骨架用于把 English Tutor Agent 从已经确认的 PRD、概要设计和详细设计，稳定推进到工程初始化、纵向功能开发、测试、Review 和发布。

## 1. 资料优先级

发生冲突时，按以下顺序判断：

1. `docs/prd/ENGLISH_TUTOR_AGENT_PRD_v1.0.0.md`
2. `docs/design/` 中对应设计文档
3. `docs/decisions/` 中已接受 ADR
4. `contracts/` 中机器可读契约
5. `docs/plans/CURRENT_TASK.md`
6. 当前会话中的明确任务说明

不得由编码 Agent 擅自修改产品目标、核心业务规则或已接受的架构决策。

## 2. 推荐使用方式

第一次在 Cursor 中打开项目后，先运行：

```text
/plan-task
```

或者向 Agent 发送：

```text
请阅读 AGENTS.md、PRD、概要设计、详细设计、IMPLEMENTATION_PLAN.md 和 TASK_BACKLOG.md。
当前不要修改代码。
请检查文档冲突，输出 Milestone 0 的实施计划和预计修改文件。
```

确认计划后，再执行单个任务。不要一次要求 Agent 实现整个项目。

## 3. 项目目录

```text
english-tutor-agent/
├── AGENTS.md
├── README.md
├── .cursor/
│   ├── rules/
│   └── commands/
├── docs/
│   ├── prd/
│   ├── design/
│   ├── ui/
│   ├── plans/
│   ├── process/
│   ├── test/
│   ├── decisions/
│   └── release/
├── contracts/
│   ├── openapi/
│   ├── schemas/
│   └── examples/
├── evaluation/
├── infrastructure/
├── server/
├── android/
└── scripts/
```

## 4. 当前阶段

当前包不包含正式业务代码。第一项开发工作是：

```text
M0-T01 建立仓库与目录基线（不包含后端或 Android 工程初始化）
```

开始编码前，应先更新：

- `docs/plans/CURRENT_TASK.md`
- `docs/plans/TASK_BACKLOG.md`

任务完成后，应更新：

- 任务状态
- 测试结果
- 必要 ADR
- API/Schema 契约
- `CHANGELOG.md`

## 5. 质量底线

任何任务只有满足 `docs/process/DEFINITION_OF_DONE.md` 才能标记完成。  
任何提交在合并前都要按照 `docs/process/REVIEW_CHECKLIST.md` 复核。
