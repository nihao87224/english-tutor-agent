# English Tutor Agent Vibe Coding 执行指南

> 任务编号说明：本文件中的任务编号已经与 `docs/plans/TASK_BACKLOG.md` 对齐；若后续仍有冲突，以 `TASK_BACKLOG.md` 和 `CURRENT_TASK.md` 为准。

> 目标：让 AI 辅助编码保持需求可追踪、代码可运行、变更可 Review，而不是一次性生成大量不可控代码。

---

# 1. 项目资料结构

建议仓库：

```text
english-tutor-agent/
├── docs/
│   ├── prd/
│   ├── design/
│   ├── decisions/
│   ├── plans/
│   ├── test/
│   └── release/
├── server/
├── android/
├── scripts/
└── README.md
```

必须保留：

- PRD 正式版本；
- 本设计文档；
- ADR；
- 每个里程碑计划；
- 测试结果；
- 发布记录。

---

# 2. 标准流程

```text
需求编号
→ 设计定位
→ 任务拆解
→ 编码计划 Review
→ 小步编码
→ 自动测试
→ 实际运行
→ Code Review
→ 修复与重构
→ 用户验收
→ 合并与记录
```

禁止流程：

```text
一句“把整个项目做完”
→ AI 生成几万行代码
→ 不能运行
→ 再通过聊天猜问题
```

---

# 3. 任务卡模板

```markdown
# TASK-M0-T02 初始化后端工程

## 关联决策
- ADR-0001
- ADR-0007
- ADR-0011

## 用户价值
建立可构建、可测试且模块边界清晰的后端工程基础。

## 范围
- Maven 多模块骨架
- Spring Boot 最小启动
- Java 21 构建
- Maven Wrapper
- 基础上下文与模块边界测试

## 非范围
- MySQL/Redis/Flyway 集成（M0-T05）
- 初评业务
- Fake 或真实 AI Provider（M0-T07）
- 业务 API

## 验收
- `./mvnw clean verify` 通过
- Spring 最小上下文可启动
- 模块依赖方向测试通过
- README 含双平台构建命令

## 测试
- ContextLoad
- ArchUnit/模块边界测试

## Review 重点
- 模块依赖
- 技术版本锁定
- 配置和密钥
```

---

# 4. 给编码 Agent 的提示词规则

每次提示词应包含：

1. 当前任务编号；
2. 必须读取的文档；
3. 允许修改的目录；
4. 禁止修改的范围；
5. 验收标准；
6. 测试命令；
7. 输出变更摘要和风险。

示例：

```text
请实现 TASK-M1-T01：保存主要学习目标。

开始前必须读取：
- docs/prd/ENGLISH_TUTOR_AGENT_PRD_v1.0.0.md 中 FR-ONB-001
- docs/design/02_DETAILED_DESIGN_BACKEND.md
- docs/design/05_DATA_MODEL_AND_API_SPEC.md

只允许修改：
- server/tutor-identity/**
- server/tutor-api/**
- 对应 Flyway 和测试

要求：
1. 用户同时只能有一个主要目标；
2. 写接口支持 Idempotency-Key；
3. Controller 无业务逻辑；
4. 添加单元测试和集成测试；
5. 实际执行 mvn test；
6. 不实现初评。

完成后输出：
- 修改文件
- 关键设计
- 测试结果
- 未解决风险
```

---

# 5. 里程碑开发顺序

## M0：工程与开发基线

```text
M0-T01 建立仓库与目录基线
M0-T02 初始化后端工程
M0-T03 初始化 Android 工程
M0-T04 本地 MySQL / Redis / MinIO
M0-T05 Flyway、Testcontainers 与健康检查
M0-T06 CI 与契约校验
M0-T07 Fake LLM / ASR / TTS Provider
M0-T08 冷启动验证
```

## M1：首次使用闭环

```text
M1-T01 主要学习目标
M1-T02 学习、纠错、提醒和隐私偏好
M1-T03 Onboarding 进度恢复
M1-T04 听说读写自评
M1-T05 自适应初评会话
M1-T06 客观题评分
M1-T07 开放答案评估
M1-T08 初始能力画像
M1-T09 首个规则型今日计划
M1-T10 Android 首次使用端到端验收
```

先用 Fake AI 完成确定性流程，再接真实 Provider。

## M2：文字学习闭环

```text
M2-001 通用训练容器
M2-002 文字任务提交
M2-003 SSE 对话
M2-004 纠错 Analyzer
M2-005 Evidence 更新
M2-006 今日总结
M2-007 第二天计划调整 E2E
```

## M3：Web 表达教练

```text
M3-R00 V1.0 Web-first 路线重基线
M3-T01 Web 工程初始化与 API Client
M3-T02 Web 最短 Onboarding 与本地用户键
M3-T03 Web 今日表达教练首页
M3-T04 Web SSE 对话与流式回复
M3-T05 Web 分层纠错与自然表达面板
M3-T06 Try Again 改写/复述练习闭环
M3-T07 Web 每日总结与下一计划变化展示
M3-T08 Web Expression Coach E2E
```

## M4：语音与听力

```text
M4-T01 Android 录音状态机
M4-T02 音频上传
M4-T03 ASR Provider
M4-T04 听力播放器
M4-T05 TTS
M4-T06 低置信度确认
M4-T07 弱网重试 E2E
```

## M5：复习与长期画像

```text
M5-001 KnowledgeState
M5-002 ReviewScheduler
M5-003 迁移任务
M5-004 延迟验证
M5-005 周报
M5-006 阶段测验
```

## M6：IELTS

```text
M6-001 Part 1
M6-002 Part 2 计时与录音
M6-003 Part 3
M6-004 完整模拟状态机
M6-005 评分 Rubric
M6-006 稳定性评测
```

## M7：发布

```text
M7-001 隐私设置
M7-002 删除流程
M7-003 可观测性
M7-004 性能和成本
M7-005 Web 部署与 Android 真机矩阵
M7-006 灰度与回滚
```

---

# 6. 每个任务的完成定义

- [ ] 需求编号明确；
- [ ] 代码编译；
- [ ] 单元测试；
- [ ] 需要时有集成/UI/E2E 测试；
- [ ] 实际运行验证；
- [ ] 日志和错误可理解；
- [ ] 文档/API/迁移同步更新；
- [ ] 无密钥和敏感内容；
- [ ] AI 输出有 Schema 和降级；
- [ ] Code Review 完成；
- [ ] 用户可看到可验证结果。

---

# 7. Review 分工

## AI 自检

- 构建和测试；
- 静态分析；
- 需求矩阵；
- 边界和异常；
- 变更摘要。

## 第二轮 AI Review

使用独立上下文或不同模型，重点：

- 是否偏离 PRD；
- 是否存在安全、并发、事务问题；
- 是否过度设计；
- 测试是否真的覆盖业务规则；
- Android 生命周期是否正确；
- Prompt 和 Schema 是否稳定。

## 用户 Review

用户重点确认：

- 产品体验是否符合预期；
- 开发结果是否真实运行；
- 设计决策是否需要变更；
- 是否进入下一任务或里程碑。

---

# 8. Git 规范

分支建议：

```text
main
├── milestone/m1-onboarding
└── feature/TASK-M1-004-primary-goal
```

提交：

```text
feat(onboarding): implement primary goal [TASK-M1-004]
test(assessment): add adaptive blueprint cases [TASK-M1-006]
fix(audio): preserve recording after upload failure [TASK-M3-007]
```

Pull Request 必须包含：

- 任务和需求链接；
- 变更说明；
- 截图/录屏或 API 示例；
- 测试命令和结果；
- 数据库变更；
- 风险与回滚。

---

# 9. 缺陷处理

```text
复现步骤
→ 预期/实际
→ 关联需求
→ 日志与 traceId
→ 根因
→ 最小修复
→ 回归测试
→ 是否需要设计变更
```

禁止只通过增加 Prompt 文字掩盖领域或状态机缺陷。

---

# 10. 发布流程

```text
冻结候选版本
→ 全量自动测试
→ AI Golden Set
→ Android 真机与弱网
→ 隐私删除演练
→ Provider 降级演练
→ 数据库备份和迁移演练
→ 小范围灰度
→ 指标观察
→ 正式发布或回滚
```

发布记录应包含：

- App/Server/Prompt/Content/Schema 版本；
- 迁移编号；
- 已知问题；
- 监控面板；
- 回滚步骤；
- 下一轮数据观察点。
