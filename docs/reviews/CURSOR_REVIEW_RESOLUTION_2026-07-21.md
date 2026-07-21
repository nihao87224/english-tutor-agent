# Cursor Review Resolution — 2026-07-21

## Conclusion

审阅有效。启动包升级为 `v1.0.1`，M0-T01 可以继续；M0-T02 必须等待
实际仓库完成 Git 基线提交后开始。

## Resolution table

| ID | 结论 | 处理 |
|---|---|---|
| C1 | 接受 | README 明确 M0-T01 不包含后端/Android 初始化 |
| C2 | 接受 | 任务编号以 TASK_BACKLOG/CURRENT_TASK 为准；执行指南已对齐 |
| C3 | 接受 | ADR-0001～0011 已补齐并与设计语义映射 |
| C4 | 接受 | 门禁拆为 M0、真实 Provider、发布三个阶段 |
| C5 | 已决策 | Java 根包名固定为 `cn.forever24.tutor` |
| M1 | 接受 | PRD 仅更新阶段元数据，不改变需求基线 |
| M2 | 接受 | 设计门禁状态已按阶段更新 |
| M3 | 接受 | 实际目录固定为 `server/` 和 `android/` |

## Technology verification

锁定：

- Java 21；
- Spring Boot 4.1.0；
- Spring AI 2.0.0。

官方兼容关系已确认。M0-T02 仍需在实际 Maven/CI 环境执行依赖解析与构建验证。

## M0-T01 remaining local actions

1. 解压 v1.0.1 到正式项目目录；
2. `cp .env.example .env`；
3. `python scripts/validate_project.py`；
4. `git init`；
5. 检查 `git status`；
6. 创建基线提交；
7. 将 M0-T01 状态更新为 DONE；
8. 把 CURRENT_TASK 切换为 M0-T02。
