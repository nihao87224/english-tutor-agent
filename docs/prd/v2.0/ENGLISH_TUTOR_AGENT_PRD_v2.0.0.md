# English Tutor Agent 产品需求文档（PRD）

> 文档版本：`2.0.1`
> 文档状态：`V2.0 双核心产品需求基线`
> 产品阶段：`个性化 AI 私教 + Lin Muen 沉浸式情景陪伴`
> 更新日期：`2026-08-19`  
> 主要客户端：`Web-first，兼容后续 Android`  
> 文档用途：作为 V2.0 概要设计、详细设计、UI 设计、开发拆解、内容生产、测试验收和发布评审的产品依据。

---

# 0. 文档说明

## 0.1 V2.0 背景

English Tutor Agent 1.x 已建立并验证基础学习闭环，包括：

- 用户目标与偏好；
- 初始能力评估；
- 今日学习计划；
- 文本训练 Session；
- SSE 流式 AI 对话；
- 分层纠错与自然表达建议；
- Try Again 再输出；
- 学习证据、总结与下一计划调整；
- 学习记忆和复习相关能力。

V2.0 不重新发明这些能力，而是在现有 Agent 学习闭环之上补齐一个关键缺口：

> **AI 知道用户应该学什么之后，手里还需要有一套质量稳定、可快速加载、可被推荐和复用的教学资源，并且能够主动把这些资源转化成真实口语训练。**

因此 V2.0 的核心不是“增加视频课程”，而是建立：

1. 能持续识别用户目标、能力缺口、遗忘风险和近期诉求的 Personalized Tutor Core；
2. 可解释、可测试的每日课程处方与教学策略；
3. 标准化 Learning Resource 与 Skill Unit 资源体系；
4. 由 Lin Muen 承载的 Season / Episode 沉浸式情景学习体验；
5. 听力输入 → 理解 → 表达学习 → 开口 → Role Play → Correction → Retry → Evidence → Memory 的完整闭环；
6. “随心学”私有授权课程区；
7. 面向后续大规模内容生产的离线资源生成体系。

## 0.2 V2.0 一句话定义

> **让 AI 像专业私教一样每天安排最适合用户的训练，并由 Lin Muen 把训练转化成值得参与、愿意坚持的沉浸式英语经历。**

## 0.3 本文档不包含

以下内容在后续专项文档中定义：

- 具体数据库表结构；
- REST/OpenAPI 接口字段；
- Agent 编排实现细节；
- TTS/ASR/LLM 厂商和模型最终选型；
- 对象存储/CDN 具体部署方案；
- Prompt 全文和模型参数；
- UI 像素级视觉规范；
- EngFluent 等第三方资源的法律授权结论。

## 0.4 需求优先级

| 优先级 | 定义 |
|---|---|
| P0 | V2.0 核心闭环必须具备，缺失则不能称为 V2.0 |
| P1 | V2.0 应具备，可在 2.0.x 小版本补齐 |
| P2 | 后续增强，不阻塞 V2.0 发布 |

---

# 1. 产品定位与版本演进

## 1.1 产品定位

English Tutor Agent 面向具备一定英语基础、但真实听说输出能力不足的中文母语成年用户。

产品长期定位仍然是：

> **用户决定为什么学；Agent 决定当前学什么、怎么练以及何时复习。**

V2.0 在该定位下新增一个重要原则：

> **个性化的是训练内容、训练顺序和交互反馈，而不是每次都实时生成昂贵的视频素材。**

V2.0 冻结“双核心”定位：

1. **AI 私教核心**：以用户目标、Skill State、Error Memory、Evidence、遗忘风险和可用时间为依据，生成每日学习处方；
2. **Lin Muen 沉浸体验核心**：以一致的人物、故事、视觉、声音和关系承载训练，让用户在真实任务中持续开口。

两者关系：

```text
AI 私教决定教学目标与训练策略
        ↓
系统选择匹配的 Skill Unit
        ↓
Lin Muen Episode 承载情景、任务与陪伴
        ↓
用户输出形成 Evidence
        ↓
AI 私教更新明日处方
```

Lin Muen 不替代 Planner、Skill Graph 或教学规则；剧情连续性不得强迫用户学习已经掌握或当前不适合的内容。

## 1.2 版本演进

| 版本 | 核心能力 | 产品价值 |
|---|---|---|
| 1.0 | 表达、纠错、Try Again | 帮用户把一句话说得更正确、更自然 |
| 1.1 | 评估、计划、记忆、复习 | AI 开始知道用户应该学什么 |
| 2.0 | 学习资源、场景训练、主动输出、随心学 | AI 手里有稳定教材，并能组织主动训练 |
| 2.x | 语音评分、Imitation、更多口音/资源 | 强化听说和自然表达 |
| 3.0+ | 更强移动端、实时语音、多模态 | 更接近长期陪伴式真人私教体验 |

---

# 2. V2.0 目标与非目标

## 2.1 用户目标

V2.0 用户应能够：

1. 打开产品后直接获得适合当前能力和目标的学习材料；
2. 先通过音频和场景获得可理解输入；
3. 从“听懂、看懂”自然过渡到“自己说出来”；
4. 在真实生活、职场、技术、旅行、IELTS 等场景完成表达任务；
5. 针对同一场景按 A2/B1/B2 不同难度逐步训练；
6. 对说错、不自然、组织困难的内容获得即时反馈并 Retry；
7. 将训练结果继续写入原有 Memory / Review / Planner 闭环；
8. 对已授权的私有课程，可进入“随心学”自主学习。

## 2.2 产品目标

### P0 目标

- 建立 Learner Model，统一目标、Skill State、Error Memory、Evidence、Review State 和临时诉求；
- 建立可解释、可测试的 Daily Learning Prescription；
- 建立前置技能、掌握度、难度适配、间隔复习、交错练习、迁移和 Retry 教学策略；
- 建立统一 Learning Resource 模型；
- 建立 Skill Unit 与 Episode Experience 的双图谱及映射；
- 建立 Scenario Lesson 标准资源格式；
- 首发覆盖 8 个主题 × 3 个场景 × 3 个等级，共 72 个 Skill Unit Variant；
- 建立 Lin Muen Season 1 的 10 个 Episode，作为可被个性化课程选择的沉浸式体验容器；
- 支持图片、TTS 音频、Transcript、重点表达、理解题和口语任务；
- 将标准资源接入今日学习推荐；
- 完成从资源学习到 Speaking / Correction / Retry / Memory 的闭环；
- 建立私有授权资源能力；
- 建立独立“随心学”入口，首个私有资源集合为 EngFluent；
- 未授权用户不会在默认推荐链路中遇到无权访问资源。

### P1 目标

- Workplace / Tech 场景增加 Singapore English 音频变体；
- 支持收藏、最近学习和学习进度；
- 支持用户主动选择是否允许私有随心学资源进入每日计划；
- 内容管理后台或轻量管理页面；
- 支持更多 Training Type，如 Question Builder、Process Explanation。

### P2 目标

- Imitation 与发音/节奏评分；
- British English 等更多口音；
- 更丰富的动态视觉/轻动画；
- 根据用户临时需求即时生成一次性音频课；
- 生成式视频作为实验功能。

## 2.3 非目标

V2.0 不追求：

- 为每个用户实时生成教学视频；
- 建设传统 MOOC 式大型课程商城；
- 让用户每天在海量目录中自行选课；
- 复制第三方课程内容成为公共教材；
- 首发覆盖 A1、C1、C2 全等级；
- 首发完成专业级发音评测；
- 用视频数量作为学习价值指标；
- 让第三方私有课程直接绕过权限系统进入推荐；
- 让所有用户按 EP001 → EP010 接受同一套固定教学顺序；
- 让 Lin Muen 或 LLM 凭主观感觉替代确定性的掌握、复习、难度和权限规则；
- 用剧情播放量或角色互动量替代能力改善指标。

---

# 3. 产品设计原则

## 3.1 今日学习仍是主入口

用户打开产品后，第一优先级仍然是：

> **今天最值得练什么？**

不把主页面改造成课程商城。

## 3.2 输入必须服务于输出

音频、图片、Transcript 不是终点。

标准学习链路必须尽可能走到：

```text
场景输入
→ 听力理解
→ 重点表达
→ 主动回答
→ Role Play / 长段输出
→ Correction
→ Retry
→ Learning Evidence
→ Memory / Review
```

## 3.3 教材离线生成，教学实时个性化

V2.0 默认策略：

- 图片：预生成；
- TTS：预生成；
- Transcript：预生成；
- 理解题：预生成；
- Key Expressions：预生成；
- 训练目标：预生成；
- 推荐排序：实时个性化；
- 用户回答反馈：实时 AI；
- Role Play：实时 AI；
- Correction / Evaluation：实时 AI。

原则：

> **教材一次生产，多次复用；真正需要个性化的“教学互动”才使用运行时模型。**

## 3.4 权限先于推荐

推荐系统不能先推荐，再在点击时提示无权限。

正确顺序必须是：

```text
候选资源
→ 发布状态过滤
→ 权限过滤
→ 等级/目标/弱点匹配
→ 推荐排序
→ 今日计划
```

## 3.5 第三方资源与自研资源解耦

所有资源统一抽象，但第三方私有内容必须独立管理：

- 独立 Provider；
- 独立 Collection；
- 独立 Access Scope；
- 独立进度；
- 不影响 INTERNAL 标准学习资源。

## 3.6 教学决策先于情景包装

系统必须先根据用户状态确定训练目标、难度、Training Type 和预期 Evidence，再选择最适合承载该训练的 Lin Muen Episode。

禁止：

```text
先决定今天推进哪一集剧情
→ 再为该剧情寻找一个看似相关的知识点
```

正确顺序：

```text
识别能力缺口或到期复习
→ 选择 Skill Unit 与教学策略
→ 匹配 Episode / Scene
→ 生成沉浸式任务
```

## 3.7 专业教学与情感陪伴同时成立

- AI 私教的判断必须可解释、可追踪、可测试；
- Lin Muen 的反馈必须温暖、自然、非评判式；
- 温暖表达不能省略必要纠错和 Retry；
- 科学训练不能退化成冷冰冰的题库与分数；
- 用户可以调整情景陪伴强度，但不能绕过必要的能力前置和安全规则。

---

# 4. V2.0 信息架构

## 4.1 一级结构

```text
English Tutor Agent
│
├── 今日学习
│   ├── 今日复习
│   ├── 场景听力/口语课程
│   ├── 专项口语任务
│   └── AI 对话
│
├── AI 对话
│
├── 随心学（权限控制）
│   ├── 私有课程集合
│   ├── English Fluency Course
│   ├── 最近学习
│   ├── 收藏（P1）
│   └── 学习进度
│
└── 我的
    ├── 能力画像
    ├── 高频错误
    ├── 学习记录
    ├── 复习
    └── 权限/设置
```

## 4.2 “随心学”入口规则

### P0 默认规则

- 用户至少拥有一个 `ADMIN_GRANTED` 私有资源集合时，显示“随心学”；
- 没有任何私有资源权限时，默认隐藏该一级入口；
- 前端隐藏不是安全边界，后端必须再次进行 entitlement 校验；
- 权限撤销后，资源立即不可继续访问，但保留用户历史学习记录；
- 后续可增加“显示锁定入口”的商业化模式，但不属于 V2.0 P0。

---

# 5. 统一 Learning Resource 模型

## 5.1 资源类型

产品层统一定义 `LearningResource`，首期支持：

- `SCENARIO_LESSON`：自研标准场景课；
- `VIDEO_COURSE`：第三方或私有视频课程；
- `AUDIO_LESSON`：音频课程；
- `ROLEPLAY_TEMPLATE`：Role Play 模板；
- `SPEAKING_TASK`：专项口语任务。

未来可扩展：

- ARTICLE；
- PODCAST；
- USER_IMPORTED_CONTENT；
- GENERATED_MICRO_LESSON。

## 5.2 核心元数据

每个资源至少包含：

- resourceId；
- provider；
- collectionId；
- resourceType；
- title；
- description；
- language；
- CEFR Level；
- topic；
- scene；
- communicationGoal；
- skillFocus；
- grammarFocus；
- vocabularyTags；
- accent；
- estimatedMinutes；
- accessScope；
- publishStatus；
- version；
- asset references；
- createdAt / updatedAt。

## 5.3 Access Scope

P0 定义：

- `PUBLIC`：所有用户可访问；
- `ADMIN_GRANTED`：管理员明确授权后可访问；
- `ADMIN_ONLY`：仅管理员或内容审核使用；
- `DISABLED`：任何学习流程不可访问。

未来可扩展：

- SUBSCRIPTION；
- ORGANIZATION；
- LICENSED_USER。

---

# 6. 标准课程：Scenario Lesson

## 6.1 产品定义

Scenario Lesson 是 V2.0 的核心教材资源，不等于“听一段 MP3”。

一节完整 Scenario Lesson 必须是一个 Learning Package：

```text
ScenarioLesson
├── Cover / Scene Image
├── Scene Context
├── Communication Goal
├── Audio
├── Transcript
├── Key Vocabulary
├── Key Expressions
├── Comprehension Questions
├── Speaking Tasks
├── Role Play Goal
├── Difficulty
├── Grammar Focus
├── Review Points
└── Metadata
```

## 6.2 单课建议时长

- A2：6–10 分钟；
- B1：8–12 分钟；
- B2：10–15 分钟。

一节课不追求知识点多，而追求至少完成一次真实输出。

## 6.3 标准学习流程

### Step 1：理解场景

用户通过图片、简短文字知道：

- 我在哪里；
- 对方是谁；
- 发生了什么；
- 我最终需要完成什么沟通任务。

### Step 2：First Listen

默认第一次不强制显示 Transcript。

用户先听 40–120 秒音频。

### Step 3：Comprehension Check

2–4 个理解题，验证用户是否抓住：

- 主旨；
- 关键事实；
- 对方意图；
- 某个表达在场景中的含义。

### Step 4：Transcript & Expressions

显示 Transcript，并突出：

- 3–8 个 Key Expressions；
- 必要词汇；
- 一个最值得迁移的表达模式。

不把该步骤变成传统语法长课。

### Step 5：Guided Speaking

先进行低压力输出，例如：

- Quick Answer；
- Sentence Builder；
- Question Builder；
- Paraphrase。

### Step 6：Active Speaking / Role Play

用户扮演场景中的一个角色，AI 扮演另一个角色。

核心目标不是复读原文，而是完成 communication goal。

### Step 7：Correction & Natural Expression

沿用现有分层纠错能力：

- 是否影响理解；
- 语法问题；
- 词汇/搭配；
- 中式英语；
- 更自然表达。

### Step 8：Try Again

关键错误或明显表达提升点需要用户重新说一次。

### Step 9：Evidence / Memory / Review

完成后产生结构化学习证据，并影响：

- skill state；
- weak points；
- learned expressions；
- review tasks；
- tomorrow plan。

## 6.4 Skill Unit 与 Episode Experience

V2.0 教学最小单位为 `Skill Unit`，体验容器为 `Episode`。

Skill Unit 定义 communication goal、target / supporting skills、prerequisites、difficulty、scaffolding、training types、common error mappings、evidence criteria 和 review template。

Episode 定义 Lin Muen 故事背景、场景与角色关系、可承载的 Skill Unit、图片、音频、Dialogue、Mission、难度变体和 story continuity。

同一 Episode 可以服务不同能力状态的用户；同一 Skill Unit 也可以映射到不同 Episode，以验证能力迁移。

---

# 7. 首发内容体系

## 7.1 内容组织原则

V2.0 不以 Grammar Unit 作为一级目录，而以用户真实想完成的 communication task 组织课程。

首发内容由“能力资源图谱”和“沉浸体验图谱”共同组成。

能力资源图谱包含 8 个一级主题：

1. Daily Life；
2. Food & Shopping；
3. Travel；
4. Workplace；
5. Tech & Engineering；
6. Social；
7. Ideas & Stories；
8. IELTS Speaking。

每个主题首发 3 个核心场景，每个场景提供 A2 / B1 / B2 三个等级。

能力资源总计：

`8 × 3 × 3 = 72 Skill Unit Variants`

Lin Muen 沉浸体验图谱首发 Season 1：`Getting Closer to English`，包含 `EP001`–`EP010`。Episode 不替代 72 个能力资源，也不是强制教学顺序；它通过 `episodeMappings` 承载一个或多个 Skill Unit Variant。

用户看到的是连贯世界，Planner 操作的是可组合的能力单元。

## 7.2 72 个首发 Skill Unit Variant 目录

| 一级主题 | 场景 1 | 场景 2 | 场景 3 |
|---|---|---|---|
| Daily Life | Talk About Daily Routine | Make Weekend Plans | Handle an Everyday Problem |
| Food & Shopping | Order at a Coffee Shop | Eat at a Restaurant | Return or Exchange an Item |
| Travel | Airport Check-in | Hotel Check-in | Handle a Travel Disruption |
| Workplace | Give a Daily Update | Ask for Help / Clarification | Explain a Work Problem |
| Tech & Engineering | Explain a Bug | Report a Deployment Incident | Discuss a Technical Trade-off |
| Social | Meet Someone / Small Talk | Invite / Accept / Decline | Apologize / Appreciate / Respond |
| Ideas & Stories | Retell an Experience | Express and Support an Opinion | Explain a Process |
| IELTS Speaking | Part 1 Personal Topics | Part 2 Long Turn | Part 3 Discussion |

以上 24 个场景分别生产 A2、B1、B2 版本。

## 7.3 Workplace 内容目标示例

### Give a Daily Update

A2：

- Today I worked on...；
- I finished...；
- Next, I will...。

B1：

- I made progress on...；
- I ran into an issue with...；
- My next step is...。

B2：

- The main blocker at the moment is...；
- We are still on track, provided that...；
- I would recommend prioritizing...。

### Ask for Help / Clarification

逐步训练：

- Could you explain that again?；
- Could you walk me through that?；
- Just to make sure I understood correctly...；
- Would you mind clarifying what you mean by...?。

## 7.4 Tech & Engineering 差异化定位

该主题作为产品特色内容重点建设，首发覆盖：

- bug 描述；
- 日志和排查结果；
- database / API / deployment 问题；
- root cause；
- workaround；
- trade-off；
- timeline / risk；
- code review / requirement discussion。

V2.0 首发 3 个场景，但内容体系必须支持持续扩展至 20+ 技术职场场景。

---

# 8. CEFR 难度体系

## 8.1 首发等级

V2.0 首期只生产：

- A2；
- B1；
- B2。

不以 A1 或 C1/C2 作为首发范围。

## 8.2 A2 内容规范

目标：让用户用短句完成最基本沟通。

建议：

- Audio：40–60 秒；
- 语速：正常偏慢；
- 句型：简单句为主；
- 生词：3–5 个；
- Key Expressions：3–4 个；
- Speaking：单句或 2–3 句回答；
- Role Play：低分支、明确目标。

## 8.3 B1 内容规范

目标：完整完成常见生活和工作沟通。

建议：

- Audio：60–90 秒；
- 语速：接近日常自然语速；
- 句型：简单复合句；
- 生词：5–8 个；
- Key Expressions：4–6 个；
- Speaking：20–45 秒；
- Role Play：允许不同表达方式完成任务。

## 8.4 B2 内容规范

目标：处理不确定、问题、解释、比较和观点。

建议：

- Audio：75–120 秒；
- 语速：自然；
- 句型：更复杂从句和连接结构；
- Key Expressions：5–8 个；
- Speaking：45–90 秒；
- Role Play：包含澄清、异议、问题解决或长段表达。

## 8.5 同场景升级原则

升级不是只替换几个高级词。

示例 Coffee Shop：

- A2：完成点单；
- B1：定制饮品并询问成分；
- B2：发现订单错误后礼貌澄清并解决问题。

难度升级应体现 communication task 的复杂度升级。

---

# 9. 图片与视觉资源策略

## 9.1 原则

图片的目标不是“好看”，而是：

> **让用户在 1–2 秒内理解场景、角色和任务。**

## 9.2 风格

首选：

- 写实或轻写实；
- 干净；
- 生活化；
- 第一视角或明确角色关系；
- 不依赖图片中文字表达关键信息。

## 9.3 图片内容

典型图片：

- 咖啡店柜台；
- 餐厅桌面与服务员；
- 机场值机柜台；
- 酒店前台；
- 办公室会议室；
- 视频会议；
- 工程师查看日志；
- 两人社交场景。

## 9.4 结构化 UI Overlay

菜单、航班信息、Jira Ticket、会议议程等关键信息不得依赖生成图片直接写字。

应采用：

```text
背景图片
+
HTML / Native UI Overlay
```

保证文字准确和可交互。

---

# 10. 音频与 TTS 策略

## 10.1 P0 音频要求

每节 Scenario Lesson 至少一份正式音频。

必须保存元数据：

- voiceId；
- accent；
- speakerRole；
- speechRate；
- generatedAt；
- sourceVersion。

## 10.2 口音策略

### P0

- American Neutral 作为大部分公共资源默认音色。

### P1

Workplace / Tech 重点资源增加：

- Singapore English / Singapore-accented English 版本。

### P2

- British English；
- 其他区域口音；
- 多人会议混合口音。

## 10.3 音频交互

至少支持：

- 播放/暂停；
- 单句重播；
- 0.8x / 1.0x / 1.1x（具体倍率 UI 阶段确认）；
- Transcript 与当前句联动（P1）；
- 首听隐藏 Transcript 的训练模式。

---

# 11. Training Type 体系

V2.0 不把所有课程限制为同一种练习。

## 11.1 P0 Training Type

### Quick Answer

目标：快速组织完整句子。

### Sentence Builder

目标：使用指定结构完成表达。

### Story Retelling

目标：把听到/读到的信息重新组织成自己的语言。

### Idea Expression

目标：表达观点并给出原因或例子。

### Role Play

目标：在真实沟通任务中灵活调用表达。

### Long Speaking

目标：连续表达 45–120 秒，强化组织和流畅度。

## 11.2 P1 Training Type

- Question Builder；
- Process Explanation；
- Compare & Choose；
- Clarification Drill。

## 11.3 P2 Training Type

- Imitation；
- Shadowing；
- Pronunciation Drill。

---

# 12. 今日学习与资源推荐

## 12.1 推荐输入

Planner 在选择 Scenario Lesson 时可使用：

- 用户目标；
- CEFR 当前估计；
- weak points；
- 最近错误；
- 最近已学主题；
- 最近完成率；
- 最近难度表现；
- 用户近期偏好；
- 可用学习时间；
- Resource entitlement。

## 12.2 推荐优先级

默认优先：

1. 与近期弱点相关；
2. 与主目标相关；
3. 当前等级适配；
4. 最近没有重复；
5. 能在当日剩余时间完成；
6. 能产生主动输出。

## 12.3 去重复规则

避免连续多日推荐完全相同课程。

允许：

- 因复习需要重新出现；
- 同场景升级更高难度；
- 同场景以不同 Training Type 重练。

## 12.4 权限过滤

Planner 的候选资源必须已经通过 Access Filter。

默认情况下，EngFluent 私有课程不进入今日推荐。

P1 可增加：

> “允许将随心学内容加入每日学习计划”

只有满足以下条件才可进入候选池：

```text
用户拥有权限
AND
用户主动开启该选项
AND
资源仍处于可用状态
```

权限被撤销时，Planner 自动回退到 INTERNAL 公共资源，不产生“推荐后无法打开”的失败体验。

## 12.5 Daily Learning Prescription

今日学习输出必须是一份教学处方，而不只是 Resource 列表。至少包含：

- 今日优先目标；
- target skills 与选择原因；
- 到期复习块；
- 新技能或巩固块；
- 主动输出块；
- 可选迁移块；
- 难度与支架级别；
- Lin Muen experience context；
- 预计时间；
- 完成条件和预期 Evidence。

P0 教学策略至少覆盖 prerequisite、retrieval practice、spaced practice、difficulty fit、interleaving、transfer、feedback and retry。

核心决策由确定性规则负责；LLM 可以解释推荐、生成受约束的互动变化，但不能直接写入 mastery 或自行改变到期复习。

---

# 13. 随心学：私有授权课程区

## 13.1 产品定位

“随心学”解决的是：

> **用户今天不想完全按照 AI 计划，而想自主学习一套自己已经获得访问权的课程。**

它不是 V2.0 默认学习主流程，也不替代今日学习。

## 13.2 首个 Collection：English Fluency Course

首期支持管理员为指定用户开启该 Collection。

产品层不要把权限逻辑硬编码为 EngFluent 专用，应该抽象为通用 Private Collection。

## 13.3 P0 功能

- Collection 列表；
- 课程/章节目录；
- 视频播放；
- 上次学习位置；
- 完成状态；
- 管理员授权/撤销；
- 后端访问校验；
- 资源不可用时友好处理。

## 13.4 P1 功能

- 收藏；
- 笔记；
- 将某一课加入今日计划；
- 看完课程后进入 AI Speaking Practice；
- 将课程中“用户手工标记的学习点”进入复习系统。

## 13.5 权限策略

P0 建议：

- 无权限用户不显示该 Collection；
- 如用户无任何 Private Collection，则隐藏整个“随心学”一级入口；
- 管理员可对 userId + collectionId 建立 grant；
- 支持 revoke；
- 可选支持 expiresAt，为未来临时授权留接口；
- 需要审计 grant / revoke 记录。

## 13.6 第三方内容边界

V2.0 产品架构支持 Private Collection，但不默认判断任何第三方内容一定允许复制、托管或向其他用户分发。

每个 Private Collection 建议保留：

- source；
- ownershipType；
- licenseNote；
- allowedAudience；
- sourceUrl；
- adminNote。

产品目标是保证系统“能够正确限制访问”，而不是替代第三方内容授权判断。

---

# 14. 内容生产体系

## 14.1 为什么采用离线生产

实时生成课程视频会带来：

- 高成本；
- 长延迟；
- 质量不可控；
- 内容重复生产；
- 用户等待体验差。

因此 V2.0 默认采用：

> **离线批量生成资源 + 运行时个性化教学。**

## 14.2 Scenario Lesson Content Pipeline

建议流程：

```text
课程蓝图
→ LLM 生成 Script / Questions / Expressions
→ 自动规则检查
→ AI 质量复核
→ 人工抽检
→ TTS 生成
→ Image 生成
→ Asset 校验
→ 打包 ScenarioLesson
→ 发布
→ CDN/Object Storage
```

## 14.3 内容版本化

每个资源必须支持版本号。

例如：

`workplace.daily_update.b1@1.2.0`

当 Transcript 或 Audio 修改时：

- 不覆盖历史学习证据语义；
- 新用户使用最新发布版本；
- 已学习用户的历史记录保留对应 resourceVersion。

## 14.4 质量检查

至少检查：

- CEFR 难度是否符合目标；
- 场景是否真实；
- 表达是否自然；
- Transcript 与 Audio 是否一致；
- 问题是否可由课程内容回答；
- 答案是否唯一或允许合理开放答案；
- Key Expressions 是否值得迁移；
- 无敏感或不适合内容；
- 不包含不必要的个人隐私；
- Image 与场景一致；
- 语音可懂度与音量正常。

## 14.5 内容生成工具形态

P0 不要求做完整内容管理后台。

允许采用：

- JSON/YAML 课程定义；
- 批处理脚本；
- 管理 API；
- 简单审核页面。

优先保证内容生产流程稳定，而不是先开发复杂 CMS。

---

# 15. 运行时成本与性能原则

## 15.1 零 Token 首屏

用户打开一节标准 Scenario Lesson，在进入 AI 对话/评价之前，原则上无需调用 LLM。

资源来自：

- DB metadata；
- Object Storage / CDN；
- 预生成 JSON；
- 预生成 Audio / Image。

## 15.2 需要实时模型的节点

主要集中在：

- 用户自由回答分析；
- Role Play；
- Correction；
- Natural Expression；
- Evaluation；
- Memory update；
- Personalized Planner。

## 15.3 性能目标

产品级目标：

- 课程元数据首屏：正常网络下尽快可见；
- Audio：点击后应快速开始播放，不等待模型生成；
- 图片：可缓存；
- AI Speaking Feedback：继续沿用流式反馈思想；
- 某项媒体资源失败时，不得导致整节课不可学习。

具体毫秒指标在技术设计阶段确定。

---

# 16. 用户核心流程

## 16.1 今日学习场景课

```text
打开首页
→ Agent 推荐一节 B1 Workplace Lesson
→ 用户进入场景
→ First Listen
→ 回答理解题
→ 查看 Transcript / Key Expressions
→ Guided Speaking
→ Role Play
→ AI Correction
→ Try Again
→ 完成
→ Evidence / Memory / Review
→ Planner 更新
```

## 16.2 自主进入随心学

```text
用户拥有 Private Collection 权限
→ 显示随心学
→ 进入 English Fluency Course
→ 选择章节
→ 视频学习
→ 记录进度
→ 可选进入 AI Practice（P1）
```

## 16.3 无权限用户

```text
用户没有任何 Private Collection
→ 不显示随心学入口
→ 今日学习只使用 PUBLIC / 其他有权资源
→ 用户不会收到 EngFluent 推荐
```

## 16.4 权限撤销

```text
管理员 revoke
→ 用户刷新/下一次访问
→ Collection 不再可访问
→ 历史进度保留
→ 今日计划若存在未开始的私有资源则自动替换/回退
```

---

# 17. 学习状态与进度

## 17.1 Scenario Lesson 状态

建议至少支持：

- NOT_STARTED；
- IN_PROGRESS；
- COMPLETED；
- NEEDS_REVIEW。

## 17.2 课程完成条件

不能以“播放完 Audio”作为完成。

P0 建议至少满足：

- 完成主要输入部分；
- 完成核心理解检查；
- 完成至少一个 Speaking Task。

Role Play 是否强制完成，可根据课程类型配置。

## 17.3 Mastery 与 Completion 分离

用户完成课程不代表掌握。

系统应分别记录：

- completion；
- evidence；
- skill state；
- retry improvement；
- review due state。

---

# 18. 管理能力

## 18.1 P0 管理能力

### Resource 管理

- 新建/导入资源；
- Publish / Unpublish；
- 查看 metadata；
- 更新资源版本；
- 删除或 Disabled。

### Collection 管理

- 新建 Collection；
- 绑定资源；
- 设置 Access Scope；
- 排序。

### Entitlement 管理

- Grant user；
- Revoke user；
- 查询用户权限；
- 查询 Collection 已授权用户；
- Audit log。

## 18.2 管理 UI 优先级

- P0：API / Script 可接受；
- P1：简单 Web 管理页；
- P2：完整 CMS。

---

# 19. 产品指标

## 19.1 学习指标

- Scenario Lesson start rate；
- Scenario Lesson completion rate；
- First Listen comprehension accuracy；
- Speaking Task completion rate；
- Role Play completion rate；
- Try Again rate；
- Retry improvement rate；
- New expression reused rate；
- 7-day review accuracy；
- 难度升级成功率。

## 19.2 推荐指标

- 推荐课程进入率；
- 推荐课程完成率；
- 被跳过率；
- 同主题重复疲劳率；
- 推荐后手工换课率；
- 因权限问题导致的打开失败次数，目标应接近 0。

## 19.3 随心学指标

- 授权用户数；
- Private Collection 使用率；
- 课程完成率；
- 最近学习恢复成功率；
- 从 Private Course 进入 AI Practice 的比例（P1）。

## 19.4 成本指标

- 每完成一节标准课程的平均实时 LLM 成本；
- 每次 Speaking Feedback 成本；
- 每次 Role Play 成本；
- 每用户每日平均 AI 成本；
- 离线资源生产一次性成本。

重点关注：

> 随着用户数增长，教材成本应主要表现为固定/摊薄成本，而不是每个用户重复生成。

## 19.5 私教专业性与沉浸体验指标

私教专业性：

- 推荐理由可解释率；
- 到期复习命中率；
- 难度适配率；
- Retry 后关键问题改善率；
- 跨场景迁移成功率；
- 不同 Skill State 用户计划差异率；
- Evidence 对下一计划产生影响的比例。

沉浸体验：

- Episode 进入后主动输出率；
- Lin Muen Mission 完成率；
- 连续学习天数；
- 故事体验后进入 Role Play 的转化率；
- 用户对陪伴强度和情景相关性的反馈。

主成功指标必须同时包含能力改善和持续参与，不能只使用播放、点击或对话轮数。

---

# 20. 非功能需求

## 20.1 权限与安全

- 私有资源权限必须由后端校验；
- 私有资源文件不应仅靠“猜不到 URL”实现保护；
- Grant / Revoke 有审计记录；
- 用户学习记录和资源权限解耦；
- 权限撤销后不能通过旧页面继续获取新资源。

## 20.2 可维护性

- Resource Type 可扩展；
- Provider 可扩展；
- Access Scope 可扩展；
- CEFR / Topic / Scene 使用稳定枚举或配置；
- 内容与 Agent Prompt 解耦；
- 内容版本化。

## 20.3 可观测性

关键链路需要 trace：

- 推荐为何选择某课程；
- Access Filter 排除了什么类型资源；
- 课程加载失败原因；
- 音频播放失败；
- AI Practice 调用结果；
- 内容版本。

## 20.4 降级

- 图片失败：仍可文本 + Audio 学习；
- Audio 失败：允许 Transcript + Speaking 继续；
- LLM 反馈失败：保存用户回答，提示稍后重新尝试该反馈，不丢学习进度；
- 私有课程不可用：不影响公共学习闭环。

---

# 21. P0 / P1 / P2 功能清单

## 21.1 P0

- Learner Model；
- Skill Graph + Experience Graph；
- Daily Learning Prescription；
- 可测试的教学策略基线与推荐原因；
- Learning Resource 统一模型；
- Scenario Lesson；
- 8 主题 × 3 场景 × A2/B1/B2 = 72 Skill Unit Variants；
- Lin Muen Season 1（EP001–EP010）沉浸体验；
- Skill Unit 与 Episode Mapping；
- Episode 资源中的 Evidence Criteria 和适配条件；
- Image + Audio + Transcript；
- Key Vocabulary / Expressions；
- Comprehension Questions；
- Speaking Task；
- Role Play 基础能力；
- Correction + Try Again 复用；
- Evidence / Memory / Review 接入；
- 今日计划推荐公共 Scenario Lesson；
- Resource Access Filter；
- Private Collection；
- Admin Grant / Revoke；
- 随心学入口；
- EngFluent Private Collection 基础目录与进度；
- 内容离线生成 Pipeline 最小版本；
- 内容版本管理。

## 21.2 P1

- Singapore English Workplace / Tech 音频变体；
- 收藏；
- 最近学习；
- 私有课程加入每日计划开关；
- Private Course → AI Practice；
- Question Builder；
- Process Explanation；
- Transcript 句级联动；
- 简单内容管理 Web UI；
- 更丰富推荐去重策略。

## 21.3 P2

- Imitation；
- Pronunciation / Stress / Rhythm；
- British English；
- 多人口音会议；
- 动态轻动画；
- 即时生成一次性 Audio Lesson；
- 生成式视频实验。

---

# 22. 开发与交付建议

## 22.1 里程碑 M1：个性化私教与资源底座

目标：系统能够基于用户状态选择训练目标，并保存、查询、授权、播放标准资源。

包含：

- Learner Model；
- Skill Graph 与 Skill Unit；
- 确定性教学策略；
- Daily Learning Prescription 与推荐解释；
- Resource / Collection / Asset / Entitlement；
- Access Filter；
- Object Storage；
- 基础资源 API；
- 第一批样例资源。

## 22.2 里程碑 M2：Scenario Lesson Player

目标：完成“个性化目标 → Lin Muen 情景 → 听 → 理解 → 表达”的资源学习流程。

建议先生产：

`8 主题 × 3 场景 × B1 = 24 Lessons`

用于验证模板和 UI。

## 22.3 里程碑 M3：AI Speaking 闭环

目标：Scenario Lesson 正式接入：

- Speaking Task；
- Role Play；
- Correction；
- Try Again；
- Evidence；
- Memory / Review。

## 22.4 里程碑 M4：完整 72 Skill Unit Variants

补齐 A2 / B2，完成内容质量校验。

## 22.5 里程碑 M5：随心学与 Private Collection

- EngFluent Collection；
- Grant / Revoke；
- 视频目录和进度；
- 无权限隐藏；
- 异常与权限回退。

## 22.6 里程碑 M6：推荐、指标与发布

- Planner 接入；
- 推荐去重；
- 埋点；
- 成本统计；
- E2E；
- 发布验收。

---

# 23. V2.0 验收标准

V2.0 发布至少满足以下条件。

## 23.1 公共学习资源

- 用户能够完成至少一节完整 Scenario Lesson；
- Audio 不依赖运行时生成；
- Transcript、Expressions、Questions 与 Audio 一致；
- 至少 72 个目标 Skill Unit Variant 处于发布状态；
- A2/B1/B2 难度差异经过内容检查。

## 23.2 主动训练

- 每节目标课程至少包含一个 Speaking Task；
- 用户回答能够进入 Correction；
- 关键反馈可以 Try Again；
- 完成后生成 Learning Evidence；
- Evidence 可影响 Memory / Review 或 Planner。

## 23.3 推荐

- 今日计划可根据目标、Skill Gap、Evidence、Review Urgency 和可用时间生成教学处方；
- 两个不同 Skill State 的同等级用户可获得不同训练；
- 每个推荐包含可验证的简短原因；
- 已掌握 Skill 不会因 Season 剧情顺序被重复作为基础课推荐；
- Episode 选择发生在 Skill Unit 和教学策略确定之后；
- 无权限资源不会进入用户候选池；
- 私有资源不可用时存在公共资源 fallback。

## 23.4 随心学

- 管理员可以给指定用户 grant；
- 授权用户能够看到并学习 Private Collection；
- 未授权用户不能通过前端或 API 获得私有资源；
- revoke 后权限立即生效；
- 学习进度可恢复。

## 23.5 性能与成本

- 打开公共 Scenario Lesson 不需要等待实时 LLM 生成教材；
- 图片、Audio 可缓存；
- 资源生产成本和用户交互 AI 成本可以分别统计；
- 生成式视频不属于发布依赖。

---

# 24. 核心风险与应对

## 24.1 内容质量不稳定

风险：批量 LLM 生成教材出现表达不自然、等级不准、答案错误。

应对：

- Schema；
- 自动规则；
- AI reviewer；
- 人工抽检；
- 版本化与下架。

## 24.2 内容很多但用户不开口

风险：产品重新退化为听力/看课工具。

应对：

- Speaking Task 作为核心完成条件；
- 今日计划优先有输出的课程；
- 用 speaking completion 而非 audio completion 衡量价值。

## 24.3 私有课程权限泄漏

风险：前端隐藏但资源 URL 可直接访问。

应对：

- 后端 entitlement；
- 私有对象保护；
- 授权 URL/代理访问方案由技术设计确定；
- 审计。

## 24.4 推荐到不可用资源

风险：权限、下架、资源损坏导致用户点击失败。

应对：

- 推荐前 Access Filter；
- 发布状态检查；
- fallback；
- 计划执行前二次校验。

## 24.5 资源过度重复

风险：72 个能力变体在体验层重复使用相同模板，仍会快速产生“套路感”。

应对：

- 同一 Scene 支持多个 Training Type；
- 逐步增加 variation；
- P1 增加参数化练习；
- 不急于使用生成式视频。

## 24.6 TTS 缺乏真实感

风险：听力材料机械，影响学习体验。

应对：

- 选择稳定高质量 TTS；
- 多角色音色；
- 人工抽检语气和停顿；
- Workplace / Tech 增加真实口音变体。

## 24.7 第三方版权与授权

风险：系统具备私有权限并不代表第三方内容自动获得再分发权。

应对：

- Private Collection 明确 source/license metadata；
- 控制 audience；
- 不把第三方内容混入公共资源；
- 当使用范围扩大时单独确认授权边界。

## 24.8 情景 IP 掩盖私教价值

风险：产品因 Lin Muen 故事体验获得短期互动提升，但所有用户仍学习同样内容，最终退化为角色化课程 App。

应对：

- 教学决策先于 Episode 匹配；
- Skill Graph 与 Experience Graph 分离；
- Season 不作为强制教学顺序；
- 验收不同能力状态是否产生不同计划；
- 以 Evidence、Retry 改善和迁移表现衡量私教价值；
- 以持续学习和主动输出衡量沉浸价值。

---

# 25. 成功标准

V2.0 成功不以“上线 72 个 Skill Unit Variant”作为唯一标准，而以以下产品信号判断：

1. 不同目标、短板和掌握状态的用户得到不同且可解释的每日处方；
2. 用户愿意在 Lin Muen 情景中完成“听 → 说 → Retry”，而不是只播放音频；
3. 推荐训练相较随机或固定顺序具有更高完成率和 Evidence 改善；
4. 用户在不同场景中的输出质量和迁移能力有改善；
5. 今日学习同时降低选课负担并增强继续学习动力；
6. 标准教材运行时成本显著低于实时生成完整课程；
7. 私有随心学资源不会污染公共推荐链路；
8. Skill / Experience / Resource 架构可扩展到 300、1000+ 能力与内容变体而不需要推翻重做。

---

# 26. 参考产品与设计借鉴边界

V2.0 可参考以下成熟学习产品的公开教学思路，但不复制其受保护内容：

## ESLPod / ESL Podcast

重点借鉴：

- 可理解输入；
- Audio + Transcript；
- 词汇和表达解释；
- 真实生活主题；
- 通过重复理解建立输入基础。

## Hibay

重点借鉴：

- 场景化学习；
- 图片增强情境；
- 同一场景多难度；
- 从内容输入进入口语任务；
- pronunciation / grammar / vocabulary feedback 的产品方向。

## EngFluent

重点借鉴教学方法：

- Q&A；
- Story Retelling；
- Idea Expression；
- Process Explanation；
- Long Speaking；
- Imitation。

EngFluent 实际课程资源在 V2.0 中只作为 Private Collection 处理，不作为 INTERNAL 公共资源来源。

---

# 27. 推荐文档目录结构

当前仓库已经采用“文档类型优先”的组织方式。V2.0 是大版本，但不建议切换为 `docs/v2.0/...`，否则会和现有 `docs/prd`、`docs/design`、`docs/plans` 等结构冲突。

推荐从 V2.0 开始采用：

```text
docs/
├── prd/
│   ├── ENGLISH_TUTOR_AGENT_PRD_v1.0.0.md      # 历史文件保持原位
│   └── v2.0/
│       └── ENGLISH_TUTOR_AGENT_PRD_v2.0.0.md
│
├── design/
│   └── v2.0/
│       ├── ARCHITECTURE.md
│       ├── RESOURCE_MODEL.md
│       └── CONTENT_PIPELINE.md
│
├── plans/
│   └── v2.0/
│       ├── IMPLEMENTATION_PLAN.md
│       └── TASK_BACKLOG.md
│
├── ui/
│   └── v2.0/
│       ├── INFORMATION_ARCHITECTURE.md
│       └── SCENARIO_LESSON_FLOW.md
│
├── test/
│   └── v2.0/
│       └── ACCEPTANCE_SCENARIOS.md
│
└── release/
    └── v2.0/
        └── RELEASE_CHECKLIST.md
```

### 目录原则

1. 顶层按文档职责分类，不按版本分类；
2. 大版本在各职责目录下建立 `v2.0/`；
3. 小版本优先通过文件版本号和 Git 历史管理，不为每个 `2.0.1` 单独建目录；
4. V2.0 后续设计文档全部以本 PRD 为产品基线；
5. 不主动移动历史 V1.x 文档，除非进行专门的文档治理任务。

---

# 28. 后续文档建议

PRD 冻结后，建议按以下顺序继续：

1. `docs/design/v2.0/ARCHITECTURE.md`  
   定义 Resource、Collection、Asset、Entitlement 与现有 Planner/Memory/Training 的关系。

2. `docs/design/v2.0/RESOURCE_MODEL.md`  
   定义数据模型、状态机、权限模型、资源版本。

3. `docs/ui/v2.0/SCENARIO_LESSON_FLOW.md`  
   冻结听力、Transcript、Speaking、Role Play、Retry 的 UI 流程。

4. `docs/design/v2.0/CONTENT_PIPELINE.md`  
   定义 72 个 Skill Unit Variant 与 Season 1 Episode Mapping 的批量生产、Prompt、TTS、图片生成、QA 和发布流程。

5. `docs/plans/v2.0/IMPLEMENTATION_PLAN.md`  
   将 V2.0 拆成可开发里程碑。

6. `docs/plans/v2.0/TASK_BACKLOG.md`  
   转化为 Codex/Cursor 可逐项实现的任务。

7. `docs/test/v2.0/ACCEPTANCE_SCENARIOS.md`  
   形成产品验收和 E2E 测试基线。

---

# 29. 产品基线结论

V2.0 最终确定以下方向：

> **V2.0 的产品决策核心是 Personalized Tutor Core：它依据用户目标、能力状态、短板、遗忘风险和近期诉求，使用可测试的教学策略生成每日课程处方。**

> **V2.0 的体验核心是 Lin Muen Immersive Experience：它把处方选择的 Skill Unit 转化为连续故事、真实情景和温暖陪伴，但不替代教学决策。**

> **主学习流继续以“今日学习”为核心；新增 INTERNAL 标准 Scenario Lesson 资源库，通过预生成图片、真人感 TTS、Transcript 和训练任务保证速度与成本；实时 AI 只用于推荐、对话、反馈、纠错和学习状态更新。**

> **EngFluent 等用户已获得访问条件的第三方课程，不进入公共资源体系，而作为 ADMIN_GRANTED Private Collection 放入独立“随心学”模块；默认不参与今日推荐，从产品结构上隔离权限和版权风险。**

> **V2.0 的教学重点不是让用户消费更多内容，而是让每份输入资源最终转化为可被评估、纠错、Retry、记忆和复习的主动英语输出。**
