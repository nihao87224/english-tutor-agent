# ADR-0011：Java 根包名

状态：Accepted

## Decision

Java 根包名使用：

```text
cn.forever24.tutor
```

## Rationale

项目使用的域名为 `forever24.cn`，Java 包名按域名反向命名应为 `cn.forever24`，
而不是 `com.forever24`。后端模块统一在该根包下组织。

## Consequences

M0-T02 生成代码前必须以此包名为准，后续不要再进行大规模包迁移。
