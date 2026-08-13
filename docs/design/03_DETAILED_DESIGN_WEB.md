# English Tutor Agent Web 详细设计

> 客户端：Web  
> V1.0 目标：AI English Expression Coach  
> 架构：组件化 UI / API Client / Client State，Web-first，文字表达优先

---

# 1. 目标

Web V1.0 用最小成本验证核心产品假设：

```text
用户表达
→ AI 理解意图
→ 流式回应
→ 错误定位
→ 自然表达优化
→ 结构解释
→ Try Again 再输出
→ 学习证据和下一计划变化
```

首版 Web 不承担完整移动端、语音、听力、IELTS 或长期复习系统。

# 2. 工程建议

```text
web/
├── package.json
├── src/
│   ├── app/
│   ├── shared/api/
│   ├── shared/session/
│   ├── features/onboarding/
│   ├── features/coach/
│   ├── features/plan/
│   ├── features/summary/
│   └── features/settings/
└── tests/e2e/
```

推荐技术：

- TypeScript；
- React + Vite；
- Fetch + EventSource/fetch-stream 处理 REST/SSE；
- TanStack Query 或同等工具管理服务端状态；
- Vitest + Testing Library；
- Playwright 验证主路径。

# 3. 页面与状态

## 3.1 最短 Onboarding

目标是尽快进入表达教练：

- 选择目标：Workplace / General / IELTS；
- 设置每日分钟数和纠错强度；
- 可选自评，默认允许走已有后端初评路径；
- 本地保存开发期 `X-User-Key`。

## 3.2 今日表达教练首页

展示：

- 今日任务标题；
- 为什么今天练这个；
- 推荐表达目标；
- 开始按钮；
- 最近一次总结和下一计划变化。

## 3.3 Coach Workspace

主界面采用三栏或两栏响应式布局：

- 左侧：今日计划、练习目标、历史弱点入口；
- 中间：对话流、用户输入、流式回复；
- 右侧：纠错面板、自然表达、Try Again。

移动窄屏时，纠错面板改为底部抽屉或标签页。

# 4. SSE 交互

Web 需要处理以下事件：

- `status`：显示理解中、生成中、纠错中；
- `text_delta`：追加 assistant 回复；
- `correction_ready`：打开或刷新纠错面板；
- `done`：结束流式状态；
- 网络异常：保留输入和已收到片段，允许重试。

客户端不得假定所有事件都会成功到达。`correction_ready` 缺失时仍展示对话回复，
并提示“本轮纠错稍后重试”。

# 5. 纠错面板

V1.0 面板固定包含：

- Correction：更正后的句子；
- Why：语法或用法解释；
- Natural：更自然表达；
- Pattern：可复用表达结构；
- Try Again：一个短改写/复述任务。

面板不展示冗长诊断，不一次指出过多问题。默认突出 1-3 个高价值问题。

# 6. Try Again

Try Again 是 V1.0 的关键闭环：

1. 用户根据建议重新表达；
2. 系统再次流式回应；
3. 如果表达改进，保存为独立使用或迁移 evidence；
4. 日总结展示本轮掌握的表达模式。

首版可复用现有 training attempt 接口，不新增复习调度。

# 7. 隐私

- 默认使用开发期 `X-User-Key`，不在浏览器保存真实密钥；
- 原文保存开关必须可见；
- 关闭原文保存时，Web 只提交必要文本供本轮处理，后端按现有隐私规则保留摘要和 evidence；
- 日志不得记录完整用户原文。

# 8. 测试

Web M3 至少覆盖：

- loading / content / empty / error 状态；
- SSE 正常流和中断；
- `correction_ready` 渲染；
- Try Again 二次提交；
- 隐私开关；
- 完成训练后 summary 和下一计划变化。

Playwright 主路径：

```text
打开 Web
→ 设置用户键
→ 进入今日表达教练
→ 输入 I very like this movie
→ 看到流式回复
→ 看到 really like / really enjoyed 的纠错和自然表达
→ Try Again
→ 完成并查看总结
```
