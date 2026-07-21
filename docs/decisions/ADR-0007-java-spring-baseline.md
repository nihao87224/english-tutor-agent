# ADR-0007：Java 与 Spring 技术基线

状态：Accepted

## Decision

首个后端工程锁定：

- Java 21 LTS；
- Spring Boot 4.1.0；
- Spring AI 2.0.0；
- Maven Wrapper。

## Rationale

Spring Boot 4.1.0 官方兼容 Java 17–26，但本项目**锁定 Java 21 LTS**，不将 Java 17 作为可接受回退版本。Spring AI 2.0 系列官方支持 Spring Boot 4.0 与 4.1 系列。

## Constraints

- 不得回退到 Java 17 或其他更低 Java 版本；
- 不得擅自降级 Spring Boot 或 Spring AI 主版本；
- 若制品源暂不可用，标记为 `BLOCKED_BY_DECISION`，先解决镜像/仓库问题。

## Verification gate

M0-T02 的第一步必须在本地和 CI 验证 BOM 与插件可解析。
如果企业镜像仓库暂未同步这些版本，应标记为环境阻塞，不得擅自退回旧技术栈。
