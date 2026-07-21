# Acceptance Scenarios

## SCN-001 首次目标设置

```gherkin
Given 用户首次打开 App
When 用户选择“职场英语”为唯一主要目标
And 设置每日 20 分钟
Then 系统保存唯一主要目标
And 再次进入时恢复该设置
And 用户不需要选择具体课程
```

## SCN-002 中等自评用户完成轻量初评

```gherkin
Given 用户自评为中等水平
When 系统根据自评分配听说读写题目
Then 评估过程目标时长为 8–10 分钟
And 客观题占主要比例
And 至少采集四项能力的最小证据
And 交互不呈现为高压力正式考试
```

## SCN-003 高自评用户增加开放表达

```gherkin
Given 用户自评口语和综合能力较高
When 系统编排初评
Then 开放对话、复述或长回答比例高于普通用户
And 仍保留少量客观校准题
```

## SCN-004 初始画像不是永久结论

```gherkin
Given 用户已经生成初始画像
When 后续训练证据与初评结果持续不一致
Then 系统调整能力状态及置信度
And 后续计划根据更新后的画像改变
```

## SCN-005 Agent 自动生成不同计划

```gherkin
Given 用户 A 的主要弱点是听力
And 用户 B 的主要弱点是口语自然度
When 两人请求今日计划
Then 两份计划的任务比例和训练形式明显不同
And 系统说明安排原因
```

## SCN-006 当天临时调整

```gherkin
Given 用户今日计划为 20 分钟
When 用户说明今天只有 5 分钟
Then 系统压缩今日计划
And 不永久覆盖长期目标和能力判断
```

## SCN-007 严重错误即时提醒

```gherkin
Given 用户表达中的错误严重影响理解
When 系统完成本轮分析
Then 系统在当前轮进行简短提醒
And 对话仍能继续
```

## SCN-008 轻微不自然不打断

```gherkin
Given 用户句子可以理解但表达轻微不自然
When 系统正常对话
Then 系统不打断当前表达
And 在训练总结中提供自然表达建议
```

## SCN-009 高频错误进入复习

```gherkin
Given 用户连续多次遗漏可数名词冠词
When 本次训练结束
Then 系统聚合为高频薄弱点
And 在合适日期安排主动输出型复习
And 计划解释中说明该安排来源
```

## SCN-010 掌握需要多次证据

```gherkin
Given 用户在一次选择题中答对知识点
Then 系统不直接标记为已掌握
When 用户随后能独立使用、迁移到新场景并通过延迟复习
Then 系统提高掌握状态
```

## SCN-011 半双工语音训练

```gherkin
Given 用户进入语音任务
When 用户按住说话并松开
Then App 显示录音、上传、识别、生成和播放状态
And ASR 置信度低时允许确认文本或重录
```

## SCN-012 AI 输出非法

```gherkin
Given AI Provider 返回非 JSON 或缺失字段
When 后端解析结果
Then 系统校验失败并执行有限重试
And 仍失败时使用安全降级
And 已完成的训练进度不丢失
```

## SCN-013 IELTS 完整模拟

```gherkin
Given 用户选择 IELTS Speaking 完整模拟
When 用户依次完成 Part 1、Part 2 和 Part 3
Then 系统根据四个官方风格维度给出练习参考评价
And 标明不是官方成绩
And 提供证据、置信度和下一步建议
```

## SCN-014 隐私设置

```gherkin
Given 用户关闭原始录音保存
When 用户完成语音训练
Then 系统可以完成必要处理
And 不长期保留原始录音
And 学习摘要仍可按用户允许的范围保存
```

## SCN-015 数据删除

```gherkin
Given 用户发起学习数据删除
When 删除流程完成
Then 相关数据库记录和对象存储资源被处理
And 请求可重复调用而不会产生不一致
And 系统保留必要的非敏感审计状态
```
