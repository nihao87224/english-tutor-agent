# V2 content contract examples

- `valid/*.manifest.json` 是可直接交给 Content Import 的完整资源清单。
- `invalid/*.fixture.json` 以有效清单为基线，通过确定性 mutation 构造无效清单，避免复制整份资源造成测试漂移。
- 每个无效 fixture 声明 `expectedRule`；校验器必须确认该规则实际触发，不能只依赖“出现任意错误”。

运行：

```bash
python scripts/validate_project.py --contracts-only
```

fixture 支持 `add`、`replace` 和 `removeMatching`。`path` 使用 JSON Pointer 语法。
