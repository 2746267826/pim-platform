# tests/Pim.UnitTests/Ai/AiSchemaRegistryTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：`AiSchemaRegistry` 按名版本获取与并发注册。
- 主要依赖：`AiSchemaRegistry`、`AiSchemaDefinition`
- 被谁使用：xUnit

## 函数级结构化伪代码

1. Register 后 Get 返回同一 schema
2. Parallel 100 次 Register/Get 后 version 42 可取

## 近逐行中文伪代码

1. [L1-L24] 注册获取
2. [L26-L47] 并发

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Ai/AiSchemaRegistryTests.cs",
      "label": "AiSchemaRegistryTests",
      "path": "tests/Pim.UnitTests/Ai/AiSchemaRegistryTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Ai/AiSchemaRegistryTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/Pim.UnitTests/Ai/AiSchemaRegistryTests.cs", "to": "src/Pim.Infrastructure/Ai/AiSchemaRegistry.cs", "type": "tests" }
  ]
}
```
