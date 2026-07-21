# ADR-0006：MySQL + Redis + S3 兼容对象存储

状态：Accepted

## Decision

- MySQL：持久化用户、评估、计划、训练、证据、复习和审计数据；
- Redis：短期状态、缓存、幂等键、分布式锁和限时数据；
- S3 兼容对象存储：用户录音、TTS 与听力音频；
- 第一版不引入向量数据库。

## Constraints

Redis 不是长期事实来源；音频二进制不写入 MySQL BLOB。
