# Cursor Review Resolution — 2026-07-21 (follow-up)

## Conclusion

用户已确认 R5（禁止 Java/Spring 技术栈回退）与 R6（Android 使用 Hilt）。文档已按确认项同步，M0-T02 可继续推进。

## Item status

| ID | 状态 | 处理 |
|---|---|---|
| R1 | **已关闭** | 孤儿 `ADR-0004-provider-abstraction.md` 已删除；仅保留 `ADR-0004-server-source-of-truth-offline-cache.md` |
| R2 | **已关闭** | Git 已初始化并推送到 `git@gitee.com:flyPanda/english-tutor-agent.git` |
| R3 | **已修复** | `ENGLISH_TUTOR_AGENT_DESIGN_v1.0.0.md` 里程碑章节已与 `TASK_BACKLOG` / `09_VIBE_CODING_EXECUTION_GUIDE.md` 对齐 |
| R4 | **已同步** | `08_ARCHITECTURE_DECISIONS_AND_RISKS.md` 与合并设计文档已补充 ADR-009～ADR-012 |
| R5 | **已确认并固化** | 移除 Java 17 回退表述；`ADR-0007` 与设计 ADR-007 明确禁止擅自降级 |
| R6 | **已确认并固化** | 新增 `ADR-0012-android-hilt.md`；设计/Android 规则改为 Hilt |

## Remaining items (non-blocking)

| ID | 说明 | 建议时点 |
|---|---|---|
| T-007 | Android `minSdk 26` 仍为设计建议值，未单独 ADR 固化 | M0-T03 初始化前确认 |
| T-001~T-006, T-008 | 真实 Provider、部署、监控等待后续里程碑决策 | 按 design/08 表 |
| M2+ 任务编号 | 合并文档中 M2～M6 仍使用 `M2-001` 风格；`TASK_BACKLOG` 对 M2+ 尚未细拆 | 进入对应里程碑前再拆 |
| 历史审阅 | `CURSOR_REVIEW_2026-07-21.md` 保留首次审阅记录 | 只读档案 |

## M0 readiness

- 文档冲突：无阻塞项
- 下一任务：`M0-T02 初始化后端工程`
- 前置验证：按 ADR-0007 在本地执行 Maven BOM 解析
