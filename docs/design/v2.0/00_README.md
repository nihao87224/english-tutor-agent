# English Tutor Agent V2.0 设计文档包

> 版本：`2.0.1`
> 状态：`V2.0 双核心设计基线`
> 日期：`2026-08-19`  
> 上游需求：`docs/prd/v2.0/ENGLISH_TUTOR_AGENT_PRD_v2.0.0.md`  
> 设计原则：`离线生产教材，运行时个性化教学；权限先于推荐；不实时生成视频`

---

## 1. 文档目标

本目录把 V2.0 PRD 转换为可以直接用于概要设计、详细设计、内容生产和 Codex 实施的技术/产品设计输入。

V2.0 不推翻 1.x 已有模块化单体和学习闭环，而是在现有能力之上增加：

1. Personalized Tutor Core 与 Daily Learning Prescription；
2. Skill Graph、Learner Model 和确定性教学策略；
3. Learning Resource 与 Skill Unit 统一资源层；
4. Lin Muen Season / Episode 沉浸体验层；
5. Skill Unit 与 Episode Mapping；
6. 资源权限、Private Collection 与“随心学”；
7. AI Speaking、Evidence、Memory、Review 闭环；
8. 可批量预生成的文本、图片、TTS 音频和 Manifest Pipeline。

## 2. 文档目录

| 文件 | 作用 |
|---|---|
| `01_ARCHITECTURE.md` | **V2.0 权威架构基线：系统上下文、现有 Maven 模块映射、领域与数据所有权、关键时序、AI/权限/内容发布边界、部署与测试架构** |
| `02_RESOURCE_MODEL.md` | LearningResource / Collection / Asset / Entitlement / Progress 数据与领域模型 |
| `03_SCENARIO_LESSON_FLOW.md` | Scenario Lesson 状态机、页面流程、AI Speaking、Retry、恢复与降级 |
| `04_CONTENT_PIPELINE.md` | 离线课程生产、校验、TTS、图片、打包、导入、发布和版本化 Pipeline |
| `05_RESOURCE_PREGENERATION_CODEX_GUIDE.md` | **可直接交给 Codex 的 72 个 Skill Unit Variant、Episode Mapping、目录结构与质量门槛** |
| `07_CURRICULUM_AND_RECOMMENDATION_DESIGN.md` | Skill Unit、课程图谱与教学目标设计 |
| `08_SKILL_GRAPH_AND_LEARNING_STATE_MODEL.md` | Learner Model、Evidence、Mastery 与 Weak Point |
| `09_RECOMMENDATION_ENGINE_DESIGN.md` | Daily Learning Prescription、教学策略与推荐解释 |
| `11_PERSONALIZED_TUTOR_AND_IMMERSIVE_LEARNING_DESIGN.md` | **AI 私教专业性与 Lin Muen 沉浸体验的权威桥接设计** |
| `12_HIGH_LEVEL_DESIGN.md` | Web 交互到逻辑组件、P0 用例、数据组、API 分组与验收场景的概要设计 |
| `13_BACKEND_DETAILED_DESIGN.md` | 后端聚合、策略、Application Service、端口、状态机、事务、AI 重试与测试详细设计 |
| `14_API_AND_DATABASE_DESIGN.md` | V2 canonical REST/SSE、DTO、错误码、幂等和 Flyway V19–V27 数据库设计 |

配套交付物：

- `docs/ui/v2.0/ENGLISH_TUTOR_AGENT_WEB_UI_PROTOTYPE_v2.0.html`：可交互 Web 原型；
- `docs/plans/v2.0/CODEX_TASK_BREAKDOWN.md`：按依赖排序、一次一张任务卡执行的开发拆解。

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

`01_ARCHITECTURE.md` 同时区分仓库已有的 `REUSE` 基础能力、V2 必须补齐的 `V2-P0` 能力和后续 `LATER` 演进，不能把目标架构误读为已经完成的实现。

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

### 4.3 教学决策与体验呈现分离

```text
Learner Model + Pedagogical Policy
  -> Daily Learning Prescription
  -> Skill Unit
  -> Episode Mapping
  -> Lin Muen Story / Mission / Dialogue
  -> Evidence
```

AI 私教决定今天学什么和怎么练；Lin Muen 决定训练如何以连续、真实、温暖的方式呈现。Season 不作为强制教学顺序。

## 5. 推荐实施阅读顺序

```text
V2.0 PRD
-> 01_ARCHITECTURE
-> 11_PERSONALIZED_TUTOR_AND_IMMERSIVE_LEARNING_DESIGN
-> 07_CURRICULUM_AND_RECOMMENDATION_DESIGN
-> 08_SKILL_GRAPH_AND_LEARNING_STATE_MODEL
-> 09_RECOMMENDATION_ENGINE_DESIGN
-> 02_RESOURCE_MODEL
-> 03_SCENARIO_LESSON_FLOW
-> 04_CONTENT_PIPELINE
-> 05_RESOURCE_PREGENERATION_CODEX_GUIDE
-> Web UI Prototype
-> 12_HIGH_LEVEL_DESIGN
-> 13_BACKEND_DETAILED_DESIGN
-> 14_API_AND_DATABASE_DESIGN
-> CODEX_TASK_BREAKDOWN
```

## 6. 关键冻结决策

1. V2.0 不以实时生成视频为发布依赖；
2. PUBLIC Scenario Lesson 使用离线预生成图片、音频和结构化课程 JSON；
3. EngFluent 作为 Private Collection，通过 `ADMIN_GRANTED` 控制访问；
4. Private Collection 默认不进入“今日学习”推荐；
5. 推荐链路必须先做 publish/access filter，再做个性化排序；
6. 72 个首发 Skill Unit Variant 按 `24 scenes x A2/B1/B2` 生产；
7. 内容生产可由 Codex/脚本批量执行，但产物必须通过 Schema + 自动规则 + Reviewer + 抽检；
8. 内容资源必须版本化，学习证据绑定当时的 `resourceVersion`；
9. 关键文字信息不依赖 AI 图片中的文字，菜单/航班/Jira 等使用结构化 UI overlay；
10. 运行时只在用户回答、Role Play、纠错、评价、记忆更新和 Planner 个性化节点使用 LLM；
11. AI 私教是决策核心，Lin Muen 是体验核心；两者不可互相替代；
12. Skill Graph 与 Experience Graph 分离，通过 Episode Mapping 连接；
13. Daily Learning Prescription 先选择 Skill Unit 和教学策略，再匹配 Episode；
14. Season 1 的 10 个 Episode 不是所有用户统一的十节固定课程；
15. 课程资源必须声明 Evidence Criteria、适配条件和可训练 Skill。
16. 现有八个后端 Maven 模块继续作为 V2.0 部署与依赖基线；新增业务能力优先使用 package + application port 表达；
17. Web 是 V2.0 P0 主客户端，API 同时保持后续 Android Compose/UDF、离线缓存和半双工 PTT 的兼容边界；
18. 具体 OpenAPI、JSON Schema、数据库 DDL、推荐阈值和媒体访问方案必须由专项契约冻结，架构文档不擅自替代这些契约。
