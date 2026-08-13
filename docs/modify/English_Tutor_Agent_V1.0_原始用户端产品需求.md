# English Tutor Agent V1.0 产品需求调整说明

## 1. 背景与核心发现

当前英语学习过程中发现：

用户的英语输入能力（Input）明显领先于输出能力（Output）。

具体表现：

- 听 ESL Podcast 等英语材料时，可以理解大部分全英文解释；
- 能理解单词、句子含义；
- 能跟随英语母语者的表达逻辑。

但是自己开口表达时存在明显困难：

- 无法快速组织英文句子；
- 语法错误频繁；
- 不确定自己的表达是否自然、正确；
- 缺少持续、即时、个性化反馈。

因此，English Tutor Agent 的定位需要从“AI 英语聊天伙伴”升级为：

> 一个帮助用户将被动英语能力转化为主动英语表达能力的 AI 英语教练。

---

## 2. 核心用户痛点

### 痛点1：理解能力强，但无法主动表达

用户可以理解：

> I was supposed to meet a friend, but something came up.

但是自己表达类似意思时，很难主动调用：

- be supposed to do something
- something came up

原因：

用户拥有大量 passive vocabulary 和 passive grammar，但是缺少：

- active vocabulary
- sentence patterns
- language chunks

产品目标：

帮助用户完成：

理解 → 模仿 → 改写 → 主动使用

---

## 3. 痛点2：不会组织英文句子

用户当前容易采用：

中文想法 → 逐字翻译 → 查找单词 → 拼接句子

导致：

- 单词正确；
- 语法可能正确；
- 但是表达不像自然英语。

例如：

错误表达：

> Recently my work pressure is very big.

更自然：

> I've been under a lot of pressure at work recently.

产品需求：

AI 需要理解用户表达意图，而不是简单翻译。

流程：

中文/英文表达
↓
识别表达意图
↓
匹配英语表达模板
↓
生成自然表达
↓
解释表达结构

---

## 4. 痛点3：语法错误无法及时发现

当前问题：

- 用户输出后不知道是否正确；
- 错误长期重复；
- 缺少即时反馈。

AI 应提供：

### 错误定位

例如：

用户：

> I very like this movie.

AI：

问题：

very 不能直接修饰动词 like。

### 规则解释

very:

- very interesting
- very quickly

really:

- really like
- really enjoy

### 自然表达优化

推荐：

> I really enjoyed this movie.

---

## 5. 痛点4：缺少学习闭环

传统学习：

输入 → 理解

缺少：

输出 → 纠错 → 修改 → 再输出 → 形成能力

English Tutor Agent 核心闭环：

用户表达
↓
AI 理解意图
↓
错误检测
↓
表达优化
↓
语法解释
↓
生成练习
↓
用户再次表达
↓
长期记忆用户弱点

---

# 6. 产品定位调整

## 原定位

AI 英语聊天伙伴：

Chat with AI in English

## 新定位

AI English Speaking Coach

帮助用户：

- Think in English
- Speak in English
- Improve through feedback

核心价值：

不是陪用户聊天，而是帮助用户建立真实英语表达能力。

---

# 7. 功能规划调整

## V1.0 智能表达纠错

### English Conversation

用户自由聊天。

AI：

- 保持自然交流；
- 不频繁打断；
- 收集用户语言数据。

### Grammar Feedback

示例：

用户：

> I go to work yesterday.

AI：

Correction:

> I went to work yesterday.

Explanation:

Yesterday indicates past time, so use past tense.

### Expression Improvement

用户：

> My work is very busy.

AI：

Natural:

> I've been really busy with work lately.

Explanation:

英语中通常说：

busy with work

而不是：

work is busy

---

# 8. V1.5 主动训练系统

## Weakness Memory

记录用户长期问题：

- 时态错误；
- 冠词错误；
- 介词错误；
- 中文直译；
- 不自然表达。

## Personalized Practice

根据历史错误生成训练。

例如：

用户经常：

> I very like...

生成：

> Tell me something you really like.

强化：

really + verb

---

# 9. 推荐学习模式

针对当前用户水平：

学习重点：

Input 40%

Output 60%

每日：

10分钟 ESL Podcast 输入

+

10分钟 AI 对话输出

+

10分钟 AI 纠错和复述

重点：

从理解英语转向主动生成英语。

---

# 10. 产品竞争力假设

目前大量 AI 英语产品主要解决：

“有人陪你聊天”

但用户真正缺少：

“为什么我的表达不自然？如何变得更像母语者？”

English Tutor Agent 的差异化：

AI Language Coach

=

Conversation Partner

+

Grammar Checker

+

Expression Optimizer

+

Personal Learning Memory

+

Adaptive Practice System

---

# 11. 后续 PRD 调整方向

下一版本重点：

1. 从 Chat Bot 转向 Speaking Coach
2. 增加表达纠错流程
3. 增加用户错误长期记忆
4. 增加个性化训练生成
5. 增加中文意图到英文表达能力
6. 增加自然表达评分体系

核心架构围绕：

用户表达数据

↓

AI 分析

↓

错误记忆

↓

个性化训练

形成完整学习闭环。
