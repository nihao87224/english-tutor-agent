# Scripts

## validate_project.py

校验：

- 必需文件是否存在；
- JSON 与 JSONL 是否可解析；
- OpenAPI 和 Compose YAML 是否可解析。

运行：

```bash
pip install pyyaml
python scripts/validate_project.py
```

后续可增加：

- OpenAPI lint；
- JSON Schema 示例验证；
- Markdown 链接检查；
- Flyway clean database 检查；
- AI 回归评测脚本。
