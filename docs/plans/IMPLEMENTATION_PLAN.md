# Implementation Plan

> 任务编号权威来源：`docs/plans/TASK_BACKLOG.md`。`docs/design/09_VIBE_CODING_EXECUTION_GUIDE.md` 只提供流程和任务卡示例。

> 原则：按可运行的纵向业务闭环交付，不按 Controller、Service、Repository 横向堆叠代码。

## M0：工程初始化

### 目标

后端、Android 和本地基础设施可以被新开发者可靠启动，CI 能执行基础质量检查。

### 任务

- M0-T01 建立仓库与目录结构
- M0-T02 初始化后端多模块工程
- M0-T03 初始化 Android Compose 工程
- M0-T04 建立 MySQL、Redis、MinIO 本地环境
- M0-T05 建立 Flyway、测试容器与健康检查
- M0-T06 建立基础 CI、格式检查和契约校验
- M0-T07 建立 Fake LLM/ASR/TTS Provider
- M0-T08 完成开发环境文档和冷启动验证

### 里程碑验收

- 后端构建与测试通过；
- Android Debug 构建通过；
- `docker compose up -d` 启动依赖；
- 健康检查返回可用；
- OpenAPI 与 JSON Schema 校验通过；
- 无真实密钥依赖；
- 新开发环境可按文档独立启动。

---

## M1：首次使用与初始评估

### 业务闭环

```text
选择唯一主要目标
→ 设置时间、纠错和隐私偏好
→ 用户自评
→ 完成 8–10 分钟轻量初评
→ 生成初始能力画像
→ 生成首个今日计划
```

### 任务

- M1-T01 用户与主要目标
- M1-T02 学习偏好和隐私偏好
- M1-T03 Onboarding 进度恢复
- M1-T04 听说读写自评
- M1-T05 初评会话与自适应题目
- M1-T06 客观题评分
- M1-T07 开放表达 Fake Evaluator
- M1-T08 初始能力画像与置信度
- M1-T09 首个规则型今日计划
- M1-T10 Android 首次使用端到端验收

### 关键策略

先用 Fake AI 和固定题库跑通流程，再接真实模型。业务状态和恢复能力优先于生成效果。

---

## M2：文字学习闭环

```text
今日计划
→ 开始训练
→ 文字对话/任务
→ 流式回复
→ 分层纠错
→ 保存学习证据
→ 更新画像
→ 当日总结
→ 下一计划变化
```

任务：

- 通用 Training Session
- 任务容器与尝试记录
- SSE 文字对话
- Correction Analyzer
- Evidence 聚合
- 日总结
- 第二天计划差异 E2E

---

## M3：Web 表达教练 MVP

```text
打开 Web
→ 最短 onboarding / 本地用户键
→ 进入今日表达教练
→ 输入英文或中英混合表达
→ SSE 流式回复
→ 展示语法纠错、自然表达和结构解释
→ Try Again 改写/复述
→ 保存学习证据
→ 日总结和下一计划变化
```

任务：

- Web 工程初始化与 API Client
- Web 最短 Onboarding 与本地用户键
- Web 今日表达教练首页
- Web SSE 对话与流式回复
- Web 分层纠错与自然表达面板
- Try Again 改写/复述练习闭环
- Web 每日总结与下一计划变化展示
- Web Expression Coach E2E

---

## M4：语音与听力

```text
播放听力
→ 用户按住说话
→ 上传音频
→ ASR
→ 低置信度确认
→ 对话/纠错
→ TTS
→ 播放回复
```

任务：

- Android 录音状态机
- 分片或单文件上传
- ASR Provider
- Audio Asset 与播放器
- TTS Provider
- 弱网重试
- 半双工语音会话 E2E

---

## M5：长期复习与动态掌握

任务：

- Knowledge State
- 高频错误聚合
- Review Scheduler
- 识别→提示→独立使用→迁移→延迟验证
- 周报
- 默认四周阶段测验，可动态提前或延后

---

## M6：IELTS Speaking

任务：

- Part 1
- Part 2 准备计时与长回答
- Part 3
- 完整模拟状态机
- 四维 Rubric
- 参考 Band 与置信度
- 评分一致性评测

---

## M7：发布准备

任务：

- 隐私控制与数据删除
- 监控、告警和成本指标
- 性能与弱网验证
- Web 部署与 Android 签名构建
- 灰度发布
- 回滚与应急手册
- 用户验收与正式发布

## 开发节奏

每个任务建议控制在 0.5–2 个工作日内。超过 2 天的任务必须继续拆分。
