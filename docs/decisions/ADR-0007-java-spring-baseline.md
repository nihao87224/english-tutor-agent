# ADR-0007：Java 与 Spring 技术基线

状态：Accepted

## Decision

首个后端工程锁定：

- Java 21 LTS；
- Spring Boot 4.1.0；
- Spring AI 2.0.0；
- Maven Wrapper。

## Rationale

Spring Boot 4.1.0 官方支持 Java 17–26；Spring AI 2.0 系列官方支持
Spring Boot 4.0 与 4.1 系列。选择 Java 21 兼顾 LTS、现代语言能力和部署稳定性。

## Verification gate

M0-T02 的第一步必须在本地和 CI 验证 BOM 与插件可解析。
如果企业镜像仓库暂未同步这些版本，应标记为环境阻塞，不得擅自退回旧技术栈。
