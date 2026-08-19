# Scripts

## validate_project.py

校验：

- 必需文件是否存在；
- JSON 与 JSONL 是否可解析；
- OpenAPI、Compose 和 GitHub Actions YAML 是否可解析；
- OpenAPI 是否包含基本 3.x 结构；
- JSON Schema 是否合法；
- `contracts/examples/*.example.json` 是否符合对应 `contracts/schemas/*.schema.json`。
- V2 learning resource manifest 及其五类组合 Schema 是否合法；
- V2 资源的 Lin Muen、task hero、引用完整性、Evidence、Audio/Transcript 业务规则；
- V2 有效样例必须通过、定向无效 fixture 必须触发声明的失败规则。

运行：

```bash
pip install -r scripts/requirements-ci.txt
python scripts/validate_project.py
```

只校验契约（适用于内容生产和 Content Import 前置检查）：

```bash
python scripts/validate_project.py --contracts-only
```

后续可增加：

- OpenAPI lint；
- Markdown 链接检查；
- Flyway clean database 检查；
- AI 回归评测脚本。
