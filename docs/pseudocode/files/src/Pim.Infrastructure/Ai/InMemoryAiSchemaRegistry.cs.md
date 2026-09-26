# src/Pim.Infrastructure/Ai/InMemoryAiSchemaRegistry.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Infrastructure
- 职责：进程内非线程安全的 AI JSON Schema 注册表，按 `(Name, Version)` 存取 `AiSchemaDefinition`；与 `AiSchemaRegistry`（并发字典）功能等价但使用普通 `Dictionary`。
- 主要依赖：`Pim.Core.Ai`（`IAiSchemaRegistry`、`AiSchemaDefinition`）
- 被谁使用：可作为 `IAiSchemaRegistry` 轻量实现（测试或单线程场景）；当前 DI 默认注册的是 `AiSchemaRegistry`，本类未在 `ServiceCollectionExtensions` 中注册

## 函数级结构化伪代码

### InMemoryAiSchemaRegistry
#### 字段 `_schemas`
- 输入：无
- 输出：`Dictionary<(string Name, string Version), AiSchemaDefinition>`
- 副作用：实例内可变字典
- 步骤：
  1. 字段初始化器创建空字典 `[]`
- 分支与异常：无
- 调用：无

#### `void Register(AiSchemaDefinition schema)`
- 输入：`schema`（含 `Name`、`Version`）
- 输出：无
- 副作用：写入/覆盖字典条目
- 步骤：
  1. 以 `(schema.Name, schema.Version)` 为键
  2. 将 `schema` 赋给 `_schemas[key]`（同键覆盖）
- 分支与异常：`schema` 为 null 时运行时异常
- 调用：无外部

#### `AiSchemaDefinition? Get(string name, string version)`
- 输入：schema 名称与版本
- 输出：命中定义或 `null`
- 副作用：无
- 步骤：
  1. `TryGetValue((name, version), out schema)`
  2. 成功返回 `schema`，否则 `null`
- 分支与异常：未注册 → `null`
- 调用：无外部

## 近逐行中文伪代码

1. 引入 `Pim.Core.Ai`
2. 命名空间 `Pim.Infrastructure.Ai`
3. 密封类 `InMemoryAiSchemaRegistry` 实现 `IAiSchemaRegistry`
4. 私有只读字段 `_schemas`：普通字典，键为 `(Name, Version)` 元组，集合表达式 `[]` 初始化
5. 方法 `Register`：表达式体，将 `schema` 以 `(schema.Name, schema.Version)` 写入 `_schemas`
6. 方法 `Get`：表达式体，对 `(name, version)` 做 `TryGetValue`
7. 命中返回 `schema`，否则返回 `null`
8. （文件结束）

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Infrastructure/Ai/InMemoryAiSchemaRegistry.cs",
      "label": "InMemoryAiSchemaRegistry",
      "path": "src/Pim.Infrastructure/Ai/InMemoryAiSchemaRegistry.cs",
      "doc": "docs/pseudocode/files/src/Pim.Infrastructure/Ai/InMemoryAiSchemaRegistry.cs.md",
      "layer": "infrastructure",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Ai/InMemoryAiSchemaRegistry.cs", "to": "src/Pim.Core/Ai/IAiSchemaRegistry.cs", "type": "implements" },
    { "from": "src/Pim.Infrastructure/Ai/InMemoryAiSchemaRegistry.cs", "to": "src/Pim.Core/Ai/AiDtos.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Ai/InMemoryAiSchemaRegistry.cs", "to": "src/Pim.Infrastructure/Ai/AiSchemaRegistry.cs", "type": "depends_on" }
  ]
}
```
