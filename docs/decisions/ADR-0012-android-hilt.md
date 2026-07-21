# ADR-0012：Android 使用 Hilt 依赖注入

状态：Accepted

## Context

Android 详细设计需要统一的依赖注入方案来装配 ViewModel、Repository、网络、数据库和音频运行时。设计阶段曾保留 Hilt 与 Koin 二选一空间。

## Decision

Android 客户端统一使用 **Hilt**：

- Application 级依赖图；
- ViewModel 注入；
- Repository 与基础设施绑定；
- 测试中使用 Hilt 测试替换或手动 Fake。

首版不引入 Koin，避免双轨 DI 约定和模块装配分裂。

## Consequences

- M0-T03 初始化 Android 工程时必须包含 Hilt Gradle 插件与基础模块；
- Compose 预览和仪器测试需遵循 Hilt 测试约定；
- 新增 Android 模块默认通过 Hilt Module 暴露依赖。
