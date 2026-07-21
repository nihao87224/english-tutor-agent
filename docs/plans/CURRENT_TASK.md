# Current Task

> 开始新任务时替换本文件内容。一个时间只允许存在一个当前开发任务。

## Task

`M0-T01 建立仓库与目录结构`

## Status

`TODO`

## Goal

确认并提交编码启动目录、规则、文档、契约和本地基础设施模板，使 Cursor 能够进入 M0-T02/M0-T03 的工程初始化。

启动包 v1.0.1 已修复任务编号、ADR、阶段门禁、技术版本和 Java 包名冲突；Git 初始化与基线提交仍需在实际项目目录中执行。

## In scope

- 检查本启动包文件完整性；
- 修复文档链接和目录问题；
- 初始化 Git；
- 创建第一笔基线提交；
- 记录项目基线版本。

## Out of scope

- 创建 Spring Boot 业务代码；
- 创建 Android 业务页面；
- 调用真实 LLM、ASR 或 TTS；
- 创建正式数据库业务表。

## Acceptance criteria

1. `python scripts/validate_project.py` 通过；
2. ZIP 解压后不存在缺失的基线文件；
3. Git 中不包含密钥或生成目录；
4. README 能说明下一步；
5. 完成独立 Review。

## Expected commands

```bash
cp .env.example .env
python scripts/validate_project.py
git init
git add .
git commit -m "chore: establish vibe coding project baseline"
```
