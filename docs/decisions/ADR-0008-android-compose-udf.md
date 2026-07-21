# ADR-0008：Android Jetpack Compose + UDF

状态：Accepted

## Decision

Android 使用：

- Kotlin；
- Jetpack Compose + Material 3；
- ViewModel + StateFlow；
- 单向数据流；
- Repository/use case 分层；
- Room/DataStore 进行本地恢复。

UI 不直接访问网络、数据库或 AI Provider。
