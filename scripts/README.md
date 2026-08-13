# Scripts

## validate_project.py

校验：

- 必需文件是否存在；
- JSON 与 JSONL 是否可解析；
- OpenAPI、Compose 和 GitHub Actions YAML 是否可解析；
- OpenAPI 是否包含基本 3.x 结构；
- JSON Schema 是否合法；
- `contracts/examples/*.example.json` 是否符合对应 `contracts/schemas/*.schema.json`。

运行：

```bash
pip install -r scripts/requirements-ci.txt
python scripts/validate_project.py
```

后续可增加：

- OpenAPI lint；
- Markdown 链接检查；
- Flyway clean database 检查；
- AI 回归评测脚本。
