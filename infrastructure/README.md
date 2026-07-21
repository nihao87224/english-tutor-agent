# Infrastructure

本地基础设施由根目录 `docker-compose.yml` 提供：

- MySQL 8.x
- Redis 7.x
- MinIO

生产部署不应直接复制本地 Compose。正式部署方案在 M6 前单独设计。

约束：

- 本地卷位于 `.infrastructure-data/`，不提交 Git；
- 数据库业务表由 Server Flyway 迁移创建；
- MinIO Bucket 初始化脚本在 M0-T04 实现；
- Redis Key 规范以详细设计为准。
