# src/Pim.Infrastructure/Ai/AiSchemaRegistry.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Infrastructure
- 职责：线程安全的 AI JSON Schema 注册表实现，按 `(Name, Version)` 存取 `AiSchemaDefinition`。
- 主要依赖：`System.Collections.Concurrent`；`Pim.Core.Ai`（`IAiSchemaRegistry`、`AiSchemaDefinition`）
- 被谁使用：DI 注册为 `IAiSchemaRegistry` 单例；`AiGateway` 解析 schema；`FileAiService.RegisterSchemas` 等模块启动注册；单元测试

## 函数级结构化伪代码

### AiSchemaRegistry
#### 字段 `_schemas`
- 输入：无
- 输出：`ConcurrentDictionary<(string Name, string Version), AiSchemaDefinition>`
- 副作用：进程内共享可变字典（单例场景）
- 步骤：
  1. 构造时初始化空并发字典
- 分支与异常：无
- 调用：无

#### `void Register(AiSchemaDefinition schema)`
- 输入：`schema`（含 `Name`、`Version` 等）
- 输出：无
- 副作用：写入/覆盖字典条目
- 步骤：
  1. 以 `(schema.Name, schema.Version)` 为键
  2. 将 `schema` 赋给 `_schemas[key]`（同键覆盖）
- 分支与异常：无显式校验；`schema` 为 null 时会由运行时抛异常
- 调用：无外部

#### `AiSchemaDefinition? Get(string name, string version)`
- 输入：schema 名称与版本字符串
- 输出：命中则定义，否则 `null`
- 副作用：无
- 步骤：
  1. `TryGetValue((name, version), out schema)`
  2. 成功返回 `schema`，失败返回 `null`
- 分支与异常：未注册 → `null`
- 调用：无外部

## 近逐行中文伪代码

1. 引入 `System.Collections.Concurrent`
2. 引入 `Pim.Core.Ai`
3. 命名空间 `Pim.Infrastructure.Ai`
4. 密封类 `AiSchemaRegistry` 实现 `IAiSchemaRegistry`
5. 私有只读字段 `_schemas`：并发字典，键为 `(Name, Version)` 元组
6. 方法 `Register`：
7.   将 `schema` 以 `(schema.Name, schema.Version)` 写入 `_schemas`（覆盖同键）
8. 方法 `Get`：
9.   尝试用 `(name, version)` 从 `_schemas` 取值
10.  找到则返回该 `AiSchemaDefinition`，否则返回 `null`
11. （文件结束）

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Infrastructure/Ai/AiSchemaRegistry.cs",
      "label": "AiSchemaRegistry",
      "path": "src/Pim.Infrastructure/Ai/AiSchemaRegistry.cs",
      "doc": "docs/pseudocode/files/src/Pim.Infrastructure/Ai/AiSchemaRegistry.cs.md",
      "layer": "infrastructure",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Ai/AiSchemaRegistry.cs", "to": "src/Pim.Core/Ai/IAiSchemaRegistry.cs", "type": "implements" },
    { "from": "src/Pim.Infrastructure/Ai/AiSchemaRegistry.cs", "to": "src/Pim.Core/Ai/AiDtos.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Extensions/ServiceCollectionExtensions.cs", "to": "src/Pim.Infrastructure/Ai/AiSchemaRegistry.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Ai/AiGateway.cs", "to": "src/Pim.Infrastructure/Ai/AiSchemaRegistry.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Ai/AiGateway.cs", "to": "src/Pim.Core/Ai/IAiSchemaRegistry.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.Files/Services/FileAiService.cs", "to": "src/Pim.Core/Ai/IAiSchemaRegistry.cs", "type": "calls" },
    { "from": "tests/Pim.UnitTests/Ai/AiSchemaRegistryTests.cs", "to": "src/Pim.Infrastructure/Ai/AiSchemaRegistry.cs", "type": "tests" },
    { "from": "tests/Pim.UnitTests/Ai/AiGatewayTests.cs", "to": "src/Pim.Infrastructure/Ai/AiSchemaRegistry.cs", "type": "tests" }
  ]
}
```
