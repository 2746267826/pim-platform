# src/Pim.Core/Ai/IAiSchemaRegistry.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Core
- 职责：定义 AI 结构化输出 Schema 注册表契约，支持按名称+版本注册与查询 `AiSchemaDefinition`。
- 主要依赖：`AiSchemaDefinition`（同命名空间，本文件未定义）
- 被谁使用：`AiGateway`（解析请求 Schema）、`AiSchemaRegistry` / `InMemoryAiSchemaRegistry`（实现）、`FileAiService.RegisterSchemas`、`FilesModule` 启动注册

## 函数级结构化伪代码

### IAiSchemaRegistry
#### void Register(AiSchemaDefinition schema)
- 输入：`schema` — 待注册的 Schema 定义（含 Name、Version、JsonSchema 等）
- 输出：无
- 副作用：将 Schema 写入注册表（实现侧决定覆盖/并发语义）
- 步骤：
  1. 接收 Schema 定义
  2. 按实现约定登记到内部存储
- 分支与异常：契约本身不规定异常；实现可对重复键覆盖或抛错
- 调用：被模块启动/服务初始化调用（如 `FileAiService.RegisterSchemas`）

#### AiSchemaDefinition? Get(string name, string version)
- 输入：`name` — Schema 名称；`version` — Schema 版本
- 输出：匹配的 `AiSchemaDefinition`，未找到则 `null`
- 副作用：无（只读查询）
- 步骤：
  1. 以 `(name, version)` 为键查找
  2. 命中返回定义，否则返回 `null`
- 分支与异常：未命中返回 `null`，不抛异常（契约层）
- 调用：被 `AiGateway.ResolveSchema` 等调用

## 近逐行中文伪代码

1. 命名空间：`Pim.Core.Ai`
2. 声明公共接口 `IAiSchemaRegistry`
3. 方法 `Register`：接收 `AiSchemaDefinition schema`，无返回值，用于登记 Schema
4. 方法 `Get`：接收 `name` 与 `version`，返回可空的 `AiSchemaDefinition`
5. 接口到此结束（无字段、无默认实现）

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Core/Ai/IAiSchemaRegistry.cs",
      "label": "IAiSchemaRegistry",
      "path": "src/Pim.Core/Ai/IAiSchemaRegistry.cs",
      "doc": "docs/pseudocode/files/src/Pim.Core/Ai/IAiSchemaRegistry.cs.md",
      "layer": "core",
      "kind": "other"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Ai/AiSchemaRegistry.cs", "to": "src/Pim.Core/Ai/IAiSchemaRegistry.cs", "type": "implements" },
    { "from": "src/Pim.Infrastructure/Ai/InMemoryAiSchemaRegistry.cs", "to": "src/Pim.Core/Ai/IAiSchemaRegistry.cs", "type": "implements" },
    { "from": "src/Pim.Infrastructure/Ai/AiGateway.cs", "to": "src/Pim.Core/Ai/IAiSchemaRegistry.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Files/Services/FileAiService.cs", "to": "src/Pim.Core/Ai/IAiSchemaRegistry.cs", "type": "calls" }
  ]
}
```
