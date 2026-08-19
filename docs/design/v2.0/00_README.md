# English Tutor Agent V2.0 设计文档包

> 版本：`2.0.0`  
> 状态：`V2.0 设计基线初版`  
> 日期：`2026-08-19`  
> 上游需求：`docs/prd/v2.0/ENGLISH_TUTOR_AGENT_PRD_v2.0.0.md`  
> 设计原则：`离线生产教材，运行时个性化教学；权限先于推荐；不实时生成视频`

---

## 1. 文档目标

本目录把 V2.0 PRD 转换为可以直接用于概要设计、详细设计、内容生产和 Codex 实施的技术/产品设计输入。

V2.0 不推翻 1.x 已有模块化单体和学习闭环，而是在现有能力之上增加：

1. Learning Resource 统一资源层；
2. Scenario Lesson 标准教材包；
3. 资源权限、Private Collection 与“随心学”；
4. Scenario Lesson Player 与 AI Speaking 闭环；
5. 离线内容生产 Pipeline；
6. 可由 Codex 批量预生成的文本、图片、TTS 音频和 Manifest 规范。

## 2. 文档目录

| 文件 | 作用 |
|---|---|
| `01_ARCHITECTURE.md` | V2.0 总体架构、模块职责、资源访问、推荐、权限、部署边界 |
| `02_RESOURCE_MODEL.md` | LearningResource / Collection / Asset / Entitlement / Progress 数据与领域模型 |
| `03_SCENARIO_LESSON_FLOW.md` | Scenario Lesson 状态机、页面流程、AI Speaking、Retry、恢复与降级 |
| `04_CONTENT_PIPELINE.md` | 离线课程生产、校验、TTS、图片、打包、导入、发布和版本化 Pipeline |
| `05_RESOURCE_PREGENERATION_CODEX_GUIDE.md` | **可直接交给 Codex 的资源预生成规范、72 课目录、目录结构、质量门槛和 Master Prompt** |

## 3. 与 V1.x 文档关系

V2.0 为增量设计。以下 V1.x 结论继续有效，除非 V2.0 文档明确覆盖：

- Java 21 / Spring Boot 4.1 / Spring AI 2.0；
- Maven 模块化单体；
- MySQL / Redis / S3-compatible object storage；
- Web-first；
- SSE 流式 AI 回复；
- Correction / Natural Expression / Try Again；
- Learning Evidence / Skill State / Memory / Review / Planner；
- Provider 抽象、可观测性、幂等、隐私与测试基线。

V2.0 优先复用现有 Maven 模块，不因为新增 Resource 领域立即拆出新的微服务或 Maven module。

## 4. V2.0 设计总原则

### 4.1 教材与教学分离

```text
离线内容生产
  -> JSON / Image / Audio / Transcript
  -> Object Storage + Resource Catalog
  -> 运行时直接读取

用户真实回答
  -> AI Role Play / Correction / Evaluation
  -> Evidence / Memory / Review / Planner
```

教材首屏不需要运行时 LLM。

### 4.2 资源 staging 与运行时资源分离

预生成资源工作区统一使用：

```text
resources/learning-content/v2.0/
```

该目录是**内容生产 staging workspace**，不是 Spring Boot classpath resources。

推荐结构：

```text
resources/learning-content/v2.0/
├── catalog/
├── lessons/
├── private-collections/
├── schemas/
├── reports/
└── tools/
```

其中大体积 `*.mp3/*.wav/*.webp/*.png/*.mp4` 是否提交 Git，由仓库容量策略决定；正式运行时媒体统一上传对象存储，数据库只保存 metadata 和 asset key。

明确禁止把批量学习媒体长期放到：

```text
server/**/src/main/resources/
```

避免媒体被打包进 JAR / Docker image。

## 5. 推荐实施阅读顺序

```text
V2.0 PRD
-> 01_ARCHITECTURE
-> 02_RESOURCE_MODEL
-> 03_SCENARIO_LESSON_FLOW
-> 04_CONTENT_PIPELINE
-> 05_RESOURCE_PREGENERATION_CODEX_GUIDE
-> Implementation Plan / Task Backlog（后续）
```

## 6. 关键冻结决策

1. V2.0 不以实时生成视频为发布依赖；
2. PUBLIC Scenario Lesson 使用离线预生成图片、音频和结构化课程 JSON；
3. EngFluent 作为 Private Collection，通过 `ADMIN_GRANTED` 控制访问；
4. Private Collection 默认不进入“今日学习”推荐；
5. 推荐链路必须先做 publish/access filter，再做个性化排序；
6. 72 节首发课程按 `24 scenes x A2/B1/B2` 生产；
7. 内容生产可由 Codex/脚本批量执行，但产物必须通过 Schema + 自动规则 + Reviewer + 抽检；
8. 内容资源必须版本化，学习证据绑定当时的 `resourceVersion`；
9. 关键文字信息不依赖 AI 图片中的文字，菜单/航班/Jira 等使用结构化 UI overlay；
10. 运行时只在用户回答、Role Play、纠错、评价、记忆更新和 Planner 个性化节点使用 LLM。
