# English Tutor Agent 概要设计与详细设计文档包

> 版本：`1.0.0`  
> 状态：`设计基线初版，含 2026-08-10 Web-first 修订`
> 基准日期：`2026-08-10`
> 上游输入：`ENGLISH_TUTOR_AGENT_PRD_v1.0.0.md`  
> 主要客户端：Web
> 后端形态：模块化单体  
> 文档用途：开发计划、任务拆解、编码、测试、Review 与发布设计的直接输入。

---

## 1. 文档目标

本设计文档将 PRD 中的产品需求转换为可实施的系统方案，回答以下问题：

1. 系统由哪些模块构成，职责如何划分；
2. Web、后端、AI、数据与内容服务如何协作；
3. 首次评估、学习计划、训练、纠错、复习和阶段测验如何运行；
4. 学习者画像如何根据证据更新；
5. API、数据模型、状态机、异常和幂等如何设计；
6. 如何测试模型输出、学习算法和端到端流程；
7. 如何按照 Vibe Coding 流程分阶段交付并 Review。

## 2. 设计边界

### 2.1 本版本包含

- 系统上下文与总体架构；
- Web 客户端架构；
- 后端模块与领域模型；
- Agent 与学习引擎设计；
- 文字表达、纠错、Try Again 与流式响应链路；
- 数据库与对象存储设计；
- API 与事件协议；
- 安全、隐私、可观测性；
- 测试与发布质量门禁；
- PRD 需求追踪矩阵；
- Vibe Coding 执行规范。

### 2.2 本版本暂不锁定

以下属于实施期可替换配置，不改变业务设计：

- 具体 LLM、ASR、TTS 厂商；
- 云厂商和对象存储品牌；
- 生产数据库托管方式；
- 最终视觉稿中的颜色、字体、动效；
- 商业订阅与支付能力；
- 全双工实时语音通信协议。

## 3. 核心架构结论

1. 第一版后端采用**模块化单体**，不拆微服务；
2. 业务规则与模型调用解耦，LLM 只承担擅长的理解和内容生成；
3. 学习计划优先由规则、画像和证据计算决定，再由模型生成具体内容；
4. Web V1.0 使用组件化 UI、单向数据流和轻量本地状态，优先验证文字表达教练闭环；
5. Android 与半双工语音作为后续阶段能力，保留既有设计但不作为 V1.0 发布门槛；
6. 对话文字使用 SSE 流式返回；实时全双工语音作为后续演进；
7. 原始录音进入对象存储，结构化学习数据进入关系数据库；
8. 所有 AI 输出必须结构化校验、可降级、可追踪；
9. 学习者画像的变化必须来自可回溯的学习证据；
10. 每个开发里程碑都必须形成可运行的端到端纵向闭环。

## 4. 技术基线

### 4.1 后端

- Java 21 LTS；
- Spring Boot 4.1.0；
- Spring AI 2.0.0；
- Maven 多模块；
- Spring MVC + SSE；
- MySQL 8.x；
- Redis 7.x；
- S3 兼容对象存储；
- Flyway；
- Micrometer / OpenTelemetry；
- Testcontainers。

### 4.2 Web

- TypeScript；
- React 或同等组件化框架；
- Vite 或同等轻量构建工具；
- Fetch/EventSource 或同等 SSE 客户端；
- TanStack Query 或同等服务端状态管理；
- Vitest + Testing Library；
- Playwright E2E；
- 轻量 localStorage/sessionStorage 保存开发期用户键和草稿。

### 4.3 Android（后续阶段）

- Kotlin；
- Jetpack Compose + Material 3；
- ViewModel + StateFlow + 单向数据流；
- Navigation Compose；
- Room；
- DataStore；
- WorkManager；
- MediaRecorder / AudioRecord；
- AndroidX Media3 ExoPlayer；
- Retrofit/OkHttp 或同等 HTTP 客户端；
- Hilt 依赖注入。

> 依赖的小版本在初始化项目时统一锁定到版本目录中，不在业务代码分散声明。

## 5. 文档目录

| 文件 | 作用 |
|---|---|
| `01_HIGH_LEVEL_DESIGN.md` | 概要设计：系统边界、架构、模块、部署与核心流程 |
| `02_DETAILED_DESIGN_BACKEND.md` | 后端详细设计：领域、应用服务、事务、任务和工程结构 |
| `03_DETAILED_DESIGN_WEB.md` | Web 详细设计：表达教练页面、SSE、纠错面板和 E2E |
| `03_DETAILED_DESIGN_ANDROID.md` | Android 详细设计：模块、状态、导航、音频和弱网恢复 |
| `04_DETAILED_DESIGN_AGENT_LEARNING_ENGINE.md` | Agent、初评、自适应计划、纠错、复习与能力更新 |
| `05_DATA_MODEL_AND_API_SPEC.md` | 数据模型、表结构、API、SSE 事件、错误码和幂等 |
| `06_SECURITY_OBSERVABILITY_TESTING.md` | 安全、隐私、可观测性、测试策略与发布门禁 |
| `07_REQUIREMENTS_TRACEABILITY.md` | PRD 需求到模块、接口、数据与测试的映射 |
| `08_ARCHITECTURE_DECISIONS_AND_RISKS.md` | 已确认架构决策、实施期选择与风险控制 |
| `09_VIBE_CODING_EXECUTION_GUIDE.md` | 后续开发、Review、测试和发布协作流程 |
| `ENGLISH_TUTOR_AGENT_DESIGN_v1.0.0.md` | 合并版完整文档 |

## 6. 阅读顺序

建议依次阅读：

```text
PRD
→ 01 概要设计
→ 04 Agent 与学习引擎
→ 05 数据与 API
→ 02 后端详细设计
→ 03 Web 详细设计
→ 03 Android 详细设计（后续阶段）
→ 06 测试与安全
→ 07 需求追踪
→ 09 开发执行指南
```

## 7. 分阶段开发门禁

### 7.1 M0 工程初始化门禁

- [x] 用户确认概要设计主要架构；
- [x] 用户确认 Web-first 表达教练作为 V1.0 首版交互；
- [x] PRD、概要设计、详细设计已进入项目 `docs/`；
- [x] 已生成实施计划、任务 Backlog 和任务编号规则；
- [x] M0/M1 默认使用 Fake Provider，不要求真实模型密钥；
- [ ] 当前任务计划经过 Review；
- [ ] Git 基线提交完成。

### 7.2 真实 Provider 接入门禁

在接入具体 LLM、ASR、TTS 前完成：

- [ ] 选择 Provider 与模型；
- [ ] 记录成本、隐私、延迟、结构化输出和降级评估；
- [ ] 建立对应回归样本和 Provider 集成测试。

### 7.3 发布门禁

生产部署环境、监控、密钥管理、数据保留和回滚方案在 M6 发布前确认，
不阻塞 M0 工程初始化。
