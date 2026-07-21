# Server

后端代码将在 M0-T02 初始化。

预期基线：

- Java 21
- Spring Boot 4.1.0
- Spring AI 2.0.0
- Maven
- MySQL 8.x
- Redis 7.x
- Flyway
- Testcontainers

建议模块边界以 `docs/design/02_DETAILED_DESIGN_BACKEND.md` 为准。

不要在当前空目录中预先生成全部业务模块。先完成 M0，再按纵向任务逐步添加。

## Namespace

Java 根包名固定为：`cn.forever24.tutor`。
