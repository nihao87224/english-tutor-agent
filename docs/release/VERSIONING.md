# Versioning

## Documents

- PRD：`major.minor.patch`
- Design：与对应 PRD 主版本绑定
- API：URL 使用 `/api/v1`，破坏兼容时升级
- JSON Schema：Schema 内使用 `$id` 和语义版本
- Prompt：`name + semantic version`
- Android/Server：发布版本独立，但必须记录契约版本

## Git

推荐分支：

- `main`：可发布
- `develop`：可选，团队较小时可以不使用
- `feature/<task-id>-<name>`
- `fix/<task-id>-<name>`

推荐提交：

```text
feat(profile): support primary learning goal
test(profile): add goal persistence integration test
fix(plan): prevent duplicate daily plan
docs(api): update learning plan contract
chore(build): initialize backend modules
```
